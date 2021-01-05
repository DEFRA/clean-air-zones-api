package uk.gov.caz.accounts.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import uk.gov.caz.accounts.dto.AccountVehicleDto;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.service.CsvAccountVehicleObjectMapper;
import uk.gov.caz.accounts.service.exception.S3InvalidUploaderIdFormatException;
import uk.gov.caz.accounts.service.exception.S3MaxFileSizeExceededException;
import uk.gov.caz.accounts.service.exception.S3MetadataException;
import uk.gov.caz.csv.model.CsvParseResult;

@ExtendWith(MockitoExtension.class)
class AccountVehicleDtoCsvRepositoryTest {

  private static final GetObjectResponse ANY_RESPONSE = GetObjectResponse.builder().build();
  public static final UUID ACCOUNT_USER_ID = UUID.randomUUID();
  public static final UUID ACCOUNT_ID = UUID.randomUUID();
  private static final HeadObjectResponse VALID_HEAD_OBJECT_RESPONSE = HeadObjectResponse
      .builder()
      .contentLength(AccountVehicleDtoCsvRepository.MAX_FILE_SIZE_IN_BYTES - 1)
      .metadata(
          Collections.singletonMap(
              AccountVehicleDtoCsvRepository.UPLOADER_ID_METADATA_KEY, ACCOUNT_USER_ID.toString()
          )
      )
      .build();
  private static final String ANY_BUCKET = "bucket-x";
  private static final String ANY_FILE = "file-x";

  @Mock
  private S3Client s3Client;

  @Mock
  private CsvAccountVehicleObjectMapper csvAccountVehicleObjectMapper;

  @Mock
  private AccountUserRepository accountUserRepository;

  @InjectMocks
  private AccountVehicleDtoCsvRepository csvRepository;

  @Test
  public void shouldThrowIllegalArgumentExceptionWhenFilenameOrBucketIsNullOrEmpty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> csvRepository.findAll(null, "file-x"));
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> csvRepository.findAll("", ANY_FILE));

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> csvRepository.findAll(ANY_BUCKET, null));
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> csvRepository.findAll(ANY_BUCKET, ""));
  }

  @Test
  public void shouldThrowNoSuchKeyExceptionWhenGettingMetadataAndFileDoesNotExist() {
    mockExceptionWhenGettingS3HeadObject(NoSuchKeyException.builder().build());

    assertThatExceptionOfType(NoSuchKeyException.class)
        .isThrownBy(() -> csvRepository.findAll(ANY_BUCKET, ANY_FILE));
  }

  @Test
  public void shouldThrowS3MetadataExceptionWhenThereIsNoUploaderIdInMetadata() {
    mockS3HeadObjectResponse(HeadObjectResponse
        .builder()
        .contentLength(AccountVehicleDtoCsvRepository.MAX_FILE_SIZE_IN_BYTES + 1)
        .build());

    assertThatExceptionOfType(S3MetadataException.class)
        .isThrownBy(() -> csvRepository.findAll(ANY_BUCKET, ANY_FILE));
  }

  @Test
  public void shouldThrowNoSuchKeyExceptionWhenGettingContentsAndFileDoesNotExist() {
    mockValidS3HeadObjectResponse();
    mockExceptionWhenGettingS3Object(NoSuchKeyException.builder().build());
    mockUserPresenceInDatabase();

    assertThatExceptionOfType(NoSuchKeyException.class).isThrownBy(() -> {
      csvRepository.findAll(ANY_BUCKET, ANY_FILE);
    });
  }

  @Test
  public void shouldThrowRuntimeExceptionInCaseOfIOException() {
    mockValidS3HeadObjectResponse();
    mockExceptionWhenGettingS3Object(new IOException());
    mockUserPresenceInDatabase();

    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> {
      csvRepository.findAll(ANY_BUCKET, ANY_FILE);
    }).withCauseInstanceOf(IOException.class);
  }

  @Test
  public void shouldThrowS3MaxFileSizeExceededExceptionWhenFileIsTooBig() {
    // given
    mockS3HeadObjectResponseWithContentSize(
        AccountVehicleDtoCsvRepository.MAX_FILE_SIZE_IN_BYTES + 1);

    // when
    Throwable throwable = catchThrowable(() -> csvRepository.findAll(ANY_BUCKET, ANY_FILE));

    // then
    assertThat(throwable).isInstanceOf(S3MaxFileSizeExceededException.class);
  }

  @Test
  public void shouldProceedWhenFileSizeIsNotSet() {
    // given
    mockS3HeadObjectResponseWithContentSize(null);

    // when
    Throwable throwable = catchThrowable(() -> csvRepository.findAll(ANY_BUCKET, ANY_FILE));

    // then
    assertThat(throwable).isNotInstanceOf(S3MaxFileSizeExceededException.class);
  }

  @Test
  public void shouldThrowS3InvalidUploaderIdFormatExceptionWhenFileUploaderIdIsNotUUID() {
    // given
    mockS3HeadObjectResponseWithUploaderId("NotUUID");

    // when
    Throwable throwable = catchThrowable(() -> csvRepository.findAll(ANY_BUCKET, ANY_FILE));

    // then
    assertThat(throwable).isInstanceOf(S3InvalidUploaderIdFormatException.class);
  }

  @Test
  public void shouldRethrowSdkException() {
    mockValidS3HeadObjectResponse();
    mockExceptionWhenGettingS3Object(SdkException.builder().build());
    mockUserPresenceInDatabase();

    assertThatExceptionOfType(SdkException.class)
        .isThrownBy(() -> csvRepository.findAll(ANY_BUCKET, ANY_FILE));
  }

  @Test
  public void shouldParseDataFromFileAtS3() throws IOException {
    String content = "OI64EFO";
    AccountVehicleDto whitelistedVehicleDto = AccountVehicleDto.builder()
        .vrn("OI64EFO")
        .build();
    List<AccountVehicleDto> vehicles = Collections.singletonList(whitelistedVehicleDto);
    mockValidS3HeadObjectResponse();
    mockValidFileReading(content, vehicles);
    mockUserPresenceInDatabase();
    assertThat(csvRepository.findAll(ANY_BUCKET, ANY_FILE).getVehicles())
        .containsExactlyElementsOf(vehicles);
  }

  @Test
  public void shouldReturnDeleteStatusAsFalseIfAnyExceptionWasThrownDuringDeletingObject() {
    mockExceptionWhenDeletingS3Object(new RuntimeException());

    assertThat(csvRepository.purgeFile(ANY_BUCKET, ANY_FILE)).isFalse();
  }

  @Test
  public void shouldDeleteFromS3WithGivenBucketAndFile() {
    mockS3DeleteCallForSuccess();

    assertThat(csvRepository.purgeFile(ANY_BUCKET, ANY_FILE)).isTrue();
  }

  private void mockS3DeleteCallForSuccess() {
    when(s3Client
        .deleteObject(DeleteObjectRequest.builder().bucket(ANY_BUCKET).key(ANY_FILE).build()))
        .thenReturn(null);
  }

  private void mockExceptionWhenDeletingS3Object(Exception e) {
    when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
        .thenAnswer(answer -> {
          throw e;
        });
  }

  private void mockValidFileReading(String content, List<AccountVehicleDto> vehicles) throws IOException {
    mockS3ObjectResponse(content);
    when(csvAccountVehicleObjectMapper.read(any(InputStream.class)))
        .thenReturn(new CsvParseResult(vehicles, Collections.emptyList()));
  }

  private void mockS3ObjectResponse(String content) {
    ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(
        ANY_RESPONSE,
        content.getBytes()
    );
    when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);
  }

  private void mockS3HeadObjectResponseWithUploaderId(String notUUID) {
    HeadObjectResponse headObjectResponse = VALID_HEAD_OBJECT_RESPONSE.toBuilder()
        .metadata(
            Collections
                .singletonMap(AccountVehicleDtoCsvRepository.UPLOADER_ID_METADATA_KEY, notUUID)
        )
        .build();
    mockS3HeadObjectResponse(headObjectResponse);
  }

  private void mockS3HeadObjectResponseWithContentSize(Long fileSize) {
    mockS3HeadObjectResponse(VALID_HEAD_OBJECT_RESPONSE.toBuilder()
        .contentLength(fileSize)
        .build()
    );
  }

  private void mockExceptionWhenGettingS3Object(Exception e) {
    when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
        .thenAnswer(answer -> {
          throw e;
        });
  }

  private void mockExceptionWhenGettingS3HeadObject(Exception e) {
    when(s3Client.headObject(any(HeadObjectRequest.class)))
        .thenAnswer(answer -> {
          throw e;
        });
  }

  private void mockS3HeadObjectResponse(HeadObjectResponse headObjectResponse) {
    when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headObjectResponse);
  }

  private void mockValidS3HeadObjectResponse() {
    mockS3HeadObjectResponse(VALID_HEAD_OBJECT_RESPONSE);
  }

  private void mockUserPresenceInDatabase() {
    User user = User.builder()
        .accountId(ACCOUNT_ID)
        .id(ACCOUNT_USER_ID)
        .build();
    given(accountUserRepository.findById(ACCOUNT_USER_ID)).willReturn(Optional.of(user));
  }
}