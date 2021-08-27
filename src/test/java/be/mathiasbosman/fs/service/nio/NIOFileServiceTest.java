package be.mathiasbosman.fs.service.nio;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.Iterables.transform;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.newBufferedReader;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.newOutputStream;
import static java.nio.file.Files.walkFileTree;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import be.mathiasbosman.fs.domain.FileSystemNode;
import be.mathiasbosman.fs.service.AbstractFileServiceTest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NIOFileServiceTest extends AbstractFileServiceTest {

  private final FileSystem fileSystem = FileSystems.getDefault();
  private final Path workdir = fileSystem.getPath("/tmp/" + System.identityHashCode(this) + "/");
  private static final String targetPath = "TARGET_DIRECTORY";
  private static final String templatePath = "TEMPLATES";

  public NIOFileServiceTest() {
    setFs(new NIOFileService(fileSystem, workdir.toString()));
  }

  @BeforeEach
  void setup() throws IOException {
    cleanup();
    createDirectories(workdir);
  }

  @AfterEach
  void cleanup() throws IOException {
    FileUtils.deleteDirectory(workdir.toFile());
  }

  @Override
  protected void putImageObject(String path) {
    putObject(path, "-");
  }

  @Override
  protected void assertExists(String path) {
    assertThat(Files.exists(workdir.resolve(path))).isTrue();
  }

  @Override
  protected void assertDirectoryExists(String path) {
    assertExists(path);
  }

  @Override
  protected void assertNotExists(String path) {
    assertThat(Files.exists(fileSystem.getPath(path))).isFalse();
  }

  @Override
  protected String getContent(String path) {
    try (InputStream in = newInputStream(workdir.resolve(path))) {
      return IOUtils.toString(in, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  protected void putDirectory(String pad) {
    try {
      createDirectories(workdir.resolve(pad));
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  protected void putObject(String location, String data) {
    try {
      Path path = workdir.resolve(location);
      if (location.lastIndexOf('/') != -1) {
        putDirectory(path.getParent().toString());
      }
      try (OutputStream out = newOutputStream(path)) {
        out.write(data.getBytes());
        out.flush();
      }
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Test
  void copy() throws IOException {
    createDirectories(workdir.resolve(targetPath));
    saveContent("template-x", "x");
    saveContent("template-y", "y");
    getFs().copy(templatePath, targetPath);
    assertTemplatesCopied("template-x", "template-y");
    assertContentCopied("x", "y");
  }

  @Test
  void getCreationTime() {
    putObject("x", "-");
    FileSystemNode fileNode = getFs().getFileNode("x");
    assertThat(getFs().getCreationTime(fileNode, ZoneId.systemDefault()))
        .isNotNull()
        .isBefore(LocalDateTime.now());
  }

  @Test
  void getLastModifiedTime() {
    putObject("x", "-");
    FileSystemNode fileNode = getFs().getFileNode("x");
    LocalDateTime lastModifiedTime = getFs().getLastModifiedTime(fileNode, ZoneId.systemDefault());
    assertThat(lastModifiedTime)
        .isNotNull()
        .isBefore(LocalDateTime.now());
    putObject("x", "--");
    assertThat(getFs().getLastModifiedTime(fileNode, ZoneId.systemDefault()))
        .isNotNull()
        .isNotEqualTo(lastModifiedTime);
  }

  @Test
  void isValidFilename() {
    assertThat(getFs().isValidFilename("valid-file_name.tst")).isTrue();
    assertThat(getFs().isValidFilename("valid/filename.tst")).isTrue();
    assertThat(getFs().isValidFilename("validFilename")).isTrue();
    assertThat(getFs().isValidFilename("")).isFalse();

    for (Character invalidWindowsSpecificChar : NIOFileService.INVALID_WINDOWS_SPECIFIC_CHARS) {
      assertThat(NIOFileService
          .isValidFilename("invalid" + invalidWindowsSpecificChar + "filename.tst", false))
          .isFalse();
    }
    for (Character invalidUnixSpecificChar : NIOFileService.INVALID_UNIX_SPECIFIC_CHARS) {
      assertThat(NIOFileService
          .isValidFilename("invalid" + invalidUnixSpecificChar + "filename.tst", true))
          .isFalse();
    }
  }

  @Test
  void mkDirectories() {
    getFs().mkDirectories(targetPath);
    assertThat(Files.exists(workdir.resolve(targetPath))).isTrue();
  }

  @Test
  void stream() {
    putObject("x/a", "-");
    putObject("x/z", "-");
    putObject("x/b/a", "-");
    Stream<FileSystemNode> stream = getFs().streamDirectory(getFs().getFileNode("x"));
    assertThat(stream).isNotNull();
    List<FileSystemNode> collected = stream.collect(Collectors.toList());
    assertThat(collected).hasSize(5);
  }

  private void assertTemplatesCopied(String... expected) {
    Iterable<Path> copiedPaths = getCopiedFiles();
    Iterable<String> paths = transform(copiedPaths, Path::toString);
    Assertions.assertThat(paths).contains(Arrays.stream(expected)
        .map(input -> workdir.resolve(targetPath).resolve(input).toString())
        .toArray(String[]::new));
  }

  private void assertContentCopied(String... expected) {
    Iterable<Path> copiedPaths = getCopiedFiles();
    Iterable<Path> onlyFiles = StreamSupport.stream(copiedPaths.spliterator(), false)
        .filter(input -> !isDirectory(input)).collect(Collectors.toList());
    List<String> contents = StreamSupport.stream(onlyFiles.spliterator(), false).map(input -> {
      try (Reader reader = newBufferedReader(input, UTF_8)) {
        return IOUtils.toString(reader);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }).collect(Collectors.toList());
    assertThat(contents).containsAll(asList(expected));
  }

  private Iterable<Path> getCopiedFiles() {
    return new FileAccumulator(workdir.resolve(targetPath)).toIterable();
  }

  private void saveContent(String sub, String content) throws IOException {
    Path template = workdir.resolve(NIOFileServiceTest.templatePath).resolve(sub);
    createDirectories(template.getParent());
    try (PrintWriter out = new PrintWriter(Files.newOutputStream(template))) {
      out.print(content);
    }
  }

  private static class FileAccumulator extends SimpleFileVisitor<Path> {

    private final List<Path> helper = new ArrayList<>();
    private final Path root;

    private FileAccumulator(Path root) {
      this.root = root;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
        throws IOException {
      if (!dir.equals(root)) {
        helper.add(dir);
      }
      return super.preVisitDirectory(dir, attrs);
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      helper.add(file);
      return super.visitFile(file, attrs);
    }

    private Iterable<Path> toIterable() {
      try {
        walkFileTree(root, this);
        return helper;
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }
}