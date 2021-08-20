package be.mathiasbosman.fs.service.nio;

import be.mathiasbosman.fs.domain.FileNode;
import be.mathiasbosman.fs.domain.FileNodeType;
import be.mathiasbosman.fs.service.AbstractFileService;
import be.mathiasbosman.fs.service.FileNodeVisitor;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

public class NIOFileService extends AbstractFileService {

  public static final Character[] INVALID_WINDOWS_SPECIFIC_CHARS = {'"', '*', ':', '<', '>', '?',
      '\\', '|', 0x7F};
  public static final Character[] INVALID_UNIX_SPECIFIC_CHARS = {'\000'};

  private final Path workDir;
  private final Function<Path, FileNode> toFile = this::file;

  public NIOFileService(FileSystem fs, String prefix) {
    workDir = fs.getPath(prefix);
  }

  public NIOFileService(String prefix) {
    this(FileSystems.getDefault(), prefix);
  }

  public static boolean isValidFilename(String filename, boolean isUnixSystem) {
    if (StringUtils.isEmpty(filename) || filename.length() > 255) {
      return false;
    }
    return Arrays
        .stream(isUnixSystem ? INVALID_UNIX_SPECIFIC_CHARS : INVALID_WINDOWS_SPECIFIC_CHARS)
        .noneMatch(ch -> filename.contains(ch.toString()));
  }

  public boolean isValidFilename(String filename) {
    return isValidFilename(filename,
        SystemUtils.IS_OS_UNIX || SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC);
  }

  @Override
  public long countFiles(FileNode fileNode) {
    if (!SystemUtils.IS_OS_UNIX) {
      return defaultFileCount(fileNode);
    }
    try {
      Path path = path(fileNode.getPath());
      String shellCommand = "find . -maxdepth 1 -type f | wc -l";
      String[] cmd = {"/bin/sh", "-c", shellCommand};
      ProcessBuilder builder = new ProcessBuilder();
      builder.redirectErrorStream(true);
      builder.command(cmd);
      builder.directory(path.toFile());
      Process process = builder.start();
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
      String line = reader.readLine();
      reader.close();
      return Long.parseLong(line.trim());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void delete(FileNode fileNode, boolean recursive) {
    if (!recursive) {
      deleteNode(fileNode);
      return;
    }

    walk(fileNode, new FileNodeVisitor() {
      @Override
      public void on(FileNode node) {
        deleteNode(node);
      }

      @Override
      public void pre(FileNode folder) {

      }

      @Override
      public void post(FileNode folder) {
        deleteNode(folder);
      }
    });
  }

  @Override
  public String getMimeType(FileNode fileNode) {
    Path path = path(fileNode.getPath());
    try {
      return Files.probeContentType(path);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isFolder(String pad) {
    return Files.isDirectory(path(pad));
  }

  @Override
  public List<FileNode> list(FileNode root) {
    try {
      Path path = path(root.getPath());
      FileAccumulator accumulator = new FileAccumulator(path);
      Files.walkFileTree(path, Collections.emptySet(), 1, accumulator);
      List<Path> fromIterable = accumulator.toList();
      return fromIterable.stream().map(toFile).sorted((Comparator.comparing(FileNode::getName)))
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public InputStream open(FileNode file) {
    try {
      return Files.newInputStream(path(file.getPath()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void save(InputStream in, String pad, long size) {
    Path path = path(pad);
    mkFolders(path.getParent());
    try (OutputStream out = Files.newOutputStream(path)) {
      IOUtils.copy(in, out);
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public Stream<FileNode> streamDirectory(FileNode root) {
    try {
      Path path = path(root.getPath());
      return Files.walk(path).map(toFile);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void copyContent(FileNode source, String target) {
    save(open(source), target);
  }

  @Override
  protected boolean exists(String path) {
    return Files.exists(path(combine(path)));
  }

  @Override
  protected FileNodeType getFileNodeType(String pad) {
    if (!exists(pad)) {
      return FileNodeType.NONE_EXISTENT;
    }
    return isFolder(pad) ? FileNodeType.FOLDER : FileNodeType.FILE;
  }

  @Override
  protected long getSize(String pad) {
    try {
      return Files.size(path(pad));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void mkFolders(String path) {
    mkFolders(path(path));
  }

  private void mkFolders(Path path) {
    try {
      Files.createDirectories(path);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  protected Path path(FileNode node) {
    return path(node.getPath());
  }

  private void deleteNode(FileNode node) {
    try {
      Files.delete(path(node));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private FileNode file(Path path) {
    String subPath = path.toString().substring(workDir.toString().length());
    return getFileNode(strip(subPath, File.pathSeparatorChar));
  }

  private Path path(String location) {
    return StringUtils.isEmpty(location) ? workDir : workDir.resolve(location);
  }

  private static class FileAccumulator extends SimpleFileVisitor<Path> {

    private final List<Path> files = new LinkedList<>();
    private final Path root;

    private FileAccumulator(Path root) {
      this.root = root;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
        throws IOException {
      if (!dir.equals(root)) {
        files.add(dir);
      }
      return super.preVisitDirectory(dir, attrs);
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      files.add(file);
      return super.visitFile(file, attrs);
    }

    List<Path> toList() {
      return files;
    }
  }
}
