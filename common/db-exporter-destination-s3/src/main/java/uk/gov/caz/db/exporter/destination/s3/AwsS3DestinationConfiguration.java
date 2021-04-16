package uk.gov.caz.db.exporter.destination.s3;

import static uk.gov.caz.db.exporter.destination.s3.AwsHelpers.areWeRunningLocallyUsingSam;
import static uk.gov.caz.db.exporter.destination.s3.AwsHelpers.getAwsAccessKeyFromEnvVar;
import static uk.gov.caz.db.exporter.destination.s3.AwsHelpers.getAwsProfileFromEnvVar;
import static uk.gov.caz.db.exporter.destination.s3.AwsHelpers.getAwsRegionFromEnvVar;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration class that provides AWS S3 database export destination. Uses old V1 Amazon SDK S3
 * client because it is required by tremendously helpful Spring's {@link
 * org.springframework.cloud.aws.core.io.s3.SimpleStorageResource}.
 */
@Configuration
@Slf4j
public class AwsS3DestinationConfiguration {

  /**
   * Default amount of hours for which presigned URI will be valid.
   */
  private static final int DEFAULT_24_HOURS_URI_EXPIRATION_WINDOW = 24;

  /**
   * Creates {@link AwsS3DestinationProvider} that can be used by clients to obtain new instances of
   * {@link AwsS3Destination} class needed to export database to AWS S3.
   *
   * @param amazonS3 {@link AmazonS3} bean that allows to do operations over S3.
   * @return {@link AwsS3DestinationProvider} that can be used by clients to obtain new instances of
   *     {@link AwsS3Destination} class needed to export database to AWS S3.
   */
  @Bean
  public AwsS3DestinationProvider awsS3DestinationProvider(AmazonS3 amazonS3) {
    return new AwsS3DestinationProvider(amazonS3);
  }

  /**
   * Creates a default {@link AwsS3DestinationUriGenerationStrategy} implementation using {@link
   * PresignedUriGenerator} that generates URIs valid for 24 hours. This bean can be easily
   * reconfigured by the caller by making a call to '.withExpirationHours(xx)' to produce customized
   * {@link PresignedUriGenerator}.
   *
   * @return {@link AwsS3DestinationUriGenerationStrategy} implementation using {@link
   *     PresignedUriGenerator}.
   */
  @Bean
  public AwsS3DestinationUriGenerationStrategy presignedUriGeneratorValidFor24Hours() {
    return new PresignedUriGenerator(DEFAULT_24_HOURS_URI_EXPIRATION_WINDOW);
  }

  /**
   * Returns an instance of {@link AmazonS3} client to do operations over S3 mocked by Localstack.
   *
   * @param s3Endpoint An endpoint of mocked S3. Cannot be empty or {@code null}
   * @return An instance of {@link AmazonS3}
   * @throws IllegalStateException if {@code s3Endpoint} is null or empty
   */
  @Profile({"integration-tests", "localstack"})
  @Bean(name = "s3V1Client")
  public AmazonS3 s3V1LocalstackClient(@Value("${aws.s3.endpoint:}") String s3Endpoint) {
    log.info("Running Spring-Boot app locally using Localstack. "
        + "Using 'dummy' AWS credentials and 'eu-west-2' region.");

    if (s3Endpoint == null || s3Endpoint.isEmpty()) {
      throw new IllegalStateException("S3 endpoint must be overridden when running with "
          + "Localstack! Please set in 'aws.s3.endpoint' property");
    }

    log.info("Using '{}' as S3 Endpoint", s3Endpoint);

    return AmazonS3ClientBuilder.standard()
        .withEndpointConfiguration(new EndpointConfiguration(s3Endpoint, "eu-west-2"))
        .withCredentials(createDummyAwsCredentialsProvider())
        .withPayloadSigningEnabled(false)
        .withPathStyleAccessEnabled(true)
        .withChunkedEncodingDisabled(true)
        .build();
  }

  /**
   * Returns an instance of {@link AmazonS3} which is used to retrieve CSV files from S3. All
   * configuration MUST be specified by environment variables.
   *
   * @return An instance of {@link AmazonS3}
   */
  @Bean(name = "s3V1Client")
  @Profile("!integration-tests & !localstack")
  public AmazonS3 s3V1Client() {
    if (areWeRunningLocallyUsingSam()) {
      log.info("Running Lambda locally using SAM Local");
    }

    logAwsVariables();

    return AmazonS3ClientBuilder.standard().build();
  }

  /**
   * Creates and returns {@link AWSCredentialsProvider} with dummy credentials. Useful for
   * LocalStack configuration.
   */
  private AWSCredentialsProvider createDummyAwsCredentialsProvider() {
    return new AWSCredentialsProvider() {
      @Override
      public AWSCredentials getCredentials() {
        return new AWSCredentials() {
          @Override
          public String getAWSAccessKeyId() {
            return "dummy";
          }

          @Override
          public String getAWSSecretKey() {
            return "dummy";
          }
        };
      }

      @Override
      public void refresh() {
        // Not needed
      }
    };
  }

  /**
   * Logs AWS credentials-related env vars.
   */
  private void logAwsVariables() {
    String awsAccessKeyId = getAwsAccessKeyFromEnvVar();
    String awsRegion = getAwsRegionFromEnvVar();
    String awsProfile = getAwsProfileFromEnvVar();

    log.info("IAM env credentials: Access Key Id is '{}'; AWS Region is '{}'; AWS profile is '{}'",
        awsAccessKeyId,
        awsRegion,
        awsProfile);
  }
}
