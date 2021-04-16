package uk.gov.caz.whitelist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.gov.caz.db.exporter.destination.s3.AwsS3Destination;
import uk.gov.caz.db.exporter.destination.s3.AwsS3DestinationProvider;
import uk.gov.caz.db.exporter.destination.s3.AwsS3DestinationUriGenerationStrategy;
import uk.gov.caz.db.exporter.postgresql.PostgresDatabaseExporter;

public class WhitelistVehiclesExporterTest {

  private static final String S3_BUCKET = "Bucket";
  private static final String MIME_TYPE = "text/csv";
  private static final String DESTINATION_PATH = "http://path.com";
  private PostgresDatabaseExporter postgresDatabaseExporter;
  private AwsS3DestinationProvider awsS3DestinationProvider;
  private AwsS3DestinationUriGenerationStrategy awsS3DestinationUriGenerationStrategy;
  private WhitelistVehiclesExporter service;
  private ArgumentCaptor<String> s3ObjectNameCaptor = ArgumentCaptor.forClass(String.class);

  @BeforeEach
  public void setup() {
    postgresDatabaseExporter = mock(PostgresDatabaseExporter.class);
    awsS3DestinationProvider = mock(AwsS3DestinationProvider.class);
    awsS3DestinationUriGenerationStrategy = mock(AwsS3DestinationUriGenerationStrategy.class);
    service = new WhitelistVehiclesExporter(postgresDatabaseExporter,
        awsS3DestinationProvider, awsS3DestinationUriGenerationStrategy, S3_BUCKET);
  }

  @Test
  public void exportCorrectlyDelegatesToDownstreamServicesAndGeneratesValidObjectName() {
    // given
    AwsS3Destination awsS3Destination = mock(AwsS3Destination.class);
    mockAwsS3DestinationProviderToProvide(awsS3Destination);
    when(postgresDatabaseExporter.exportTo(awsS3Destination)).thenReturn(uri(DESTINATION_PATH));

    // when
    String destinationPath = service.export();

    // then
    assertThat(destinationPath).isEqualTo(DESTINATION_PATH);
    assertThat(s3ObjectNameCaptor.getValue())
        .matches("whitelist_vehicles_\\d{4}-\\d{2}-\\d{2}_\\d{6}.csv");
  }

  private void mockAwsS3DestinationProviderToProvide(AwsS3Destination awsS3Destination) {
    AwsS3DestinationProvider.Builder builder = mock(AwsS3DestinationProvider.Builder.class);
    when(awsS3DestinationProvider.provide()).thenReturn(builder);
    when(builder.inS3Bucket(S3_BUCKET)).thenReturn(builder);
    when(builder.inS3Object(s3ObjectNameCaptor.capture())).thenReturn(builder);
    when(builder.usingUriGenerator(awsS3DestinationUriGenerationStrategy)).thenReturn(builder);
    when(builder.withMimeType(MIME_TYPE)).thenReturn(builder);
    when(builder.inDestination()).thenReturn(awsS3Destination);
  }

  private URI uri(String desintationPath) {
    return URI.create(desintationPath);
  }
}
