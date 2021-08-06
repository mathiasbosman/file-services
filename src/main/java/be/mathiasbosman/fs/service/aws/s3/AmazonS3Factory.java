package be.mathiasbosman.fs.service.aws.s3;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.Builder;

@Builder
public class AmazonS3Factory {

  private final String serviceEndpoint;
  private final Region region;
  private final String key;
  private final String secret;
  private final String bucket;
  private final boolean pathStyleAccessEnabled;
  private final boolean createBucketIfMissing;

  @SuppressWarnings("unused")
  public AmazonS3 toAmazonS3() {
    AmazonS3 s3 = AmazonS3ClientBuilder.standard()
        .withCredentials(
            new AWSStaticCredentialsProvider(new BasicAWSCredentials(key, secret)))
        .withEndpointConfiguration(
            new EndpointConfiguration(serviceEndpoint, region != null ? region.getName() : null))
        .withPathStyleAccessEnabled(pathStyleAccessEnabled)
        .build();
    if (createBucketIfMissing && !s3.doesBucketExistV2(bucket)) {
      s3.createBucket(bucket);
    }
    return s3;
  }
}
