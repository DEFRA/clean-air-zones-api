package uk.gov.caz.vcc.configuration;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.google.common.base.Strings;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * Configuration class for instantiating AWS clients.
 *
 */
@Configuration
@Slf4j
public class AwsConfiguration {

  private static final String DUMMY = "dummy";

  @Value("${cloud.aws.region.static}")
  private String region;
  
  @Bean
  @Profile("!integration-tests & !localstack")
  public S3Client s3Client() {
    return S3Client.create();
  }

  /**
   * Returns an instance of {@link S3Client} which is used to retrieve CSV files from S3 mocked by
   * Localstack.
   *
   * @param s3Endpoint An endpoint of mocked S3. Cannot be empty or {@code null}
   * @return An instance of {@link S3Client}
   * @throws IllegalStateException if {@code s3Endpoint} is null or empty
   */
  @Profile({"integration-tests", "localstack"})
  @Bean
  public S3Client s3LocalstackClient(@Value("${aws.s3.endpoint:}") String s3Endpoint) {
    if (Strings.isNullOrEmpty(s3Endpoint)) {
      throw new IllegalStateException("S3 endpoint must be overridden when running with "
          + "Localstack! Please set in 'aws.s3.endpoint' property");
    }

    log.info("Using '{}' as S3 Endpoint", s3Endpoint);

    return S3Client.builder()
        .region(Region.EU_WEST_2)
        .endpointOverride(URI.create(s3Endpoint))

        // unfortunately there is a checksum error when uploading a file to localstack
        // so the check must be disabled
        .serviceConfiguration(S3Configuration.builder().checksumValidationEnabled(false).build())
        .credentialsProvider(() -> AwsBasicCredentials.create(DUMMY, DUMMY))
        .build();
  }

  /**
   * Returns an instance of {@link AmazonSQS} which is used to send a message to a SQS queue mocked
   * by Localstack.
   *
   * @param sqsEndpoint An endpoint of mocked SQS. Cannot be empty or {@code null}
   * @return An instance of {@link AmazonSQS}
   * @throws IllegalStateException if {@code sqsEndpoint} is null or empty
   */
  @Bean
  @Primary
  @Profile({"integration-tests", "localstack"})
  public AmazonSQS sqsLocalstackClient(@Value("${aws.sqs.endpoint:}") String sqsEndpoint) {
    log.info("Running Spring-Boot app locally using Localstack. ");

    if (Strings.isNullOrEmpty(sqsEndpoint)) {
      throw new IllegalStateException("SQS endpoint must be overridden when running with "
          + "Localstack! Please set in 'aws.sqs.endpoint' property");
    }

    log.info("Using '{}' as SQS Endpoint", sqsEndpoint);

    return AmazonSQSClientBuilder.standard().withCredentials(dummyCredentialsProvider())
        .withEndpointConfiguration(new EndpointConfiguration(sqsEndpoint, region)).build();
  }

  private AWSStaticCredentialsProvider dummyCredentialsProvider() {
    return new AWSStaticCredentialsProvider(
        new BasicAWSCredentials("dummy-access-key", "dummy-secret-key"));
  }

  /**
   * Creates the AmazonSqsAsync Bean. Overriding the default SQS Client Bean config because
   * AmazonSqsBufferedAsyncClient is not currently supported by FIFO queues.
   * 
   * @return the AmazonSqsAsync Bean
   */
  @Bean
  @Primary
  @Profile("!integration-tests & !localstack")
  public AmazonSQS amazonSqs() {
    return AmazonSQSClientBuilder.standard().withRegion(region).build();
  }

}

  