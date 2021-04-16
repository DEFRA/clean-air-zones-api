package uk.gov.caz.taxiregister.configuration;

import com.google.common.base.Strings;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.LambdaClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import uk.gov.caz.awslambda.AwsHelpers;

@Configuration
@Slf4j
public class AwsConfiguration {

  private static final String DUMMY = "dummy";
  private static final String RUNNING_SPRING_BOOT_APP_LOCALLY_USING_LOCALSTACK_AND_DUMMY_CREDENTIALS
      = "Running Spring-Boot app locally using Localstack."
      + "Using 'dummy' AWS credentials and 'eu-west-2' region.";
  private static final String RUNNING_LAMBDA_LOCALLY_USING_SAM_LOCAL
      = "Running Lambda locally using SAM Local";

  /**
   * Returns and instance of {@link LambdaClientBuilder} interface that is being used to create AWS
   * Lambda clients. In current implementation it is DefaultLambdaClientBuilder.
   *
   * @return An instance of {@link LambdaClientBuilder}.
   */
  @Bean
  public LambdaClientBuilder lambdaClientBuilder() {
    return LambdaClient.builder();
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
    log.info(RUNNING_SPRING_BOOT_APP_LOCALLY_USING_LOCALSTACK_AND_DUMMY_CREDENTIALS);

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
   * Returns an instance of {@link S3Client} which is used to retrieve CSV files from S3. All
   * configuration MUST be specified by environment variables.
   *
   * @return An instance of {@link S3Client}
   */
  @Bean
  @Profile("!integration-tests & !localstack")
  public S3Client s3Client() {
    if (AwsHelpers.areWeRunningLocallyUsingSam()) {
      log.info(RUNNING_LAMBDA_LOCALLY_USING_SAM_LOCAL);
    }

    logAwsVariables();

    return S3Client.create();
  }

  private void logAwsVariables() {
    String awsAccessKeyId = AwsHelpers.getAwsAccessKeyFromEnvVar();
    String awsRegion = AwsHelpers.getAwsRegionFromEnvVar();
    String awsProfile = AwsHelpers.getAwsProfileFromEnvVar();

    log.info("IAM env credentials: Access Key Id is '{}'; AWS Region is '{}'; AWS profile is '{}'",
        awsAccessKeyId,
        awsRegion,
        awsProfile);
  }

  /**
   * Returns an instance of {@link SqsClient} which is used to send a message to a SQS queue mocked
   * by Localstack.
   *
   * @param sqsEndpoint An endpoint of mocked SQS. Cannot be empty or {@code null}
   * @return An instance of {@link SqsClient}
   * @throws IllegalStateException if {@code sqsEndpoint} is null or empty
   */
  @Profile({"integration-tests", "localstack"})
  @Bean
  public SqsClient sqsLocalstackClient(@Value("${aws.sqs.endpoint:}") String sqsEndpoint) {
    log.info(RUNNING_SPRING_BOOT_APP_LOCALLY_USING_LOCALSTACK_AND_DUMMY_CREDENTIALS);

    if (Strings.isNullOrEmpty(sqsEndpoint)) {
      throw new IllegalStateException("SQS endpoint must be overridden when running with "
          + "Localstack! Please set in 'aws.sqs.endpoint' property");
    }

    log.info("Using '{}' as SQS Endpoint", sqsEndpoint);

    return SqsClient.builder()
        .region(Region.EU_WEST_2)
        .endpointOverride(URI.create(sqsEndpoint))
        .credentialsProvider(() -> AwsBasicCredentials.create(DUMMY, DUMMY))
        .build();
  }

  /**
   * Returns an instance of {@link S3Client} which is used to retrieve CSV files from S3. All
   * configuration MUST be specified by environment variables.
   *
   * @return An instance of {@link S3Client}
   */
  @Bean
  @Profile("!integration-tests & !localstack")
  public SqsClient sqsClient() {
    if (AwsHelpers.areWeRunningLocallyUsingSam()) {
      log.info(RUNNING_LAMBDA_LOCALLY_USING_SAM_LOCAL);
    }

    logAwsVariables();

    return SqsClient.create();
  }

  /**
   * Returns an instance of {@link SesClient} which is used to send emails. All configuration MUST
   * be specified by environment variables.
   *
   * @return An instance of {@link SesClient}
   */
  @Bean
  @Profile("!integration-tests & !localstack")
  public SesClient sesClient(
      @Value("${aws.ses.region}") String region,
      @Value("${aws.ses.use-role-credentials}") boolean useRoleCredentials,
      @Value("${aws.ses.accessKeyId}") String accessKeyId,
      @Value("${aws.ses.secretAccessKey}") String secretAccessKey
  ) {
    if (AwsHelpers.areWeRunningLocallyUsingSam()) {
      log.info(RUNNING_LAMBDA_LOCALLY_USING_SAM_LOCAL);
    }

    logAwsVariables();

    if (useRoleCredentials) {
      return SesClient.builder()
          .region(Region.of(region))
          .build();
    }

    return SesClient.builder()
        .region(Region.of(region))
        .credentialsProvider(() -> AwsBasicCredentials.create(accessKeyId, secretAccessKey))
        .build();
  }

  /**
   * Returns an instance of {@link SesClient} which is used to send emails. All configuration MUST
   * be specified by environment variables.
   *
   * @return An instance of {@link SesClient}
   */
  @Profile({"integration-tests", "localstack"})
  @Bean
  public SesClient localSesClient(@Value("${aws.ses.endpoint:}") String sesEndpoint) {
    log.info(RUNNING_SPRING_BOOT_APP_LOCALLY_USING_LOCALSTACK_AND_DUMMY_CREDENTIALS);

    if (Strings.isNullOrEmpty(sesEndpoint)) {
      throw new IllegalStateException("SES endpoint must be overridden when running with "
          + "Localstack! Please set in 'aws.ses.endpoint' property");
    }

    log.info("Using '{}' as SES Endpoint", sesEndpoint);

    return SesClient.builder()
        .region(Region.EU_WEST_2)
        .endpointOverride(URI.create(sesEndpoint))
        .credentialsProvider(() -> AwsBasicCredentials.create(DUMMY, DUMMY))
        .build();
  }
}