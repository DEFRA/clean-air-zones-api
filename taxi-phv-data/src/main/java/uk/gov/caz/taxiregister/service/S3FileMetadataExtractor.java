package uk.gov.caz.taxiregister.service;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import uk.gov.caz.taxiregister.service.exception.S3MetadataException;

/**
 * Tries to get metadata from file on S3.
 */
@Slf4j
@Service
public class S3FileMetadataExtractor {

  @VisibleForTesting
  public static final String UPLOADER_ID_METADATA_KEY = "uploader-id";

  @VisibleForTesting
  public static final String UPLOADER_EMAIL_METADATA_KEY = "email";

  private final S3Client s3Client;

  /**
   * Creates an instance of {@link S3FileMetadataExtractor}.
   *
   * @param s3Client A client for AWS S3
   */
  public S3FileMetadataExtractor(S3Client s3Client) {
    this.s3Client = s3Client;
  }

  /**
   * Tries to get 'uploader-id' metadata from file on S3 bucket.
   *
   * @param s3Bucket The name of the bucket at S3 where files with vehicles data is stored.
   * @param filename The name of the key at S3 where vehicles data is stored.
   * @return {@link Optional} with {@link UUID} that contain uploader id.
   */
  public Optional<String> getUploaderEmail(String s3Bucket, String filename) {
    return getMetadataValue(s3Bucket, filename, UPLOADER_EMAIL_METADATA_KEY);
  }

  /**
   * Tries to get 'uploader-id' metadata from file on S3 bucket.
   *
   * @param s3Bucket The name of the bucket at S3 where files with vehicles data is stored.
   * @param filename The name of the key at S3 where vehicles data is stored.
   * @return {@link Optional} with {@link UUID} that contain uploader id.
   */
  public Optional<UUID> getUploaderId(String s3Bucket, String filename) {
    return getMetadataValue(s3Bucket, filename, UPLOADER_ID_METADATA_KEY)
        .flatMap(tryParseUuid());
  }

  /**
   * An internal method that fetches metadata value for given key.
   */
  private Optional<String> getMetadataValue(String s3Bucket, String filename, String key) {
    try {
      HeadObjectResponse headObjectResponse = getFileMetadata(s3Bucket, filename, key);
      return Optional.ofNullable(headObjectResponse.metadata().get(key));
    } catch (NoSuchKeyException e) {
      log.error("Exception while getting file's {}/{} metadata - bucket or file does not exist",
          s3Bucket,
          filename);
    } catch (S3MetadataException e) {
      log.error("No required metadata", e);
    }
    return Optional.empty();
  }

  /**
   * Gets file metadata from S3.
   *
   * @throws NoSuchKeyException if the file is not located at S3.
   */
  private HeadObjectResponse getFileMetadata(String s3Bucket, String filename, String key) {
    HeadObjectRequest request = HeadObjectRequest.builder()
        .bucket(s3Bucket)
        .key(filename)
        .build();
    return validateMetadata(s3Client.headObject(request), key);
  }

  private HeadObjectResponse validateMetadata(HeadObjectResponse fileMetadata, String key) {
    if (!fileMetadata.metadata().containsKey(key)) {
      throw new S3MetadataException(
          "The file does not contain required metadata key: " + key);
    }
    return fileMetadata;
  }

  /**
   * Function that tries to parse UUID and hides all exceptions under Optional.empty()
   */
  @NotNull
  private Function<String, Optional<UUID>> tryParseUuid() {
    return value -> {
      try {
        return Optional.of(UUID.fromString(value));
      } catch (IllegalArgumentException e) {
        return Optional.empty();
      }
    };
  }
}
