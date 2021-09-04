package be.mathiasbosman.fs.service.aws.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import be.mathiasbosman.fs.AbstractContainerTest;
import be.mathiasbosman.fs.ContainerServiceDto;
import be.mathiasbosman.fs.domain.FileSystemNode;
import be.mathiasbosman.fs.service.FileService;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.base.Charsets;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Slf4j
class S3FileServiceTest extends AbstractContainerTest {

  private static final String dockerComposeFile = "src/test/resources/docker/docker-compose-minio.yml";
  private static final String dockerS3Service = "fs-test-minio";
  private static final int dockerS3Port = 9000;

  private final AmazonS3 s3;
  private final String bucketName = "test";

  S3FileServiceTest() {
    super(dockerComposeFile, new ContainerServiceDto(dockerS3Service, dockerS3Port));
    // see docker compose
    String endpoint = "http://localhost:" + dockerS3Port;
    log.debug("Creating s3 connection on {}", endpoint);
    s3 = AmazonS3Factory.toAmazonS3(endpoint,
        Region.EU_London.toAWSRegion(), "minio_key", "minio_secret", bucketName,
        true, false);
  }

  @BeforeEach
  void setup() {
    cleanUp();
    s3.createBucket(bucketName);
    setFs(new S3FileService(s3, bucketName));
  }

  @AfterEach
  void cleanUp() {
    if (!s3.doesBucketExistV2(bucketName)) {
      return;
    }
    AtomicReference<ObjectListing> objectListing = new AtomicReference<>(
        s3.listObjects(bucketName));
    Iterator<S3ObjectSummary> iterator = objectListing.get().getObjectSummaries().iterator();
    while (true) {
      while (iterator.hasNext()) {
        s3.deleteObject(bucketName, iterator.next().getKey());
      }
      if (objectListing.get().isTruncated()) {
        objectListing.set(s3.listNextBatchOfObjects(objectListing.get()));
      } else {
        break;
      }
    }
    s3.deleteBucket(bucketName);
  }

  @Override
  protected void putDirectory(String path) {
    getFs().mkDirectories(path);
  }

  private void putObject(String path, String data, String contentType) {
    byte[] bytes = data.getBytes(Charsets.UTF_8);
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentEncoding("aws-chunked");
    metadata.setContentType(contentType);
    metadata.setContentLength(bytes.length);
    log.debug("Putting remote object to {}", path);
    s3.putObject(bucketName, path, new ByteArrayInputStream(bytes), metadata);
  }

  @Override
  protected void assertExists(String path) {
    assertThat(s3.doesObjectExist(bucketName, path)).isTrue();
  }

  @Override
  protected void assertDirectoryExists(String path) {
    assertThat(getFs().isDirectory(path)).isTrue();
  }

  @Override
  protected void assertNotExists(String path) {
    assertThat(s3.doesObjectExist(bucketName, path)).isFalse();
  }

  @Override
  protected String getContent(String path) {
    return s3.getObjectAsString(bucketName, path);
  }

  @Override
  protected void putObject(String path, String data) {
    putObject(path, data, "text/plain");
  }

  @Override
  protected void putImageObject(String path) {
    putObject(path, "-", "image/jpeg");
  }

  @Override
  protected String getRemotePath(String path) {
    return path;
  }

  @Test
  void delete() {
    putDirectory("x");
    putObject("x/a", "-");
    FileSystemNode nodeToDelete = getFs().getFileNode("x");
    assertThatThrownBy(() -> getFs().delete(nodeToDelete))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Directory not empty for deletion");
  }

  @Test
  void getCreationTime() {
    putObject("x", "-");
    FileSystemNode node = getFs().getFileNode("x");
    FileService fs = getFs();
    ZoneId zoneId = ZoneId.systemDefault();
    assertThatThrownBy(() -> fs.getCreationTime(node, zoneId))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void getLastModifiedTime() {
    putObject("x", "-");
    FileSystemNode fileNode = getFs().getFileNode("x");
    LocalDateTime lastModifiedTimeFile = getFs()
        .getLastModifiedTime(fileNode, ZoneId.systemDefault());
    assertThat(lastModifiedTimeFile).isNotNull();
  }


  @Test
  void streamDirectory() {
    putObject("x/a", "-");
    putObject("x/z", "-");
    putObject("x/b/a", "-");
    List<FileSystemNode> files = getFs().streamDirectory(getFs().getFileNode("x"))
        .collect(Collectors.toList());
    assertThat(files).hasSize(3);
  }

}
