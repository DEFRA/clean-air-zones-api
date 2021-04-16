package uk.gov.caz.db.exporter.destination.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.s3.AmazonS3;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.caz.db.exporter.exception.DatabaseExportException;

public class PresignedUriGeneratorTest {

  private final static int DEFAULT_EXPIRATION_IN_HOURS = 24;
  private final static int EXPIRATION_IN_HOURS = 1;
  private final static String S3_BUCKET = "Bucket";
  private final static String S3_OBJECT = "Object";
  private final static String SOME_URL = "http://file.com";

  private PresignedUriGenerator presignedUriGenerator;
  private AmazonS3 amazonS3;

  @BeforeEach
  public void setup() {
    presignedUriGenerator = new PresignedUriGenerator(DEFAULT_EXPIRATION_IN_HOURS);
    amazonS3 = mock(AmazonS3.class);
  }

  @Test
  public void correctlyGeneratesDestinationUriForDefaultExpirationWindow()
      throws MalformedURLException {
    // given
    mockS3ClientToReturnValidPresignedUrl();

    // when
    URI destinationUri = generateDestinationUri();

    // then
    assertThat(destinationUri.toString()).isEqualTo(SOME_URL);
  }

  @Test
  public void correctlyGeneratesDestinationUriForCustomExpirationWindow()
      throws MalformedURLException {
    // given
    mockS3ClientToReturnValidPresignedUrl();
    presignedUriGenerator = presignedUriGenerator.withExpirationInHours(EXPIRATION_IN_HOURS);

    // when
    URI destinationUri = generateDestinationUri();

    // then
    assertThat(destinationUri.toString()).isEqualTo(SOME_URL);
  }

  @Test
  public void correctlyGeneratesDestinationUriForExplicitlySetDefaultExpirationWindow()
      throws MalformedURLException {
    // given
    mockS3ClientToReturnValidPresignedUrl();
    presignedUriGenerator = presignedUriGenerator
        .withExpirationInHours(DEFAULT_EXPIRATION_IN_HOURS);

    // when
    URI destinationUri = generateDestinationUri();

    // then
    assertThat(destinationUri.toString()).isEqualTo(SOME_URL);
  }

  @Test
  public void correctlyMapsExceptionsToDatabaseExportException() {
    // given
    when(amazonS3.generatePresignedUrl(eq(S3_BUCKET), eq(S3_OBJECT), any(Date.class)))
        .thenThrow(new RuntimeException("error"));

    // when
    Throwable throwable = catchThrowable(
        () -> presignedUriGenerator.getUri(S3_BUCKET, S3_OBJECT, amazonS3));

    // then
    assertThat(throwable).isInstanceOf(DatabaseExportException.class)
        .hasMessage("Unable to export database data to AWS S3");
  }

  private URI generateDestinationUri() {
    return presignedUriGenerator.getUri(S3_BUCKET, S3_OBJECT, amazonS3);
  }

  private void mockS3ClientToReturnValidPresignedUrl() throws MalformedURLException {
    when(amazonS3.generatePresignedUrl(eq(S3_BUCKET), eq(S3_OBJECT), any(Date.class)))
        .thenReturn(new URL(SOME_URL));
  }
}
