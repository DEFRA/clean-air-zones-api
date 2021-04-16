package uk.gov.caz.db.exporter.destination.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.gov.caz.db.exporter.exception.DatabaseExportException;

public class AwsS3DestinationTest {

  private static final String S3_BUCKET = "Bucket";
  private static final String S3_OBJECT = "Object";
  private static final String MIME_TYPE = "text/csv";
  private AmazonS3 amazonS3;
  private AwsS3Destination awsS3Destination;
  private AwsS3DestinationUriGenerationStrategy uriGenerationStrategy;
  private OutputStream outputStream;

  @BeforeEach
  public void setup() {
    amazonS3 = mock(AmazonS3.class);
    uriGenerationStrategy = mock(AwsS3DestinationUriGenerationStrategy.class);
    awsS3Destination = new AwsS3Destination(S3_BUCKET, S3_OBJECT, MIME_TYPE,
        amazonS3, uriGenerationStrategy);
  }

  @Nested
  public class ConstructingNewInstance {

    @Test
    public void validatesNullInputForS3BucketParameter() {
      check("s3Bucket",
          () -> new AwsS3Destination(null, S3_OBJECT, MIME_TYPE, amazonS3, uriGenerationStrategy));
    }

    @Test
    public void validatesNullInputForS3ObjectParameter() {
      check("s3Object",
          () -> new AwsS3Destination(S3_BUCKET, null, MIME_TYPE, amazonS3, uriGenerationStrategy));
    }

    @Test
    public void validatesNullInputForMimeTypeParameter() {
      check("mimeType",
          () -> new AwsS3Destination(S3_BUCKET, S3_OBJECT, null, amazonS3, uriGenerationStrategy));
    }

    @Test
    public void validatesNullInputForAmazonS3Parameter() {
      check("amazonS3",
          () -> new AwsS3Destination(S3_BUCKET, S3_OBJECT, MIME_TYPE, null, uriGenerationStrategy));
    }

    @Test
    public void validatesNullInputForAwsS3DestinationUriGenerationStrategy() {
      check("awsS3DestinationUriGenerationStrategy",
          () -> new AwsS3Destination(S3_BUCKET, S3_OBJECT, MIME_TYPE, amazonS3, null));
    }

    private void check(String parameterName, Runnable creator) {
      try {
        creator.run();
      } catch (NullPointerException npe) {
        return;
      }
      fail("Should check if '" + parameterName + "' parameter is not null");
    }
  }

  @Nested
  public class GettingOutputStream {

    @Test
    public void ofDefaultSpringSimpleStorageResource() {
      // given

      // when
      OutputStream outputStream = awsS3Destination.outputStream();

      // then
      assertThat(outputStream).isNotNull();
    }

    @Test
    public void whenItThrowsAnException() {
      // given
      Supplier<OutputStream> outputStreamSupplier = () -> {
        throw new RuntimeException("some error");
      };
      awsS3Destination.setDestinationOutputStreamSupplier(outputStreamSupplier);

      // when
      Throwable throwable = catchThrowable(() -> awsS3Destination.outputStream());

      // then
      assertThat(throwable).isInstanceOf(DatabaseExportException.class)
          .hasMessage("Unable to export database data to AWS S3");
    }
  }

  @Nested
  public class Flushing {

    @Test
    public void delegatesToOutputStreamAndAssignsMetadata() throws IOException {
      // given
      prepOutputStreamMock();

      // when
      awsS3Destination.outputStream();
      awsS3Destination.flush();

      // then
      verify(outputStream).flush();
      verify(outputStream).close();
      verifyThatMimeTypeHasBeenSet();
    }

    @Test
    public void throwsIfOutputStreamHasNotBeenUsed() {
      // given

      // when
      Throwable throwable = catchThrowable(() -> awsS3Destination.flush());

      // then
      assertThat(throwable).isInstanceOf(IllegalStateException.class).hasMessage(
          "'outputStream' method must be called before 'flush', 'close' or 'getDestinationUri'");
    }

    @Test
    public void throwsDatabaseExportExceptionOnAnyErrorFromAws() throws IOException {
      // given
      prepOutputStreamMock();
      doThrow(new IOException()).when(outputStream).flush();

      // when
      awsS3Destination.outputStream();
      Throwable throwable = catchThrowable(() -> awsS3Destination.flush());

      // then
      assertThat(throwable).isInstanceOf(DatabaseExportException.class).hasMessage(
          "Unable to export database data to AWS S3");
    }

    @Nested
    public class AndSettingMimeType {

      @Test
      public void triesToDeleteObjectAndThrowsDatabaseExportExceptionOnErrorWhenSettingMimeType() {
        // given
        prepOutputStreamMock();
        when(amazonS3.copyObject(any(CopyObjectRequest.class)))
            .thenThrow(new RuntimeException("error when setting mime type"));

        // when
        awsS3Destination.outputStream();
        Throwable throwable = catchThrowable(() -> awsS3Destination.flush());

        // then
        assertThat(throwable).isInstanceOf(DatabaseExportException.class).hasMessage(
            "Unable to export database data to AWS S3");
        verify(amazonS3).deleteObject(S3_BUCKET, S3_OBJECT);
      }

      @Test
      public void onlyLogsWarningWhenDeletingObjectFailsOnErrorWhenSettingMimeType() {
        // given
        prepOutputStreamMock();
        when(amazonS3.copyObject(any(CopyObjectRequest.class)))
            .thenThrow(new RuntimeException("error when setting mime type"));
        doThrow(new RuntimeException("error when deleting object")).when(amazonS3)
            .deleteObject(S3_BUCKET, S3_OBJECT);

        // when
        awsS3Destination.outputStream();
        Throwable throwable = catchThrowable(() -> awsS3Destination.flush());

        // then
        assertThat(throwable).isInstanceOf(DatabaseExportException.class).hasMessage(
            "Unable to export database data to AWS S3");
      }
    }

    private ArgumentCaptor<CopyObjectRequest> getCopyObjectRequestArgumentCaptor() {
      return ArgumentCaptor
          .forClass(CopyObjectRequest.class);
    }

    private void verifyThatMimeTypeHasBeenSet() {
      ArgumentCaptor<CopyObjectRequest> copyObjectRequestArgumentCaptor = getCopyObjectRequestArgumentCaptor();
      verify(amazonS3).copyObject(copyObjectRequestArgumentCaptor.capture());
      assertThat(copyObjectRequestArgumentCaptor.getValue()).isNotNull();
      assertThat(copyObjectRequestArgumentCaptor.getValue().getNewObjectMetadata().getContentType())
          .isEqualTo("text/csv");
    }
  }

  @Nested
  public class GettingDestinationUri {

    public static final String SOME_URL = "http://some.url";

    @Test
    public void generatesPresignedUrlToS3() {
      // given
      prepOutputStreamMock();
      when(uriGenerationStrategy.getUri(S3_BUCKET, S3_OBJECT, amazonS3))
          .thenReturn(URI.create(SOME_URL));

      // when
      awsS3Destination.outputStream();
      awsS3Destination.flush();
      URI destinationUri = awsS3Destination.getDestinationUri();

      // then
      assertThat(destinationUri).isNotNull();
      assertThat(destinationUri.toString()).isEqualTo(SOME_URL);
    }

    @Test
    public void throwsIfNotFlushed() {
      // given
      prepOutputStreamMock();

      // when
      awsS3Destination.outputStream();
      Throwable throwable = catchThrowable(() -> awsS3Destination.getDestinationUri());

      // then
      assertThat(throwable).isInstanceOf(IllegalStateException.class)
          .hasMessage("AwsS3Destination: call 'flush' before 'getDestinationUrl'");
    }

    @Test
    public void throwsDatabaseExportExceptionOnAnyErrorFromAws() {
      // given
      prepOutputStreamMock();
      when(uriGenerationStrategy.getUri(S3_BUCKET, S3_OBJECT, amazonS3))
          .thenThrow(new RuntimeException("some error"));

      // when
      awsS3Destination.outputStream();
      awsS3Destination.flush();
      Throwable throwable = catchThrowable(() -> awsS3Destination.getDestinationUri());

      // then
      assertThat(throwable).isInstanceOf(DatabaseExportException.class)
          .hasMessage("Unable to export database data to AWS S3");
    }
  }

  @Nested
  public class Closing {

    @Test
    public void doesNothing() {
      awsS3Destination.close();
    }
  }

  private void prepOutputStreamMock() {
    outputStream = mock(OutputStream.class);
    awsS3Destination.setDestinationOutputStreamSupplier(() -> outputStream);
  }
}
