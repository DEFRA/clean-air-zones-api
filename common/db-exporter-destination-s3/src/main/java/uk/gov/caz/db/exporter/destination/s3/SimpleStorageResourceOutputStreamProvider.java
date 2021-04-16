package uk.gov.caz.db.exporter.destination.s3;

import com.amazonaws.services.s3.AmazonS3;
import java.io.OutputStream;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResource;
import org.springframework.core.task.SyncTaskExecutor;

/**
 * Provides default Spring's {@link SimpleStorageResource} implementation. {@link AwsS3Destination}
 * uses it by default until changed by a setter.
 */
@RequiredArgsConstructor
class SimpleStorageResourceOutputStreamProvider implements Supplier<OutputStream> {

  /**
   * S3 destination bucket.
   */
  @NonNull
  private final String s3Bucket;

  /**
   * S3 destination object name/key.
   */
  @NonNull
  private final String s3ObjectName;

  /**
   * V1 Amazon S3 client.
   */
  @NonNull
  private final AmazonS3 amazonS3;

  @SneakyThrows
  @Override
  public OutputStream get() {
    SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3,
        s3Bucket,
        s3ObjectName,
        new SyncTaskExecutor());
    return simpleStorageResource.getOutputStream();
  }
}
