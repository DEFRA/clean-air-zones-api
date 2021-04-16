package uk.gov.caz.whitelist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_EMAIL;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID;
import static uk.gov.caz.whitelist.service.CsvFileOnS3MetadataExtractor.CSV_CONTENT_TYPE_METADATA_KEY;
import static uk.gov.caz.whitelist.service.CsvFileOnS3MetadataExtractor.UPLOADER_EMAIL_METADATA_KEY;
import static uk.gov.caz.whitelist.service.CsvFileOnS3MetadataExtractor.UPLOADER_ID_METADATA_KEY;

import com.google.common.collect.ImmutableMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import uk.gov.caz.whitelist.model.CsvContentType;
import uk.gov.caz.whitelist.service.CsvFileOnS3MetadataExtractor.CsvMetadata;
import uk.gov.caz.whitelist.service.exception.FatalErrorWithCsvFileMetadataException;
import uk.gov.caz.whitelist.service.exception.S3MaxFileSizeExceededException;

@ExtendWith(MockitoExtension.class)
class CsvFileOnS3MetadataExtractorTest {

  private static final String S3_BUCKET = "s3Bucket";
  private static final String FILENAME = "filename";

  @Mock
  private S3Client mockedS3Client;

  @InjectMocks
  private CsvFileOnS3MetadataExtractor csvFileOnS3MetadataExtractor;

  @Test
  public void correctMetadataOnValidS3BucketAndFileShouldProduceProperResult() {
    // given
    prepareMockResponseFromS3Client(TYPICAL_REGISTER_JOB_UPLOADER_ID.toString(),
        CsvContentType.WHITELIST_LIST.name(),
        CsvFileOnS3MetadataExtractor.MAX_FILE_SIZE_IN_BYTES - 1);

    // when
    CsvMetadata csvMetadata = csvFileOnS3MetadataExtractor.getRequiredMetadata(S3_BUCKET, FILENAME);

    // then
    assertThat(csvMetadata).isNotNull();
    assertThat(csvMetadata.getUploaderId()).isEqualByComparingTo(TYPICAL_REGISTER_JOB_UPLOADER_ID);
    assertThat(csvMetadata.getEmail()).isEqualTo(TYPICAL_EMAIL);
  }

  @Test
  public void correctLowercaseMetadataOnValidS3BucketAndFileShouldProduceProperResult() {
    // given
    prepareMockResponseFromS3Client(TYPICAL_REGISTER_JOB_UPLOADER_ID.toString().toLowerCase(),
        CsvContentType.WHITELIST_LIST.name().toLowerCase(),
        CsvFileOnS3MetadataExtractor.MAX_FILE_SIZE_IN_BYTES - 1);

    // when
    CsvMetadata csvMetadata = csvFileOnS3MetadataExtractor.getRequiredMetadata(S3_BUCKET, FILENAME);

    // then
    assertThat(csvMetadata).isNotNull();
    assertThat(csvMetadata.getUploaderId()).isEqualByComparingTo(TYPICAL_REGISTER_JOB_UPLOADER_ID);
    assertThat(csvMetadata.getEmail()).isEqualTo(TYPICAL_EMAIL);
  }

  @Test
  public void returnEmptyEmailIfNotExistsInMetadata() {
    // given
    prepareMockResponseFromS3ClientWithoutEmailMetadata();

    // when
    CsvMetadata csvMetadata = csvFileOnS3MetadataExtractor.getRequiredMetadata(S3_BUCKET, FILENAME);

    // then
    assertThat(csvMetadata).isNotNull();
    assertThat(csvMetadata.getEmail()).isNull();
  }

  @Test
  public void whenUploaderIdHasInvalidUUIDSyntaxItShouldThrow() {
    // given
    prepareMockResponseFromS3Client("Invalid UUID", CsvContentType.WHITELIST_LIST.name(),
        CsvFileOnS3MetadataExtractor.MAX_FILE_SIZE_IN_BYTES - 1);

    // when
    Throwable throwable = catchThrowable(
        () -> csvFileOnS3MetadataExtractor.getRequiredMetadata(S3_BUCKET, FILENAME));

    // then
    then(throwable)
        .isInstanceOf(FatalErrorWithCsvFileMetadataException.class)
        .hasMessage("Invalid format of uploader-id: Invalid UUID");
  }

  @Test
  public void whenThereIsNoUploaderIdMetadataItShouldThrow() {
    // given
    prepareMockResponseFromS3ClientWithoutUploaderIdMetadata();

    // when
    Throwable throwable = catchThrowable(
        () -> csvFileOnS3MetadataExtractor.getRequiredMetadata(S3_BUCKET, FILENAME));

    // then
    then(throwable)
        .isInstanceOf(FatalErrorWithCsvFileMetadataException.class)
        .hasMessage("The file does not contain required metadata key: uploader-id");
  }

  @Test
  public void shouldThrowS3MaxFileSizeExceededExceptionWhenFileIsTooBig() {
    // given
    prepareMockResponseFromS3Client(TYPICAL_REGISTER_JOB_UPLOADER_ID.toString().toLowerCase(),
        CsvContentType.WHITELIST_LIST.name(),
        CsvFileOnS3MetadataExtractor.MAX_FILE_SIZE_IN_BYTES + 1);

    // when
    Throwable throwable = catchThrowable(
        () -> csvFileOnS3MetadataExtractor.getRequiredMetadata(S3_BUCKET, FILENAME));

    // then
    assertThat(throwable).isInstanceOf(S3MaxFileSizeExceededException.class);
  }

  @ParameterizedTest
  @MethodSource("s3ExceptionsProvider")
  public void exceptionsDuringMetadataFetchingAndParsingShouldProduceEmptyResult(
      Exception s3Exception) {
    // given
    prepareS3ClientToThrow(s3Exception);

    // when
    Throwable throwable = catchThrowable(
        () -> csvFileOnS3MetadataExtractor.getRequiredMetadata(S3_BUCKET, FILENAME));

    // then
    then(throwable)
        .isInstanceOf(FatalErrorWithCsvFileMetadataException.class)
        .hasMessage(
            "Exception while getting file's s3Bucket/filename metadata - bucket or file does not exist");
  }

  private void prepareMockResponseFromS3Client(String uploaderIdToReturn,
      String csvContentTypeToReturn, long contentLength) {
    HeadObjectResponse response = HeadObjectResponse.builder()
        .metadata(
            ImmutableMap
                .of(UPLOADER_ID_METADATA_KEY, uploaderIdToReturn,
                    CSV_CONTENT_TYPE_METADATA_KEY, csvContentTypeToReturn,
                    UPLOADER_EMAIL_METADATA_KEY, TYPICAL_EMAIL))
        .contentLength(contentLength)
        .build();
    HeadObjectRequest request = HeadObjectRequest.builder()
        .bucket(S3_BUCKET)
        .key(FILENAME)
        .build();
    given(mockedS3Client.headObject(request)).willReturn(response);
  }

  private void prepareMockResponseFromS3ClientWithoutUploaderIdMetadata() {
    HeadObjectResponse response = HeadObjectResponse.builder()
        .metadata(
            ImmutableMap
                .of(CSV_CONTENT_TYPE_METADATA_KEY, CsvContentType.WHITELIST_LIST.name()))
        .build();
    HeadObjectRequest request = HeadObjectRequest.builder()
        .bucket(S3_BUCKET)
        .key(FILENAME)
        .build();
    given(mockedS3Client.headObject(request)).willReturn(response);
  }

  private void prepareMockResponseFromS3ClientWithoutEmailMetadata() {
    HeadObjectResponse response = HeadObjectResponse.builder()
        .metadata(
            ImmutableMap
                .of(UPLOADER_ID_METADATA_KEY, TYPICAL_REGISTER_JOB_UPLOADER_ID.toString(),
                    CSV_CONTENT_TYPE_METADATA_KEY, CsvContentType.WHITELIST_LIST.name()))
        .build();
    HeadObjectRequest request = HeadObjectRequest.builder()
        .bucket(S3_BUCKET)
        .key(FILENAME)
        .build();
    given(mockedS3Client.headObject(request)).willReturn(response);
  }

  private void prepareS3ClientToThrow(Exception s3Exception) {
    HeadObjectRequest request = HeadObjectRequest.builder()
        .bucket(S3_BUCKET)
        .key(FILENAME)
        .build();
    given(mockedS3Client.headObject(request)).willThrow(s3Exception);
  }

  static Stream<Exception> s3ExceptionsProvider() {
    return Stream.of(
        NoSuchKeyException.builder().build()   // No S3 Bucket or file
    );
  }
}