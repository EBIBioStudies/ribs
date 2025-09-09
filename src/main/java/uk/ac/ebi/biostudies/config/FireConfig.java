package uk.ac.ebi.biostudies.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:fire.properties")
public class FireConfig {
  @Value("${fire.credentials.access-key}")
  private String accessKey;

  @Value("${fire.credentials.secret-key}")
  private String secretKey;

  @Value("${fire.region}")
  private String region;

  @Value("${fire.s3.endpoint}")
  private String endpoint;

  @Value("${fire.s3.bucket}")
  private String bucketName;

  @Value("${fire.s3.connection.pool}")
  private Integer conPoolSize;

  @Value("${fire.s3.connection.timeout}")
  private int conTimeout;

  @Value("${fire.s3.connection.socket.timeout}")
  private int sockTimeout;

  @Value("${fire.s3.connection.magetab.pool}")
  private Integer mergetabPoolSize;

  @Value("${fire.s3.ftp.redirect.enabled}")
  private boolean isFtpRedirectEnabled;

  private AmazonS3 amazonS3Client(int poolSize) {
    BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(accessKey, secretKey);
    AwsClientBuilder.EndpointConfiguration endpointConfiguration =
        new AwsClientBuilder.EndpointConfiguration(endpoint, region); // The region is not important
    return AmazonS3Client.builder()
        .withClientConfiguration(
            new ClientConfiguration()
                .withMaxConnections(poolSize)
                .withConnectionTimeout(conTimeout)
                .withSocketTimeout(sockTimeout))
        .withEndpointConfiguration(endpointConfiguration)
        .withPathStyleAccessEnabled(true)
        .withCredentials(new AWSStaticCredentialsProvider(basicAWSCredentials))
        .build();
  }

  @Bean("S3DownloadClient")
  public AmazonS3 amazonS3DownloadClient() {
    return amazonS3Client(conPoolSize);
  }

  @Bean("S3MageTabClient")
  public AmazonS3 amazonS3mergeTabClient() {
    return amazonS3Client(mergetabPoolSize);
  }

  public String getAccessKey() {
    return accessKey;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public String getRegion() {
    return region;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public String getBucketName() {
    return bucketName;
  }

  public boolean isFtpRedirectEnabled() {
    return isFtpRedirectEnabled;
  }
}
