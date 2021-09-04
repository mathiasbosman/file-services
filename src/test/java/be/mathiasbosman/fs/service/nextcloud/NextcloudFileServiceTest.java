package be.mathiasbosman.fs.service.nextcloud;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.newOutputStream;
import static org.assertj.core.api.Assertions.assertThat;

import be.mathiasbosman.fs.AbstractContainerTest;
import be.mathiasbosman.fs.ContainerServiceDto;
import be.mathiasbosman.fs.service.AbstractFileService;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.aarboard.nextcloud.api.NextcloudConnector;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.util.StringUtils;

@Slf4j
class NextcloudFileServiceTest extends AbstractContainerTest {

  private static final String dockerComposeFile = "src/test/resources/docker/docker-compose-nextcloud.yml";
  private static final String dockerDbService = "fs-test-nextcloud-db";
  private static final int dockerDbPort = 5432;
  private static final String dockerNextcloudService = "fs-test-nextcloud-app";
  private static final int dockerNextcloudPort = 80;
  private static final String remoteDir = "testdir";
  // as the connector requires File inputs we need to create them locally
  private final FileSystem fileSystem = FileSystems.getDefault();
  private final Path workdir = fileSystem.getPath("/tmp/" + System.identityHashCode(this) + "/");
  private final NextcloudConnector connector;

  public NextcloudFileServiceTest() {
    super(dockerComposeFile, Arrays.asList(
        new ContainerServiceDto(dockerDbService, dockerDbPort),
        new ContainerServiceDto(dockerNextcloudService, dockerNextcloudPort)
    ));
    connector = new NextcloudConnector("localhost", false, 9002,
        "admin", "admin");
  }

  protected String getRemotePath(String path) {
    return StringUtils.hasLength(path)
        ? AbstractFileService.combine(remoteDir, path)
        : remoteDir;
  }

  @BeforeEach
  void setup() throws IOException {
    cleanup();
    connector.createFolder(remoteDir);
    createDirectories(workdir);
    setFs(new NextcloudFileService(connector));
  }

  @AfterEach
  void cleanup() throws IOException {
    // need to clean all files remotely
    if (connector.folderExists(remoteDir)) {
      connector.deleteFolder(remoteDir);
    }
    FileUtils.deleteDirectory(workdir.toFile());
  }

  @Override
  protected void assertExists(String path) {
    assertThat(connector.fileExists(getRemotePath(path))).isTrue();
  }

  @Override
  protected void assertDirectoryExists(String path) {
    assertThat(connector.folderExists(getRemotePath(path))).isTrue();
  }

  @Override
  protected void assertNotExists(String path) {
    assertThat(connector.fileExists(getRemotePath(path))
        || connector.folderExists(getRemotePath(path))).isFalse();
  }

  @Override
  protected String getContent(String path) {
    try {
      InputStream inputStream = connector.downloadFile(getRemotePath(path));
      return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  protected void putDirectory(String path) {
    Pair<String, String> split = AbstractFileService.split(path);
    if (split.getLeft() != null) {
      putDirectory(split.getLeft());
    }
    if (!connector.folderExists(getRemotePath(path))) {
      log.debug("Creating remote folder {}", path);
      connector.createFolder(getRemotePath(path));
    }
  }

  @Override
  protected void putObject(String location, String data) {

    try {
      String targetLocation = getRemotePath(location);
      Path path = workdir.resolve(location);
      String directory = AbstractFileService.split(location).getLeft();
      if (directory != null) {
        String dirPath = path.getParent().toString();
        log.trace("Creating local directory {}", dirPath);
        Files.createDirectories(workdir.resolve(dirPath));
        putDirectory(directory);
      }

      try (OutputStream out = newOutputStream(path)) {
        out.write(data.getBytes());
        out.flush();
      }
      log.debug("Uploading file {} to {}", path, targetLocation);
      connector.uploadFile(path.toFile(), targetLocation);
      Files.delete(path);
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  protected void putImageObject(String path) {
    putObject(path, "-");
  }
}
