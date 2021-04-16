package uk.gov.caz.whitelist.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_EMAIL;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID;
import static uk.gov.caz.whitelist.model.Actions.DELETE;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDto;
import uk.gov.caz.whitelist.model.CsvParseResult;
import uk.gov.caz.whitelist.service.CsvFileOnS3MetadataExtractor;
import uk.gov.caz.whitelist.service.CsvFileOnS3MetadataExtractor.CsvMetadata;
import uk.gov.caz.whitelist.service.CsvObjectMapper;
import uk.gov.caz.whitelist.service.exception.FatalErrorWithCsvFileMetadataException;

@ExtendWith(MockitoExtension.class)
class WhitelistedVehicleDtoCsvRepositoryTest {

  private static final GetObjectResponse ANY_RESPONSE = GetObjectResponse.builder().build();
  private static final String ANY_BUCKET = "bucket-x";
  private static final String ANY_FILE = "file-x";

  @Mock
  private S3Client s3Client;

  @Mock
  private CsvObjectMapper csvObjectMapper;

  @Mock
  private CsvFileOnS3MetadataExtractor metadataExtractor;

  @InjectMocks
  private WhitelistedVehicleDtoCsvRepository csvRepository;

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
    metadataExtractorThrowException();

    assertThatExceptionOfType(FatalErrorWithCsvFileMetadataException.class)
        .isThrownBy(() -> csvRepository.findAll(ANY_BUCKET, ANY_FILE));
  }

  @Test
  public void shouldThrowNoSuchKeyExceptionWhenGettingContentsAndFileDoesNotExist() {
    mockMetadataExtractor();
    mockExceptionWhenGettingS3Object(NoSuchKeyException.builder().build());

    assertThatExceptionOfType(NoSuchKeyException.class).isThrownBy(() -> {
      csvRepository.findAll(ANY_BUCKET, ANY_FILE);
    });
  }

  @Test
  public void shouldThrowRuntimeExceptionInCaseOfIOException() {
    mockMetadataExtractor();
    mockExceptionWhenGettingS3Object(new IOException());

    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> {
      csvRepository.findAll(ANY_BUCKET, ANY_FILE);
    }).withCauseInstanceOf(IOException.class);
  }

  @Test
  public void shouldRethrowSdkException() {
    mockMetadataExtractor();
    mockExceptionWhenGettingS3Object(SdkException.builder().build());

    assertThatExceptionOfType(SdkException.class)
        .isThrownBy(() -> csvRepository.findAll(ANY_BUCKET, ANY_FILE));
  }

  @Test
  public void shouldParseDataFromFileAtS3() throws IOException {
    String content = "OI64EFO,reason,manu,D";
    WhitelistedVehicleDto whitelistedVehicleDto = WhitelistedVehicleDto.builder()
        .vrn("OI64EFO")
        .reason("reason")
        .action(DELETE.getActionCharacter())
        .manufacturer(Optional.of("manu"))
        .build();
    List<WhitelistedVehicleDto> vehicles = Collections.singletonList(whitelistedVehicleDto);
    mockMetadataExtractor();
    mockValidFileReading(content, vehicles);

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

  private void mockMetadataExtractor() {
    when(metadataExtractor.getRequiredMetadata(ANY_BUCKET, ANY_FILE))
        .thenReturn(new CsvMetadata(TYPICAL_REGISTER_JOB_UPLOADER_ID, TYPICAL_EMAIL));
  }

  private void metadataExtractorThrowException() {
    when(metadataExtractor.getRequiredMetadata(ANY_BUCKET, ANY_FILE))
        .thenThrow(new FatalErrorWithCsvFileMetadataException(""));
  }

  private void mockExceptionWhenDeletingS3Object(Exception e) {
    when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
        .thenAnswer(answer -> {
          throw e;
        });
  }

  private void mockS3DeleteCallForSuccess() {
    when(s3Client
        .deleteObject(DeleteObjectRequest.builder().bucket(ANY_BUCKET).key(ANY_FILE).build()))
        .thenReturn(null);
  }

  private void mockValidFileReading(String content, List<WhitelistedVehicleDto> licences)
      throws IOException {
    mockS3ObjectResponse(content);
    when(csvObjectMapper.read(any(InputStream.class)))
        .thenReturn(new CsvParseResult(licences, Collections.emptyList()));
  }

  private void mockExceptionWhenGettingS3Object(Exception e) {
    when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
        .thenAnswer(answer -> {
          throw e;
        });
  }

  private void mockS3ObjectResponse(String content) {
    ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(
        ANY_RESPONSE,
        content.getBytes()
    );
    when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);
  }
}