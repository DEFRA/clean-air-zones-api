package uk.gov.caz.taxiregister.service;

import static com.google.common.collect.ImmutableMap.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static uk.gov.caz.taxiregister.service.S3FileMetadataExtractor.UPLOADER_EMAIL_METADATA_KEY;
import static uk.gov.caz.taxiregister.service.S3FileMetadataExtractor.UPLOADER_ID_METADATA_KEY;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
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
import uk.gov.caz.taxiregister.service.exception.S3MetadataException;

@ExtendWith(MockitoExtension.class)
class S3FileMetadataExtractorTest {

  private static final String S3_BUCKET = "s3Bucket";
  private static final String FILENAME = "filename";

  @Mock
  private S3Client mockedS3Client;

  @InjectMocks
  private S3FileMetadataExtractor s3FileMetadataExtractor;

  @Test
  public void correctUploaderIdOnValidS3BucketAndFileShouldProduceProperResult() {
    // given
    prepareMockResponseFromS3Client(of(UPLOADER_ID_METADATA_KEY, TYPICAL_REGISTER_JOB_UPLOADER_ID.toString()));

    // when
    Optional<UUID> optionalUploaderId = s3FileMetadataExtractor
        .getUploaderId(S3_BUCKET, FILENAME);

    // then
    assertThat(optionalUploaderId).isPresent();
    assertThat(optionalUploaderId.get()).isEqualByComparingTo(TYPICAL_REGISTER_JOB_UPLOADER_ID);
  }

  @Test
  public void whenUploaderIdHasInvalidUUIDSyntaxItShouldProduceEmptyResult() {
    // given
    prepareMockResponseFromS3Client(of(UPLOADER_ID_METADATA_KEY, "Invalid UUID"));

    // when
    Optional<UUID> optionalUploaderId = s3FileMetadataExtractor
        .getUploaderId(S3_BUCKET, FILENAME);

    // then
    assertThat(optionalUploaderId).isEmpty();
  }

  @Test
  public void whenThereIsNoUploaderItMetadataItShouldProduceEmptyResult() {
    // given
    prepareMockResponseFromS3ClientWithoutAnyMetadata();

    // when
    Optional<UUID> optionalUploaderId = s3FileMetadataExtractor
        .getUploaderId(S3_BUCKET, FILENAME);

    // then
    assertThat(optionalUploaderId).isEmpty();
  }

  @Test
  public void whenThereIsNoEmailMetadataItShouldProduceEmptyResult() {
    // given
    prepareMockResponseFromS3ClientWithoutAnyMetadata();

    // when
    Optional<String> email = s3FileMetadataExtractor
        .getUploaderEmail(S3_BUCKET, FILENAME);

    // then
    assertThat(email).isEmpty();
  }

  @Test
  public void shouldReturnProperEmailIfEmailMetadataExists() {
    // given
    String givenEmail = "ntr@testest.com";
    prepareMockResponseFromS3Client(of(UPLOADER_EMAIL_METADATA_KEY, givenEmail));

    // when
    Optional<String> email = s3FileMetadataExtractor
        .getUploaderEmail(S3_BUCKET, FILENAME);

    // then
    assertThat(email).isPresent();
    assertThat(email.get()).isEqualTo(givenEmail);
  }

  @ParameterizedTest
  @MethodSource("s3ExceptionsProvider")
  public void exceptionsDuringMetadataFetchingAndParsingShouldProduceEmptyResult(
      Exception s3Exception) {
    // given
    prepareS3ClientToThrow(s3Exception);

    // when
    Optional<UUID> optionalUploaderId = s3FileMetadataExtractor
        .getUploaderId(S3_BUCKET, FILENAME);

    // then
    assertThat(optionalUploaderId).isEmpty();
  }

  private void prepareMockResponseFromS3Client(Map<String, String> metadata) {
    HeadObjectResponse response = HeadObjectResponse.builder()
        .metadata(metadata)
        .build();
    HeadObjectRequest request = HeadObjectRequest.builder()
        .bucket(S3_BUCKET)
        .key(FILENAME)
        .build();
    given(mockedS3Client.headObject(request)).willReturn(response);
  }

  private void prepareMockResponseFromS3ClientWithoutAnyMetadata() {
    HeadObjectResponse response = HeadObjectResponse.builder()
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
        NoSuchKeyException.builder().build(),   // No S3 Bucket or file
        new S3MetadataException("No metadata")  // No "uploader-id" metadata on file
    );
  }
}