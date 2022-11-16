package be.mathiasbosman.fs.service.nio;

import be.mathiasbosman.fs.core.domain.FileSystemNode;
import be.mathiasbosman.fs.core.domain.FileSystemNodeType;
import be.mathiasbosman.fs.core.service.AbstractFileService;
import be.mathiasbosman.fs.core.service.FileNodeVisitor;
import be.mathiasbosman.fs.core.util.FileServiceUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * An implementation of the {@link be.mathiasbosman.fs.core.service.FileService} for NIO file
 * systems. It holds some static arrays containing invalid filename characters for Windows and Unix
 * systems.
 */
@Slf4j
public class NioFileService extends AbstractFileService {

  public static final FileSystem DEFAULT_FILE_SYSTEM = FileSystems.getDefault();

  private final Path workDir;
  private final Function<Path, FileSystemNode> toFile = this::file;

  public NioFileService(FileSystem fs, String prefix) {
    workDir = fs.getPath(prefix);
  }

  public NioFileService(String prefix) {
    this(DEFAULT_FILE_SYSTEM, prefix);
  }

  @Override
  public void delete(FileSystemNode node, boolean recursive) {
    if (!recursive) {
      if (node.isDirectory() && countFiles(node) > 0) {
        throw new IllegalStateException("Directory is not empty for deletion");
      }
      deleteNode(node);
      return;
    }

    walk(node, new FileNodeVisitor() {
      @Override
      public void on(FileSystemNode node) {
        deleteNode(node);
      }

      @Override
      public void pre(FileSystemNode directory) {
        // no operation on pre() when deleting
      }

      @Override
      public void post(FileSystemNode directory) {
        deleteNode(directory);
      }
    });
  }

  @Override
  public LocalDateTime getCreationTime(FileSystemNode node, ZoneId zoneId) {
    try {
      Path path = path(node.getPath());
      BasicFileAttributes attributes = getAttributes(path);
      FileTime fileTime = attributes.creationTime();
      return LocalDateTime.ofInstant(fileTime.toInstant(), zoneId);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public LocalDateTime getLastModifiedTime(FileSystemNode node, ZoneId zoneId) {
    try {
      Path path = path(node.getPath());
      BasicFileAttributes attributes = getAttributes(path);
      FileTime fileTime = attributes.lastModifiedTime();
      return LocalDateTime.ofInstant(fileTime.toInstant(), zoneId);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public boolean isDirectory(String pad) {
    return Files.isDirectory(path(pad));
  }

  @Override
  public List<FileSystemNode> list(FileSystemNode root) {
    try {
      Path path = path(root.getPath());
      FileAccumulator accumulator = new FileAccumulator(path);
      Files.walkFileTree(path, Collections.emptySet(), 1, accumulator);
      List<Path> fromIterable = accumulator.toList();
      return fromIterable.stream()
          .map(toFile)
          .sorted((Comparator.comparing(FileSystemNode::getName)))
          .toList();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public InputStream open(FileSystemNode node) {
    try {
      log.debug("Getting {}", node.getPath());
      return Files.newInputStream(path(node.getPath()));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void save(InputStream in, String path, long size) {
    log.debug("Saving input stream to {}", path);
    Path resolvedPath = path(path);
    mkDirectories(resolvedPath.getParent());
    try (OutputStream out = Files.newOutputStream(resolvedPath)) {
      IOUtils.copy(in, out);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  @SuppressWarnings("resource") //we return the stream
  public Stream<FileSystemNode> streamDirectory(FileSystemNode root) {
    try {
      Path path = path(root.getPath());
      return Files.walk(path).map(toFile);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  protected void copyContent(FileSystemNode source, String target) {
    log.debug("Copying object {} to {}", source, target);
    save(open(source), target);
  }

  @Override
  protected boolean exists(String path) {
    return Files.exists(path(FileServiceUtils.combine(path)));
  }

  @Override
  protected FileSystemNodeType getFileNodeType(String pad) {
    if (!exists(pad)) {
      return FileSystemNodeType.NONE_EXISTENT;
    }
    return isDirectory(pad) ? FileSystemNodeType.DIRECTORY : FileSystemNodeType.FILE;
  }

  @Override
  protected long getSize(String pad) {
    try {
      return Files.size(path(pad));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  protected void mkDirectories(String path) {
    mkDirectories(path(path));
  }

  private void mkDirectories(Path path) {
    try {
      Files.createDirectories(path);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  protected Path path(FileSystemNode node) {
    return path(node.getPath());
  }

  private Path path(String location) {
    return StringUtils.isEmpty(location) ? workDir : workDir.resolve(location);
  }

  private void deleteNode(FileSystemNode node) {
    try {
      Files.delete(path(node));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private FileSystemNode file(Path path) {
    String subPath = path.toString().substring(workDir.toString().length());
    return getFileNode(FileServiceUtils.strip(subPath));
  }

  private BasicFileAttributes getAttributes(Path path) throws IOException {
    return Files.readAttributes(path, BasicFileAttributes.class);
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
