package be.mathiasbosman.fs.service.nio;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.newOutputStream;
import static org.assertj.core.api.Assertions.assertThat;

import be.mathiasbosman.fs.domain.FileSystemNode;
import be.mathiasbosman.fs.service.AbstractFileServiceTest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Slf4j
class NioFileServiceTest extends AbstractFileServiceTest {

  private final FileSystem fileSystem = FileSystems.getDefault();
  private final Path workdir = fileSystem.getPath("/tmp/" + System.identityHashCode(this) + "/");

  public NioFileServiceTest() {
    setFs(new NioFileService(fileSystem, workdir.toString()));
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

  @Override
  protected String getRemotePath(String path) {
    return path;
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
  }

  @Test
  void streamDirectory() {
    putObject("x/a", "-");
    putObject("x/z", "-");
    putObject("x/b/a", "-");
    List<FileSystemNode> directories = getFs().streamDirectory(getFs().getFileNode("x"))
        .filter(FileSystemNode::isDirectory)
        .collect(Collectors.toList());
    List<FileSystemNode> files = getFs().streamDirectory(getFs().getFileNode("x"))
        .filter(n -> !n.isDirectory())
        .collect(Collectors.toList());
    assertThat(files).hasSize(3);
    assertThat(directories).hasSize(2);
  }
}