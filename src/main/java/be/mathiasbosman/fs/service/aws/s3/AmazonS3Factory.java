package be.mathiasbosman.fs.service.aws.s3;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.experimental.UtilityClass;

/**
 * Factory method for {@link AmazonS3} using the {@link AmazonS3ClientBuilder#standard()} by
 * default. The client builder can be overriden if need bee.
 *
 * @author mathiasbosman
 * @see AmazonS3
 * @see AmazonS3ClientBuilder
 * @since 0.0.1
 */
@UtilityClass
public class AmazonS3Factory {

  public static AmazonS3 toAmazonS3(String serviceEndpoint, Region region, String key,
      String secret,
      String bucket, boolean pathStyleAccessEnabled, boolean createBucketIfMissing) {
    return toAmazonS3(AmazonS3ClientBuilder.standard(), serviceEndpoint, region, key, secret,
        bucket, pathStyleAccessEnabled, createBucketIfMissing);
  }

  public static AmazonS3 toAmazonS3(AmazonS3ClientBuilder clientBuilder,
      String serviceEndpoint, Region region, String key, String secret,
      String bucket, boolean pathStyleAccessEnabled, boolean createBucketIfMissing) {
    AmazonS3 s3 = clientBuilder
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
