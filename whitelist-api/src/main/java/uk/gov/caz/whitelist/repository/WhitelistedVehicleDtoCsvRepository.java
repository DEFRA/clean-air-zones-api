package uk.gov.caz.whitelist.repository;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import uk.gov.caz.whitelist.model.CsvFindResult;
import uk.gov.caz.whitelist.model.CsvParseResult;
import uk.gov.caz.whitelist.service.CsvFileOnS3MetadataExtractor;
import uk.gov.caz.whitelist.service.CsvFileOnS3MetadataExtractor.CsvMetadata;
import uk.gov.caz.whitelist.service.CsvObjectMapper;
import uk.gov.caz.whitelist.service.exception.S3MetadataException;

/**
 * A class that is responsible for managing vehicle data located at S3.
 */
@Repository
@Slf4j
public class WhitelistedVehicleDtoCsvRepository {

  public static final String UPLOADER_ID_METADATA_KEY = "uploader-id";

  private final S3Client s3Client;
  private final CsvObjectMapper csvObjectMapper;
  private final CsvFileOnS3MetadataExtractor metadataExtractor;

  /**
   * Creates an instance of {@link WhitelistedVehicleDtoCsvRepository}.
   *
   * @param s3Client A client for AWS S3
   * @param csvObjectMapper An instance of {@link CsvObjectMapper}
   * @param metadataExtractor An instance of {@link CsvFileOnS3MetadataExtractor}
   */
  public WhitelistedVehicleDtoCsvRepository(S3Client s3Client,
      CsvObjectMapper csvObjectMapper,
      CsvFileOnS3MetadataExtractor metadataExtractor) {
    this.s3Client = s3Client;
    this.csvObjectMapper = csvObjectMapper;
    this.metadataExtractor = metadataExtractor;
  }

  /**
   * Reads the content of a UTF-8-encoded file located at S3 and maps it to a set of {@link
   * CsvFindResult}. The set is returned alongside the {@code uploaderId} which represents an entity
   * which uploaded the file.
   *
   * @param bucket The name of a S3 bucket
   * @param filename The name (key) of a file within a given bucket
   * @return {@link CsvParseResult} value object which contains the set with parsed vehicles data
   *     and the uploader id
   * @throws NullPointerException if {@code bucket} or {@code filename} is null or empty
   * @throws RuntimeException with {@link IOException} as a cause when {@link IOException}
   *     occurs
   * @throws NoSuchKeyException when the file's does not exist at S3
   * @throws S3MetadataException when the file's content type (set as a metadata) is {@code
   *     null} or not equal to 'text/csv
   */
  public CsvFindResult findAll(String bucket, String filename) {
    Preconditions
        .checkArgument(!Strings.isNullOrEmpty(bucket), "Bucket %s cannot be null or empty");
    Preconditions
        .checkArgument(!Strings.isNullOrEmpty(filename), "Filename %s cannot be null or empty");

    CsvMetadata requiredMetadata = metadataExtractor.getRequiredMetadata(bucket, filename);

    UUID uploaderId = requiredMetadata.getUploaderId();
    String email = requiredMetadata.getEmail();
    try (InputStream inputStream = getS3FileInputStream(bucket, filename)) {
      CsvParseResult result = csvObjectMapper.read(inputStream);
      return new CsvFindResult(email, uploaderId, result.getWhitelistedVehicles(),
          result.getValidationErrors());
    } catch (IOException e) {
      log.error("IOException while reading file {}/{}", bucket, filename);
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Deletes file from S3 and reports status to the caller.
   */
  public boolean purgeFile(String bucket, String filename) {
    try {
      log.info("Deleting file {}/{} from S3", bucket, filename);

      DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
          .bucket(bucket)
          .key(filename)
          .build();
      s3Client.deleteObject(deleteObjectRequest);

      return true;
    } catch (Exception e) {
      log.warn("Cannot delete file from S3", e);
      return false;
    }
  }

  private InputStream getS3FileInputStream(String bucket, String filename) {
    try {
      GetObjectRequest getObjectRequest = GetObjectRequest.builder()
          .bucket(bucket)
          .key(filename)
          .build();
      return s3Client.getObjectAsBytes(getObjectRequest).asInputStream();
    } catch (NoSuchKeyException | NoSuchBucketException e) {
      log.error("Exception while getting file {}/{} - bucket/file does not exist", bucket,
          filename);
      throw e;
    }
  }
}
