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
 * default. The client builder can be overridden if need bee.
 *
 * @author mathiasbosman
 * @see AmazonS3
 * @see AmazonS3ClientBuilder
 * @since 0.0.1
 */
@UtilityClass
public class AmazonS3Factory {

  /**
   * Factor an AmazonS3 class.
   *
   * @param serviceEndpoint        The service endpoint url
   * @param region                 The AWS {@link Region} (optional)
   * @param key                    The S3 key
   * @param secret                 The S3 secret
   * @param bucket                 Name of the bucket used
   * @param pathStyleAccessEnabled If path style access should be enabled
   * @param createBucketIfMissing  If the bucket should be created if missing
   * @return instance of a standard {@link AmazonS3}
   * @see AmazonS3
   * @see AmazonS3ClientBuilder#standard()
   */
  public static AmazonS3 toAmazonS3(String serviceEndpoint, Region region, String key,
      String secret,
      String bucket, boolean pathStyleAccessEnabled, boolean createBucketIfMissing) {
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
