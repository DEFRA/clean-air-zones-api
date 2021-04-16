package uk.gov.caz.taxiregister.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static uk.gov.caz.testutils.TestObjects.S3_REGISTER_JOB_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_CORRELATION_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_UPLOADER_EMAIL;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.csv.model.CsvValidationError;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.CsvFindResult;
import uk.gov.caz.taxiregister.repository.TaxiPhvLicenceCsvRepository;

@ExtendWith(MockitoExtension.class)
class RegisterFromCsvCommandTest {

  @Mock
  private TaxiPhvLicenceCsvRepository csvRepository;

  private RegisterFromCsvCommand registerFromCsvCommand;

  @BeforeEach
  public void setup() {
    registerFromCsvCommand = new RegisterFromCsvCommand(
        new RegisterServicesContext(null, null, null, null, csvRepository, null, null, null, null,
            null, 0, 0, null, null),
        S3_REGISTER_JOB_ID,
        TYPICAL_CORRELATION_ID, "bucket", "filename");
  }

  @Test
  public void shouldFetchCsvResultAndReturnResults() {
    // given
    UUID uploaderId = UUID.randomUUID();
    List<VehicleDto> licences = Lists.list(VehicleDto.builder().vrm("abc").build());
    List<CsvValidationError> validationErrors = Arrays.asList(
        CsvValidationError.with("error 1", 1),
        CsvValidationError.with("error 2", 2)
    );
    CsvFindResult csvFindResult = new CsvFindResult(uploaderId, TYPICAL_UPLOADER_EMAIL, licences,
        validationErrors);

    given(csvRepository.findAll(any(), any())).willReturn(csvFindResult);

    // when
    registerFromCsvCommand.beforeExecute();

    // then
    assertThat(registerFromCsvCommand.getLicencesParseValidationErrors())
        .hasSameSizeAs(validationErrors);
    assertThat(registerFromCsvCommand.getLicencesToRegister()).isEqualTo(licences);
    assertThat(registerFromCsvCommand.getUploaderId()).isEqualTo(uploaderId);
  }

  @Test
  public void shouldThrowExceptionWhenGettingParseErrorsIfBeforeExecuteHasNotBeenCalled() {

    // when
    Throwable throwable = catchThrowable(
        () -> registerFromCsvCommand.getLicencesParseValidationErrors());

    // then
    assertThat(throwable).isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void shouldThrowExceptionWhenGettingUploaderIdIfBeforeExecuteHasNotBeenCalled() {

    // when
    Throwable throwable = catchThrowable(() -> registerFromCsvCommand.getUploaderId());

    // then
    assertThat(throwable).isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void shouldThrowExceptionWhenGettingLicencesToRegisterIfBeforeExecuteHasNotBeenCalled() {

    // when
    Throwable throwable = catchThrowable(() -> registerFromCsvCommand.getLicencesToRegister());

    // then
    assertThat(throwable).isInstanceOf(IllegalStateException.class);
  }
}