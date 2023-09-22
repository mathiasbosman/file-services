package be.mathiasbosman.fs.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import be.mathiasbosman.fs.core.domain.FileSystemNode;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This test class tests the abstract file service
 */
@Slf4j
public class MockServiceTest extends AbstractFileServiceTest {

  private static Path tempDir;

  public MockServiceTest() throws IOException {
    tempDir = Files.createTempDirectory(null);
  }

  @AfterAll
  static void afterAll() throws IOException {
    log.debug("Deleting temp directory");
    FileUtils.deleteDirectory(tempDir.toFile());
  }

  @Override
  protected FileService getFs() {
    return new MockFileService(tempDir);
  }

  @BeforeEach
  void beforeEach() throws IOException {
    log.debug("Cleaning temp directory");
    FileUtils.cleanDirectory(tempDir.toFile());
  }

  @Override
  protected void assertExists(String path) {
    assertThat(Files.exists(tempDir.resolve(path))).isTrue();
  }

  @Override
  protected void assertDirectoryExists(String path) {
    assertThat(Files.isDirectory(tempDir.resolve(path))).isTrue();
  }

  @Override
  protected void assertNotExists(String path) {
    assertThat(Files.exists(tempDir.resolve(path))).isFalse();
  }

  @Override
  protected String getContent(String path) {
    try {
      return FileUtils.readFileToString(tempDir.resolve(path).toFile(), Charset.defaultCharset());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  protected void putDirectory(String path) {
    try {
      log.debug("Creating directory {}", path);
      Files.createDirectories(tempDir.resolve(path));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  protected void putObject(String path, String data) {
    log.debug("Putting object to {}", path);
    Path resolvedPath = tempDir.resolve(path);
    if (path.lastIndexOf('/') != -1) {
      putDirectory(resolvedPath.getParent().toString());
    }
    try (OutputStream out = Files.newOutputStream(resolvedPath)) {
      out.write(data.getBytes());
      out.flush();
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Test
  public void stream() {
    putObject("x/a");
    putObject("x/z");
    putObject("x/b/a");
    Stream<FileSystemNode> stream = getFs().streamDirectory(getFs().getFileNode("x"));
    assertThat(stream).isNotNull();
    List<FileSystemNode> collected = stream.collect(Collectors.toList());
    assertThat(collected).hasSize(5);
  }

  @Override
  protected void assertModifiedFolder(String path) {
    final FileSystemNode folderNode = getFs().getFileNode(path);
    final Date lastModified = folderNode.getLastModified();
    assertThat(lastModified).isNotNull();
  }
}
