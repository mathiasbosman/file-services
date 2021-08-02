package be.mathiasbosman.fs.service.aws.s3;

import static org.assertj.core.api.Assertions.assertThat;

import be.mathiasbosman.fs.domain.FileNode;
import be.mathiasbosman.fs.service.AbstractFileServiceTest;
import be.mathiasbosman.fs.service.FileService;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.StringInputStream;
import com.google.common.base.Charsets;
import java.io.ByteArrayInputStream;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class S3FileSystemTest extends AbstractFileServiceTest {

  private final AmazonS3 s3;
  private final String bucketName = "test";
  private final String prefix = "sandbox/";

  public S3FileSystemTest() {
    // see docker compose
    AWSCredentials credentials = new BasicAWSCredentials("minio_key", "minio_secret");
    String hostAddress = "http://localhost:11000";
    ClientConfiguration clientConfiguration = new ClientConfiguration();
    clientConfiguration.setSignerOverride("AWSS3V4SignerType");
    s3 = AmazonS3ClientBuilder.standard()
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
            hostAddress, Regions.EU_WEST_1.name()
        ))
        .withClientConfiguration(clientConfiguration)
        .withPathStyleAccessEnabled(true)
        .build();
  }

  @Before
  public void setup() {
    cleanUp();
    s3.createBucket(bucketName);
    setFs(new S3FileSystem(this.s3, bucketName, prefix));
  }

  @After
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
