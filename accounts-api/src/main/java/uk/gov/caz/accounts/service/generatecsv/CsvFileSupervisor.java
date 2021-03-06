package uk.gov.caz.accounts.service.generatecsv;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import uk.gov.caz.accounts.service.generatecsv.exception.CsvExportException;

/**
 * This class acts as a supervisor around uploading csv file.
 */
@Slf4j
@Component
public class CsvFileSupervisor {

  private static final String TEXT_CSV = "text/csv";
  private final S3Client s3Client;
  private final CsvFileNameGenerator csvFileNameGenerator;
  private final CsvWriter csvWriter;
  private final CsvUrlGenerator csvUrlGenerator;
  private final String bucket;

  /**
   * Default constructor.
   */
  public CsvFileSupervisor(S3Client s3Client, CsvFileNameGenerator csvFileNameGenerator,
      CsvWriter csvWriter, @Value("${csv-export-bucket}") String bucket,
      CsvUrlGenerator csvUrlGenerator) {
    this.s3Client = s3Client;
    this.csvFileNameGenerator = csvFileNameGenerator;
    this.csvWriter = csvWriter;
    this.csvUrlGenerator = csvUrlGenerator;
    this.bucket = bucket;
  }

  /**
   * Upload csv file to s3.
   *
   * @param accountId ID of Account/Fleet.
   * @return {@link URL}.
   */
  public URL uploadCsvFileAndGetPresignedUrl(UUID accountId) {
    String fileName = prepareFileName(accountId);
    String error = String.format("Exception while uploading file %s/%s", bucket, fileName);
    try {
      s3Client.putObject(prepareRequestObject(accountId), prepareRequestBody(accountId));
      return csvUrlGenerator.getPresignedUrl(fileName);
    } catch (S3Exception e) {
      logAwsExceptionDetails(e);
      throw new CsvExportException(error);
    } catch (IOException e) {
      log.error(error);
      throw new CsvExportException(error);
    }
  }

  /**
   * Helper method to prepare put request object.
   */
  private PutObjectRequest prepareRequestObject(UUID accountId) {
    return PutObjectRequest.builder()
        .bucket(bucket)
        .key(prepareFileName(accountId))
        .contentType(TEXT_CSV)
        .build();
  }

  /**
   * Helper method to prepare request body.
   */
  private RequestBody prepareRequestBody(UUID accountId) throws IOException {
    try (Writer vehiclesWithCharges = csvWriter.createWriterWithCsvContent(accountId)) {
      return RequestBody.fromBytes(vehiclesWithCharges.toString().getBytes());
    }
  }

  /**
   * Helper method to prepare file name.
   */
  private String prepareFileName(UUID accountId) {
    return csvFileNameGenerator.generate(accountId);
  }

  /**
   * Logs details of the {@code exception}.
   */
  private void logAwsExceptionDetails(S3Exception exception) {
    AwsErrorDetails details = exception.awsErrorDetails();
    log.warn("Unable to upload file: error code: {}, error message: {}",
        details.errorCode(), details.errorMessage());
  }
}
