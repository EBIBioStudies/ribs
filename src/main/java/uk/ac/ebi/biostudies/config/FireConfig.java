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
    @Bean
    public AmazonS3 amazonS3Client() {
        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(accessKey, secretKey);
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(
                endpoint, region); // The region is not important
        return AmazonS3Client.builder()
                .withClientConfiguration(new ClientConfiguration().withMaxConnections(conPoolSize))
                .withEndpointConfiguration(endpointConfiguration)
                .withPathStyleAccessEnabled(true)
                .withCredentials(new AWSStaticCredentialsProvider(basicAWSCredentials))
                .build();
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
}