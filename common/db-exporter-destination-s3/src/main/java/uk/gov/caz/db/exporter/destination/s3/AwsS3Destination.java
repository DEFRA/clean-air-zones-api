package uk.gov.caz.db.exporter.destination.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResource;
import uk.gov.caz.db.exporter.DatabaseExportDestination;
import uk.gov.caz.db.exporter.exception.DatabaseExportException;

/**
 * AWS S3 as a destination for database export operation.
 */
@Slf4j
public class AwsS3Destination implements DatabaseExportDestination {

  /**
   * Destination S3 bucket name.
   */
  private final String s3Bucket;

  /**
   * Destination S3 object key/name.
   */
  private final String s3ObjectName;

  /**
   * Destination data mime type.
   */
  private final String mimeType;

  /**
   * V1 AWS S3 client.
   */
  private final AmazonS3 amazonS3;

  /**
   * Implementation that generates target destination URI on AWS S3.
   */
  private final AwsS3DestinationUriGenerationStrategy awsS3DestinationUriGenerationStrategy;

  /**
   * {@link OutputStream} used to write to S3.
   */
  private OutputStream outputStream;

  /**
   * Helper flag that works in assertion of calling "flush" before "getDestinationUri".
   */
  private boolean flushed;

  /**
   * Supplies {@link OutputStream}, by default by using Spring's {@link SimpleStorageResource}.
   * Usable in tests to provide own/mocked implementation.
   */
  @Setter(AccessLevel.PACKAGE)
  private Supplier<OutputStream> destinationOutputStreamSupplier;

  /**
   * Creates new instance of {@link AwsS3Destination} class.
   *
   * @param s3Bucket Destination S3 bucket name.
   * @param s3ObjectName Destination S3 object key/name.
   * @param mimeType Destination data mime type.
   * @param amazonS3 V1 AWS S3 client.
   * @param awsS3DestinationUriGenerationStrategy Implementation that generates target
   *     destination URI on AWS S3.
   */
  AwsS3Destination(@NonNull String s3Bucket, @NonNull String s3ObjectName,
      @NonNull String mimeType, @NonNull AmazonS3 amazonS3,
      @NonNull AwsS3DestinationUriGenerationStrategy awsS3DestinationUriGenerationStrategy) {
    this.s3Bucket = s3Bucket;
    this.s3ObjectName = s3ObjectName;
    this.mimeType = mimeType;
    this.amazonS3 = amazonS3;
    this.awsS3DestinationUriGenerationStrategy = awsS3DestinationUriGenerationStrategy;
    destinationOutputStreamSupplier = new SimpleStorageResourceOutputStreamProvider(s3Bucket,
        s3ObjectName, amazonS3);
  }

  @Override
  public OutputStream outputStream() {
    try {
      outputStream = destinationOutputStreamSupplier.get();
      return outputStream;
    } catch (Exception exception) {
      throw throwDatabaseExportException(exception);
    }
  }

  @Override
  public void flush() {
    throwIfOutputStreamNotUsed();
    try {
      log.info("Starting upload {} object to S3", s3ObjectName);
      flushAndClose();
      flushed = true;
      setObjectMimeType();
    } catch (Exception exception) {
      if (flushed) {
        tryToDeleteObject();
      }
      throw throwDatabaseExportException(exception);
    }
  }


  @Override
  public void close() {
    // Not needed because already done in "flush"
  }

  @Override
  public URI getDestinationUri() {
    throwIfOutputStreamNotUsed();
    throwIfNotFlushed();
    try {
      return awsS3DestinationUriGenerationStrategy.getUri(s3Bucket, s3ObjectName, amazonS3);
    } catch (Exception exception) {
      throw throwDatabaseExportException(exception);
    }
  }

  /**
   * Flushes and closes OutputStream provided from "outputStream" method.
   */
  private void flushAndClose() throws IOException {
    outputStream.flush();
    outputStream.close();
  }

  /**
   * Sets mime type on AWS S3 object. Note that this operation cannot be done at object contents
   * upload time and must be done by copying object to itself with new metadata.
   */
  private void setObjectMimeType() {
    ObjectMetadata metadataCopy = new ObjectMetadata();
    metadataCopy.setContentType(mimeType);

    CopyObjectRequest request = new CopyObjectRequest(s3Bucket, s3ObjectName, s3Bucket,
        s3ObjectName)
        .withNewObjectMetadata(metadataCopy);

    amazonS3.copyObject(request);
  }

  /**
   * When object has been successfully uploaded but some other operation failed object should be
   * deleted to keep transactional state.
   */
  private void tryToDeleteObject() {
    try {
      amazonS3.deleteObject(s3Bucket, s3ObjectName);
    } catch (Exception exception) {
      log.warn(
          "Unable to delete S3 object {} when cleaning up after "
              + "unsuccessful setObjectMimeType operation", s3ObjectName);
    }
  }

  /**
   * Throws {@link IllegalStateException} when 'outputStream' method was not called before doing any
   * operation on this {@link AwsS3Destination}.
   */
  private void throwIfOutputStreamNotUsed() {
    if (outputStream == null) {
      throw new IllegalStateException(
          "'outputStream' method must be called before 'flush', 'close' or 'getDestinationUri'");
    }
  }

  /**
   * Throws {@link IllegalStateException} when 'flush' method was not called before trying to get
   * destination URI from this {@link AwsS3Destination}.
   */
  private void throwIfNotFlushed() {
    if (!flushed) {
      throw new IllegalStateException("AwsS3Destination: call 'flush' before 'getDestinationUrl'");
    }
  }

  /**
   * Throws {@link DatabaseExportException} wrapping root cause exception.
   */
  private DatabaseExportException throwDatabaseExportException(Exception exception) {
    return new DatabaseExportException("Unable to export database data to AWS S3", exception);
  }
}
