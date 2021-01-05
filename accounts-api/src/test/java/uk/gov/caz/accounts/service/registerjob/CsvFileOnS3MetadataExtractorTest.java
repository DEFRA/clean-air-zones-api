package uk.gov.caz.accounts.service.registerjob;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static uk.gov.caz.accounts.service.registerjob.CsvFileOnS3MetadataExtractor.ACCOUNT_USER_ID_METADATA_KEY;
import static uk.gov.caz.accounts.util.TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.UUID;
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
import uk.gov.caz.accounts.service.exception.FatalErrorWithCsvFileMetadataException;

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
    prepareMockResponseFromS3Client(TYPICAL_REGISTER_JOB_UPLOADER_ID.toString());

    // when
    UUID uploaderId = csvFileOnS3MetadataExtractor.getAccountUserId(S3_BUCKET, FILENAME);

    // then
    assertThat(uploaderId).isEqualByComparingTo(TYPICAL_REGISTER_JOB_UPLOADER_ID);
  }

  @Test
  public void correctLowercaseMetadataOnValidS3BucketAndFileShouldProduceProperResult() {
    // given
    prepareMockResponseFromS3Client(TYPICAL_REGISTER_JOB_UPLOADER_ID.toString().toLowerCase());

    // when
    UUID uploaderId = csvFileOnS3MetadataExtractor.getAccountUserId(S3_BUCKET, FILENAME);

    // then
    assertThat(uploaderId).isEqualByComparingTo(TYPICAL_REGISTER_JOB_UPLOADER_ID);
  }

  @Test
  public void whenUploaderIdHasInvalidUUIDSyntaxItShouldThrow() {
    // given
    prepareMockResponseFromS3Client("Invalid UUID");

    // when
    Throwable throwable = catchThrowable(
        () -> csvFileOnS3MetadataExtractor.getAccountUserId(S3_BUCKET, FILENAME));

    // then
    then(throwable)
        .isInstanceOf(FatalErrorWithCsvFileMetadataException.class)
        .hasMessage("Invalid format of account-user-id: Invalid UUID");
  }

  @Test
  public void whenThereIsNoUploaderIdMetadataItShouldThrow() {
    // given
    prepareMockResponseFromS3ClientWithoutUploaderIdMetadata();

    // when
    Throwable throwable = catchThrowable(
        () -> csvFileOnS3MetadataExtractor.getAccountUserId(S3_BUCKET, FILENAME));

    // then
    then(throwable)
        .isInstanceOf(FatalErrorWithCsvFileMetadataException.class)
        .hasMessage("The file does not contain required metadata key: " + ACCOUNT_USER_ID_METADATA_KEY);
  }

  @ParameterizedTest
  @MethodSource("s3ExceptionsProvider")
  public void exceptionsDuringMetadataFetchingAndParsingShouldProduceEmptyResult(
      Exception s3Exception) {
    // given
    prepareS3ClientToThrow(s3Exception);

    // when
    Throwable throwable = catchThrowable(
        () -> csvFileOnS3MetadataExtractor.getAccountUserId(S3_BUCKET, FILENAME));

    // then
    then(throwable)
        .isInstanceOf(FatalErrorWithCsvFileMetadataException.class)
        .hasMessage(
            "Exception while getting file's s3Bucket/filename metadata - bucket or file does not exist");
  }

  private void prepareMockResponseFromS3Client(String uploaderIdToReturn) {
    HeadObjectResponse response = HeadObjectResponse.builder()
        .metadata(ImmutableMap.of(ACCOUNT_USER_ID_METADATA_KEY, uploaderIdToReturn))
        .build();
    HeadObjectRequest request = HeadObjectRequest.builder()
        .bucket(S3_BUCKET)
        .key(FILENAME)
        .build();
    given(mockedS3Client.headObject(request)).willReturn(response);
  }

  private void prepareMockResponseFromS3ClientWithoutUploaderIdMetadata() {
    HeadObjectResponse response = HeadObjectResponse.builder()
        .metadata(Collections.emptyMap())
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