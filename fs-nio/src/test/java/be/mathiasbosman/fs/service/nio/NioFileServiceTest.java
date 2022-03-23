package be.mathiasbosman.fs.service.nio;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.newOutputStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import be.mathiasbosman.fs.core.domain.FileSystemNode;
import be.mathiasbosman.fs.core.domain.FileSystemNodeImpl;
import be.mathiasbosman.fs.core.service.AbstractFileServiceTest;
import be.mathiasbosman.fs.core.service.FileService;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class NioFileServiceTest extends AbstractFileServiceTest {

  private static final String targetPath = "TARGET_DIRECTORY";
  private final FileSystem fileSystem = NioFileService.DEFAULT_FILE_SYSTEM;
  private final Path workdir = fileSystem.getPath("/tmp/" + System.identityHashCode(this) + "/");

  @Override
  protected FileService getFs() {
    return new NioFileService(workdir.toString());
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
  protected void putDirectory(String path) {
    try {
      createDirectories(workdir.resolve(path));
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
  void deleteOnNio() {
    putObject("x/y/z", "-");

    FileService fs = getFs();
    FileSystemNode fileNode = fs.getFileNode("x/y");
    assertThatThrownBy(() -> fs.delete(fileNode, false))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Directory is not empty for deletion");
  }

  @Test
  void listWithException() {
    try (MockedStatic<Files> files = Mockito.mockStatic(Files.class)) {
      files.when(() -> Files.walkFileTree(any(), any(), anyInt(), any()))
          .thenThrow(new IOException("Mocked IOException"));

      FileService fs = getFs();
      final FileSystemNode mockNode = new FileSystemNodeImpl("x", "y", true, 0);

      assertThatThrownBy(() -> fs.list(mockNode))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Mocked IOException");
    }
  }

  @Test
  void openWithException() {
    try (MockedStatic<Files> files = Mockito.mockStatic(Files.class)) {
      files.when(() -> Files.newInputStream(any()))
          .thenThrow(new IOException("Mocked IOException"));

      FileService fs = getFs();
      final FileSystemNode mockNode = new FileSystemNodeImpl("x", "y", false, 1);

      assertThatThrownBy(() -> fs.open(mockNode))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Mocked IOException");
    }
  }

  @Test
  void saveWithException() {
    try (MockedStatic<IOUtils> mockIOUtils = Mockito.mockStatic(IOUtils.class)) {
      mockIOUtils.when(() -> IOUtils.copy(any(InputStream.class), any(OutputStream.class)))
          .thenThrow(new IOException("Mocked IOException"));

      FileService fs = getFs();
      byte[] bytes = "content".getBytes();
      assertThatThrownBy(() -> fs.save(bytes, "x/y"))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Mocked IOException");
    }
  }

  @Test
  void streamDirectoryWithException() {
    try (MockedStatic<Files> files = Mockito.mockStatic(Files.class)) {
      files.when(() -> Files.walk(any()))
          .thenThrow(new IOException("Mocked IOException"));

      final FileSystemNode mockNode = new FileSystemNodeImpl("x", "y", false, 1);

      FileService fs = getFs();
      assertThatThrownBy(() -> fs.streamDirectory(mockNode))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Mocked IOException");
    }
  }

  @Test
  void getSizeWithException() {
    try (MockedStatic<Files> files = Mockito.mockStatic(Files.class)) {
      files.when(() -> Files.size(any()))
          .thenThrow(new IOException("Mocked IOException"));

      final FileSystemNode mockNode = new FileSystemNodeImpl("x", "y", false, 1);

      FileService fs = getFs();
      assertThatThrownBy(() -> fs.getSize(mockNode))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Mocked IOException");
    }
  }

  @Test
  void mkDirectoriesWithException() {
    try (MockedStatic<Files> files = Mockito.mockStatic(Files.class)) {
      files.when(() -> Files.createDirectories(any()))
          .thenThrow(new IOException("Mocked IOException"));

      FileService fs = getFs();
      assertThatThrownBy(() -> fs.mkDirectories("x"))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Mocked IOException");
    }
  }

  @Test
  void deleteWithException() {
    try (MockedStatic<Files> files = Mockito.mockStatic(Files.class)) {
      files.when(() -> Files.delete(any()))
          .thenThrow(new IOException("Mocked IOException"));

      final FileSystemNode mockNode = new FileSystemNodeImpl("x", "y", false, 1);

      FileService fs = getFs();
      assertThatThrownBy(() -> fs.delete(mockNode))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Mocked IOException");
    }
  }

  @Test
  void getCreationTime() {
    putObject("x", "-");

    FileService fs = getFs();
    FileSystemNode fileNode = fs.getFileNode("x");
    assertThat(fs.getCreationTime(fileNode, ZoneId.systemDefault()))
        .isNotNull()
        .isBefore(LocalDateTime.now());
  }

  @Test
  void getLastModifiedTime() {
    putObject("x", "-");

    FileSystemNode fileNode = getFs().getFileNode("x");
    assertThat(getFs().getLastModifiedTime(fileNode, ZoneId.systemDefault()))
        .isNotNull()
        .isBefore(LocalDateTime.now());
  }

  @Test
  void readAttributesException() {
    try (MockedStatic<Files> mockFiles = Mockito.mockStatic(Files.class)) {
      mockFiles.when(
              () -> Files.readAttributes(any(), ArgumentMatchers.<Class<BasicFileAttributes>>any()))
          .thenThrow(new IOException("Mocked IOException"));

      final FileSystemNode mockNode = new FileSystemNodeImpl("x", "y", false, 1);
      FileService fs = getFs();
      ZoneId zoneId = ZoneId.systemDefault();
      assertThatThrownBy(
          () -> fs.getLastModifiedTime(mockNode, zoneId))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Mocked IOException");
      assertThatThrownBy(() -> fs.getCreationTime(mockNode, zoneId))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Mocked IOException");
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

}