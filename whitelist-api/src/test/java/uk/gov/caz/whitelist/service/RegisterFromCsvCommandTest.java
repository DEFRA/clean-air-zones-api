package uk.gov.caz.whitelist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.caz.testutils.TestObjects.MODIFIED_REGISTER_JOB_VALIDATION_ERRORS;
import static uk.gov.caz.testutils.TestObjects.S3_REGISTER_JOB_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_CORRELATION_ID;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.BDDAssertions;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.testutils.TestObjects;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDto;
import uk.gov.caz.whitelist.model.ConversionResults;
import uk.gov.caz.whitelist.model.CsvFindResult;
import uk.gov.caz.whitelist.model.ValidationError;
import uk.gov.caz.whitelist.model.registerjob.RegisterJobStatus;
import uk.gov.caz.whitelist.repository.AuditingRepository;
import uk.gov.caz.whitelist.repository.WhitelistedVehicleDtoCsvRepository;

@ExtendWith(MockitoExtension.class)
class RegisterFromCsvCommandTest {

  private static final int ANY_MAX_ERRORS_COUNT = 10;
  public static final String BUCKET = "bucket";
  public static final String FILENAME = "filename";

  @Mock
  private WhitelistedVehicleDtoCsvRepository csvRepository;

  @Mock
  private AuditingRepository auditingRepository;

  @Mock
  private RegisterService registerService;

  @Mock
  private RegisterJobSupervisor jobSupervisor;

  @Mock
  private RegisterFromCsvExceptionResolver exceptionResolver;

  @Mock
  private WhitelistedVehicleDtoToModelConverter converter;

  private RegisterFromCsvCommand registerFromCsvCommand;

  @Mock
  private WhitelistService whitelistService;

  @BeforeEach
  public void setup() {
    RegisterServicesContext context = new RegisterServicesContext(registerService,
        exceptionResolver, jobSupervisor, converter, csvRepository, ANY_MAX_ERRORS_COUNT);
    registerFromCsvCommand = new RegisterFromCsvCommand(context, S3_REGISTER_JOB_ID,
        TYPICAL_CORRELATION_ID,
        BUCKET, FILENAME);
  }

  @Test
  public void shouldFetchCsvResultAndReturnResults() {
    //given
    final UUID uploaderId = UUID.randomUUID();
    final List<WhitelistedVehicleDto> vehicles = Lists
        .list(WhitelistedVehicleDto.builder().vrn("abc").build());
    final List<ValidationError> validationErrors = MODIFIED_REGISTER_JOB_VALIDATION_ERRORS;
    final CsvFindResult csvFindResult = new CsvFindResult(TestObjects.TYPICAL_EMAIL, uploaderId, vehicles,
        validationErrors);

    given(csvRepository.findAll(any(), any())).willReturn(csvFindResult);

    //when
    registerFromCsvCommand.beforeExecute();

    //then
    assertThat(registerFromCsvCommand.getParseValidationErrors())
        .isEqualTo(MODIFIED_REGISTER_JOB_VALIDATION_ERRORS);
    assertThat(registerFromCsvCommand.getVehiclesToRegister()).isEqualTo(vehicles);
  }

  @Test
  public void shouldThrowExceptionIfCsvFindResultIsNull() {
    //given
    given(csvRepository.findAll(any(), any())).willReturn(null);

    //when
    registerFromCsvCommand.beforeExecute();

    //then
    assertThrows(IllegalStateException.class,
        () -> registerFromCsvCommand.getParseValidationErrors());
    assertThrows(IllegalStateException.class, () -> registerFromCsvCommand.getVehiclesToRegister());
  }

  @Test
  public void shouldMarkJobFailedWhenExceptionOccursDuringExecution() {
    // given
    RuntimeException exception = new RuntimeException();
    given(csvRepository.findAll(any(), any())).willThrow(exception);
    given(exceptionResolver.resolve(exception))
        .willReturn(RegisterResult.failure(Collections.emptyList()));
    given(csvRepository.purgeFile(BUCKET, FILENAME)).willReturn(true);

    // when
    RegisterResult result = registerFromCsvCommand.execute();

    // then
    BDDAssertions.then(result.isSuccess()).isFalse();
    verify(jobSupervisor).markFailureWithValidationErrors(anyInt(), any(), anyList());
  }

  @Test
  public void shouldMarkJobFailedWhenThereAreParseValidationErrors() {
    // given
    ValidationError parseValidationError = ValidationError.valueError("detail", 1);
    List<WhitelistedVehicleDto> vehicles = Collections.singletonList(
        WhitelistedVehicleDto.builder().vrn("abc").build());
    CsvFindResult csvFindResult = new CsvFindResult(TestObjects.TYPICAL_EMAIL,
        TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID, vehicles,
        Collections.singletonList(parseValidationError)
    );
    given(csvRepository.findAll(any(), any())).willReturn(csvFindResult);
    given(converter.convert(eq(vehicles), any(UUID.class), anyString()))
        .willReturn(ConversionResults.from(Collections.emptyList()));
    given(csvRepository.purgeFile(BUCKET, FILENAME)).willReturn(true);

    // when
    RegisterResult result = registerFromCsvCommand.execute();

    // then
    BDDAssertions.then(result.isSuccess()).isFalse();
    verify(jobSupervisor).markFailureWithValidationErrors(anyInt(),
        eq(RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS), anyList());
  }

  @Test
  public void shouldMarkJobFailedWhenRegistrationFails() {
    // given
    List<WhitelistedVehicleDto> vehicles = Collections.singletonList(
        WhitelistedVehicleDto.builder().vrn("abc").build());
    CsvFindResult csvFindResult = new CsvFindResult(TestObjects.TYPICAL_EMAIL,
        TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID,
        vehicles, Collections.emptyList());
    ConversionResults conversionResults = ConversionResults.from(Collections.emptyList());
    given(csvRepository.findAll(any(), any())).willReturn(csvFindResult);
    given(converter.convert(eq(vehicles), any(UUID.class), anyString()))
        .willReturn(conversionResults);
    given(registerService.register(conversionResults, TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID,
        TestObjects.TYPICAL_EMAIL))
        .willReturn(RegisterResult.failure(Collections.emptyList()));
    given(csvRepository.purgeFile(BUCKET, FILENAME)).willReturn(true);

    // when
    RegisterResult result = registerFromCsvCommand.execute();

    // then
    BDDAssertions.then(result.isSuccess()).isFalse();
    verify(jobSupervisor).markFailureWithValidationErrors(anyInt(),
        eq(RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS), anyList());
  }

  @Test
  public void shouldNotSetJobStatusIfFileWasntDeletedFromS3() {
    // given
    List<WhitelistedVehicleDto> vehicles = Collections.singletonList(
        WhitelistedVehicleDto.builder().vrn("abc").build());
    CsvFindResult csvFindResult = new CsvFindResult(TestObjects.TYPICAL_EMAIL,
        TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID,
        vehicles, Collections.emptyList());
    ConversionResults conversionResults = ConversionResults.from(Collections.emptyList());
    given(csvRepository.findAll(any(), any())).willReturn(csvFindResult);
        given(converter.convert(eq(vehicles), any(UUID.class), anyString()))
        .willReturn(conversionResults);
    given(registerService.register(conversionResults, TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID,
        TestObjects.TYPICAL_EMAIL))
        .willReturn(RegisterResult.failure(Collections.emptyList()));
    given(csvRepository.purgeFile(BUCKET, FILENAME)).willReturn(false);

    // when
    RegisterResult result = registerFromCsvCommand.execute();

    // then
    BDDAssertions.then(result.isSuccess()).isFalse();
    verify(jobSupervisor, never()).markFailureWithValidationErrors(anyInt(), any(), anyList());
  }

  @Test
  public void shouldMarksAsSuccess() {
    // given
    List<WhitelistedVehicleDto> vehicles = Collections.singletonList(
        WhitelistedVehicleDto.builder().vrn("abc").build());
    CsvFindResult csvFindResult = new CsvFindResult(TestObjects.TYPICAL_EMAIL,
        TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID,
        vehicles, Collections.emptyList());
    ConversionResults conversionResults = ConversionResults.from(Collections.emptyList());
    given(csvRepository.findAll(any(), any())).willReturn(csvFindResult);
    given(converter.convert(eq(vehicles), any(UUID.class), anyString()))
        .willReturn(conversionResults);
    given(registerService.register(conversionResults, TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID,
        TestObjects.TYPICAL_EMAIL))
        .willReturn(RegisterResult.success());

    // when
    RegisterResult result = registerFromCsvCommand.execute();

    // then
    BDDAssertions.then(result.isSuccess()).isTrue();
    verify(jobSupervisor, never()).markFailureWithValidationErrors(anyInt(), any(), anyList());
  }
}