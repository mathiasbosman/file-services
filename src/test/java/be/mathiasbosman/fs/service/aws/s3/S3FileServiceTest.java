package be.mathiasbosman.fs.service.aws.s3;

import static org.assertj.core.api.Assertions.assertThat;

import be.mathiasbosman.fs.AbstractFileServiceContainerTest;
import be.mathiasbosman.fs.domain.FileNode;
import be.mathiasbosman.fs.service.FileService;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.StringInputStream;
import com.google.common.base.Charsets;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class S3FileServiceTest extends AbstractFileServiceContainerTest {

  private static final String dockerComposeFile = "src/test/resources/docker/docker-compose.yml";
  private static final String dockerS3Service = "fs-test-minio";
  private static final int dockerS3Port = 9000;

  private final AmazonS3 s3;
  private final String bucketName = "test";
  private final String prefix = "sandbox/";

  public S3FileServiceTest() {
    // see docker compose
    s3 = AmazonS3Factory.builder()
        .bucket(bucketName)
        .serviceEndpoint("http://localhost:" + dockerS3Port)
        .key("minio_key")
        .secret("minio_secret")
        .pathStyleAccessEnabled(true)
        .region(Region.EU_London.toAWSRegion())
        .build().toAmazonS3();
  }

  @Override
  public DockerComposeContainer<?> createContainer() {
    return new DockerComposeContainer<>(
        new File(dockerComposeFile))
        .withExposedService(dockerS3Service, dockerS3Port, Wait.forListeningPort())
        .withLogConsumer(dockerS3Service, getContainerLogConsumer(dockerS3Service))
        .withLocalCompose(true)
        .withPull(false);
  }

  @BeforeEach
  public void setup() {
    cleanUp();
    s3.createBucket(bucketName);
    setFs(new S3FileService(this.s3, bucketName, prefix));
  }

  @AfterEach
  public void cleanUp() {
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
  protected void putFolder(String path) {
    getFs().mkFolders(path);
  }

  private void putObject(String path, String data, String contentType) {
    byte[] bytes = data.getBytes(Charsets.UTF_8);
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentEncoding("aws-chunked");
    metadata.setContentType(contentType);
    metadata.setContentLength(bytes.length);
    s3.putObject(bucketName, prefix + path, new ByteArrayInputStream(bytes), metadata);
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
  protected void assertExists(String path) {
    assertThat(s3.doesObjectExist(bucketName, prefix + path)).isTrue();
  }

  @Override
  protected void assertFolderExists(String path) {
    assertThat(getFs().isFolder(path)).isTrue();
  }

  @Override
  protected void assertNotExists(String path) {
    assertThat(s3.doesObjectExist(bucketName, prefix + path)).isFalse();
  }

  @Override
  protected String getContent(String path) {
    return s3.getObjectAsString(bucketName, prefix + path);
  }

  @Test
  public void copy() throws Exception {
    FileService fs = getFs();
    fs.save(new StringInputStream("information"), "folder/file.txt");
    fs.save(new StringInputStream("more data"), "folder/more.txt");
    fs.copy(fs.get("folder"), "copy");
    assertThat(fs.exists("copy/more.txt")).isTrue();
    assertThat(fs.read("copy/file.txt")).isEqualTo("information");
  }

  @Test
  public void delete() {
    FileService fs = getFs();
    putObject("x/.folder", "-");
    fs.delete(fs.get("x"));
    assertNotExists("x/.folder");
    assertNotExists("x");
  }

  @Test
  public void stream() {
    putObject("x/a", "-");
    putObject("x/z", "-");
    putObject("x/b/a", "-");
    Stream<FileNode> stream = getFs().streamDirectory(getFs().get("x"));
    assertThat(stream).isNotNull();
    List<FileNode> collected = stream.collect(Collectors.toList());
    assertThat(collected).hasSize(3);
  }

}