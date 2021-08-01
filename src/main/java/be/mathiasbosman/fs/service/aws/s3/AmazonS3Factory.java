package be.mathiasbosman.fs.service.aws.s3;

import static com.amazonaws.Protocol.HTTP;
import static com.amazonaws.regions.Regions.US_EAST_1;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

public class AmazonS3Factory {
  private String serviceEndpoint;

  public AmazonS3 toAmazonS3() {
    return toAmazonS3(new DefaultAWSCredentialsProviderChain());
  }

  public AmazonS3 toAmazonS3(String username, String password) {
    return toAmazonS3(new AWSCredentialsProviderChain(
        new AWSCredentialsProviderChain(
            new LazyBasicAWSCredentialsProvider(username, password),
            new DefaultAWSCredentialsProviderChain()
        )
    ));
  }

  public void setServiceEndpoint(String serviceEndpoint) {
    this.serviceEndpoint = serviceEndpoint;
  }

  private AmazonS3 toAmazonS3(AWSCredentialsProvider provider) {
    ClientConfiguration config = new ClientConfiguration();
    config.setConnectionTimeout(50 * 1000);
    config.setSocketTimeout(50 * 1000);
    config.setProtocol(HTTP);
    config.setSignerOverride("AWSS3V4SignerType");

    return AmazonS3ClientBuilder.standard()
        .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
            serviceEndpoint,
            US_EAST_1.getName()
        ))
        .withPathStyleAccessEnabled(true)
        .withClientConfiguration(config)
        .withCredentials(provider)
        .build();
  }

  private static class LazyBasicAWSCredentialsProvider implements AWSCredentialsProvider {
    private final String username;
    private final String password;

    public LazyBasicAWSCredentialsProvider(String username, String password) {
      this.username = username;
      this.password = password;
    }

    @Override
    public AWSCredentials getCredentials() {
      return new BasicAWSCredentials(username, password);
    }

    @Override
    public void refresh() {
    }
  }
}
