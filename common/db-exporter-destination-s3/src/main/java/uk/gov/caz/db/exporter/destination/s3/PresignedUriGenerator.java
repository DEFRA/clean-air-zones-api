package uk.gov.caz.db.exporter.destination.s3;

import com.amazonaws.services.s3.AmazonS3;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.With;
import uk.gov.caz.db.exporter.exception.DatabaseExportException;

/**
 * Generates presigned URI to AWS S3 object. Usage: use existing bean from Spring's Configuration to
 * use default 24 hours validation window or call 'withExpirationInHours(xx)' to produce new
 * instance of {@link PresignedUriGenerator} with desired expiration window.
 */
@RequiredArgsConstructor
public class PresignedUriGenerator implements AwsS3DestinationUriGenerationStrategy {

  /**
   * Defines for how many hours presigned URI will be valid.
   */
  @With
  private final int expirationInHours;

  @Override
  public URI getUri(String s3Bucket, String s3ObjectName, AmazonS3 amazonS3) {
    try {
      URL presignedUrl = amazonS3.generatePresignedUrl(s3Bucket, s3ObjectName, expirationDate());
      return presignedUrl.toURI();
    } catch (Exception exception) {
      throw new DatabaseExportException("Unable to export database data to AWS S3", exception);
    }
  }

  /**
   * Generate {@link Date} set as 'expirationInHours' into the future.
   */
  private Date expirationDate() {
    return Date.from(
        LocalDateTime.now().plus(expirationInHours, ChronoUnit.HOURS).toInstant(ZoneOffset.UTC));
  }
}
