package uk.gov.caz.db.exporter.destination.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.amazonaws.services.s3.AmazonS3;
import org.junit.jupiter.api.Test;

public class AwsS3DestinationProviderTest {

  @Test
  public void createsAwsS3DestinationByBuilder() {
    // given
    AmazonS3 amazonS3 = mock(AmazonS3.class);
    AwsS3DestinationUriGenerationStrategy awsS3DestinationUriGenerationStrategy = mock(
        AwsS3DestinationUriGenerationStrategy.class);
    AwsS3DestinationProvider awsS3DestinationProvider = new AwsS3DestinationProvider(amazonS3);

    // when
    AwsS3Destination awsS3Destination = awsS3DestinationProvider.provide()
        .inS3Bucket("Bucket")
        .inS3Object("File")
        .withMimeType("text/csv")
        .usingUriGenerator(awsS3DestinationUriGenerationStrategy)
        .inDestination();

    // then
    assertThat(awsS3Destination).isNotNull();
  }
}
