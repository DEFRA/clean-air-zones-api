package uk.gov.caz.accounts.service.registerjob;

import com.google.common.annotations.VisibleForTesting;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import uk.gov.caz.accounts.service.exception.FatalErrorWithCsvFileMetadataException;

/**
 * Tries to get 'account-user-id' metadata from file at S3.
 */
@Slf4j
@AllArgsConstructor
@Service
public class CsvFileOnS3MetadataExtractor {

  @VisibleForTesting
  public static final String ACCOUNT_USER_ID_METADATA_KEY = "account-user-id";

  private final S3Client s3Client;

  /**
   * Tries to get 'account-user-id' metadata from file on S3 bucket.
   *
   * @param s3Bucket The name of the bucket at S3 where files with vehicles data is stored.
   * @param filename The name of the key at S3 where vehicles data is stored.
   * @return {@link UUID} that contains account-user-id of the CSV file.
   * @throws FatalErrorWithCsvFileMetadataException if CSV file is unreachable, or has no
   *     metadata or metadata has invalid format.
   */
  public UUID getAccountUserId(String s3Bucket, String filename) {
    try {
      HeadObjectResponse headObjectResponse = getFileMetadata(s3Bucket, filename);
      return extractAccountUserId(headObjectResponse);
    } catch (NoSuchKeyException e) {
      String error = String.format("Exception while getting file's %s/%s metadata - bucket or "
              + "file does not exist", s3Bucket, filename);
      log.error(error);
      throw new FatalErrorWithCsvFileMetadataException(error);
    }
  }

  private HeadObjectResponse getFileMetadata(String s3Bucket, String filename) {
    HeadObjectRequest request = HeadObjectRequest.builder()
        .bucket(s3Bucket)
        .key(filename)
        .build();
    return validateMetadata(s3Client.headObject(request));
  }

  private HeadObjectResponse validateMetadata(HeadObjectResponse fileMetadata) {
    if (!fileMetadata.metadata().containsKey(ACCOUNT_USER_ID_METADATA_KEY)) {
      throw new FatalErrorWithCsvFileMetadataException(
          "The file does not contain required metadata key: " + ACCOUNT_USER_ID_METADATA_KEY);
    }
    return fileMetadata;
  }

  private UUID extractAccountUserId(HeadObjectResponse fileMetadata) {
    String unparsedAccountUserId = fileMetadata.metadata().get(ACCOUNT_USER_ID_METADATA_KEY);
    try {
      return UUID.fromString(unparsedAccountUserId);
    } catch (IllegalArgumentException e) {
      String error = "Invalid format of account-user-id: " + unparsedAccountUserId;
      log.error(error);
      throw new FatalErrorWithCsvFileMetadataException(error);
    }
  }
}