package uk.gov.caz.whitelist.service;

import com.google.common.annotations.VisibleForTesting;
import java.util.UUID;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import uk.gov.caz.whitelist.service.exception.FatalErrorWithCsvFileMetadataException;
import uk.gov.caz.whitelist.service.exception.S3MaxFileSizeExceededException;

/**
 * Tries to get 'uploader-id' and 'csv-content-type' metadata from file on S3.
 */
@Slf4j
@Service
public class CsvFileOnS3MetadataExtractor {

  public static final long MAX_FILE_SIZE_IN_BYTES = 100L * 1024 * 1024; // 100 MB

  @VisibleForTesting
  static final String UPLOADER_ID_METADATA_KEY = "uploader-id";

  @VisibleForTesting
  public static final String CSV_CONTENT_TYPE_METADATA_KEY = "csv-content-type";

  @VisibleForTesting
  public static final String UPLOADER_EMAIL_METADATA_KEY = "email";

  private final S3Client s3Client;

  /**
   * Creates an instance of {@link CsvFileOnS3MetadataExtractor}.
   *
   * @param s3Client A client for AWS S3
   */
  public CsvFileOnS3MetadataExtractor(S3Client s3Client) {
    this.s3Client = s3Client;
  }

  @Value
  public static class CsvMetadata {

    UUID uploaderId;

    String email;
  }

  /**
   * Tries to get 'uploader-id' and 'csv-content-type' metadata from file on S3 bucket.
   *
   * @param s3Bucket The name of the bucket at S3 where files with vehicles data is stored.
   * @param filename The name of the key at S3 where vehicles data is stored.
   * @return {@link CsvMetadata} with {@link UUID} that contains uploader id and email.
   * @throws FatalErrorWithCsvFileMetadataException if CSV file is unreachable, or has no
   *     metadata or metadata has invalid format.
   */
  public CsvMetadata getRequiredMetadata(String s3Bucket, String filename) {
    try {
      HeadObjectResponse headObjectResponse = getFileMetadata(s3Bucket, filename);
      return new CsvMetadata(getUploaderId(headObjectResponse),
          getUploaderEmail(headObjectResponse));
    } catch (NoSuchKeyException e) {
      String error = String
          .format("Exception while getting file's %s/%s metadata - bucket or file does not exist",
              s3Bucket, filename);
      log.error(error);
      throw new FatalErrorWithCsvFileMetadataException(error);
    }
  }

  private HeadObjectResponse getFileMetadata(String s3Bucket, String filename) {
    HeadObjectRequest request = HeadObjectRequest.builder()
        .bucket(s3Bucket)
        .key(filename)
        .build();
    HeadObjectResponse headObjectResponse = validateMetadata(s3Client.headObject(request));
    checkMaxFileSizePrecondition(headObjectResponse);
    return headObjectResponse;
  }

  private HeadObjectResponse validateMetadata(HeadObjectResponse fileMetadata) {
    if (!fileMetadata.metadata().containsKey(UPLOADER_ID_METADATA_KEY)) {
      throw new FatalErrorWithCsvFileMetadataException(
          "The file does not contain required metadata key: " + UPLOADER_ID_METADATA_KEY);
    }
    return fileMetadata;
  }

  private void checkMaxFileSizePrecondition(HeadObjectResponse fileMetadata) {
    Long fileSizeInBytes = fileMetadata.contentLength();
    if (fileSizeInBytes != null
        && fileSizeInBytes > CsvFileOnS3MetadataExtractor.MAX_FILE_SIZE_IN_BYTES) {
      throw new S3MaxFileSizeExceededException();
    }
  }

  private UUID getUploaderId(HeadObjectResponse fileMetadata) {
    String unparsedUploaderId = fileMetadata.metadata().get(UPLOADER_ID_METADATA_KEY);
    try {
      return UUID.fromString(unparsedUploaderId);
    } catch (IllegalArgumentException e) {
      String error = "Invalid format of uploader-id: " + unparsedUploaderId;
      log.error(error);
      throw new FatalErrorWithCsvFileMetadataException(error);
    }
  }

  private String getUploaderEmail(HeadObjectResponse fileMetadata) {
    return fileMetadata.metadata().get(UPLOADER_EMAIL_METADATA_KEY);
  }
}
