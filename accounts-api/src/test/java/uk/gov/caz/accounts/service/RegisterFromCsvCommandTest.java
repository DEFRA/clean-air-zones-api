package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.caz.accounts.util.TestObjects.S3_REGISTER_JOB_ID;
import static uk.gov.caz.accounts.util.TestObjects.TYPICAL_CORRELATION_ID;

import java.util.Arrays;
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
import uk.gov.caz.accounts.dto.AccountVehicleDto;
import uk.gov.caz.accounts.dto.CsvFindResult;
import uk.gov.caz.accounts.model.ConversionResult;
import uk.gov.caz.accounts.model.ConversionResults;
import uk.gov.caz.accounts.model.registerjob.RegisterJobStatus;
import uk.gov.caz.accounts.model.registerjob.ValidationError;
import uk.gov.caz.accounts.repository.AccountVehicleDtoCsvRepository;
import uk.gov.caz.accounts.service.chargecalculation.AsyncChargeCalculationStarter;
import uk.gov.caz.accounts.service.registerjob.RegisterJobSupervisor;
import uk.gov.caz.accounts.service.registerjob.RegisterResult;
import uk.gov.caz.accounts.service.registerjob.RegisterService;
import uk.gov.caz.accounts.util.TestObjects;
import uk.gov.caz.csv.model.CsvValidationError;

@ExtendWith(MockitoExtension.class)
class RegisterFromCsvCommandTest {

  private static final int ANY_MAX_ERRORS_COUNT = 10;
  public static final String BUCKET = "bucket";
  public static final String FILENAME = "filename";

  @Mock
  private AccountVehicleDtoCsvRepository csvRepository;

  @Mock
  private RegisterService registerService;

  @Mock
  private RegisterJobSupervisor registerJobSupervisor;

  @Mock
  private RegisterFromCsvExceptionResolver exceptionResolver;

  @Mock
  private AccountVehicleDtoToModelConverter converter;

  @Mock
  private AsyncChargeCalculationStarter asyncChargeCalculationStarter;

  private RegisterFromCsvCommand registerFromCsvCommand;

  @BeforeEach
  public void setup() {
    RegisterServiceContext context = new RegisterServiceContext(registerService,
        exceptionResolver, registerJobSupervisor, csvRepository, converter,
        asyncChargeCalculationStarter, ANY_MAX_ERRORS_COUNT);
    registerFromCsvCommand = new RegisterFromCsvCommand(context, S3_REGISTER_JOB_ID,
        TYPICAL_CORRELATION_ID, BUCKET, FILENAME, true);
  }

  @Test
  public void shouldFetchCsvResultsAndReturnResults() {
    // given
    UUID uploaderId = UUID.randomUUID();
    List<AccountVehicleDto> vehicles = Lists.list(AccountVehicleDto.builder().vrn("abc").build());
    List<CsvValidationError> validationErrors = Arrays.asList(
        CsvValidationError.with("error 1", 1),
        CsvValidationError.with("error 2", 2)
    );
    CsvFindResult csvFindResult = new CsvFindResult(uploaderId, vehicles, validationErrors);

    given(csvRepository.findAll(any(), any())).willReturn(csvFindResult);

    // when
    registerFromCsvCommand.beforeExecute();

    // then
    assertThat(registerFromCsvCommand.getParseValidationErrors())
        .hasSameSizeAs(validationErrors);
    assertThat(registerFromCsvCommand.getVehiclesToRegister()).isEqualTo(vehicles);
    verify(asyncChargeCalculationStarter, never()).fireAndForget(any(), anyInt(), any(), anyInt(),
        anyBoolean());
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
    verify(asyncChargeCalculationStarter, never()).fireAndForget(any(), anyInt(), any(), anyInt(),
        anyBoolean());
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
    RegisterResult result = registerFromCsvCommand.register();

    // then
    BDDAssertions.then(result.isSuccess()).isFalse();
    verify(registerJobSupervisor).markFailureWithValidationErrors(anyInt(), any(), anyList());
    verify(asyncChargeCalculationStarter, never()).fireAndForget(any(), anyInt(), any(), anyInt(),
        anyBoolean());
  }

  @Test
  public void shouldMarkJobFailedWhenThereAreParseValidationErrors() {
    // given
    CsvValidationError parseValidationError = CsvValidationError.with("detail", 1);
    List<AccountVehicleDto> vehicles = Collections.singletonList(
        AccountVehicleDto.builder().vrn("abc").build());
    CsvFindResult csvFindResult = new CsvFindResult(
        TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID, vehicles,
        Collections.singletonList(parseValidationError)
    );
    given(csvRepository.findAll(any(), any())).willReturn(csvFindResult);
    given(converter.convert(eq(vehicles), any(UUID.class)))
        .willReturn(ConversionResults.from(Collections.emptyList()));
    given(csvRepository.purgeFile(BUCKET, FILENAME)).willReturn(true);

    // when
    RegisterResult result = registerFromCsvCommand.register();

    // then
    BDDAssertions.then(result.isSuccess()).isFalse();
    verify(registerJobSupervisor).markFailureWithValidationErrors(anyInt(),
        eq(RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS), anyList());
    verify(asyncChargeCalculationStarter, never()).fireAndForget(any(), anyInt(), any(), anyInt(),
        anyBoolean());
  }

  @Test
  public void shouldMarkJobFailedWhenThereAreConversionValidationErrors() {
    // given
    List<ValidationError> conversionValidationErrors = Arrays
        .asList(ValidationError.valueError("detail", 1));
    List<AccountVehicleDto> vehicles = Collections.singletonList(
        AccountVehicleDto.builder().vrn("a-b-c").build());
    CsvFindResult csvFindResult = new CsvFindResult(
        TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID, vehicles,
        Collections.emptyList()
    );
    given(csvRepository.findAll(any(), any())).willReturn(csvFindResult);
    given(converter.convert(eq(vehicles), any(UUID.class)))
        .willReturn(ConversionResults.from(Collections.singletonList(
            ConversionResult.failure(conversionValidationErrors)
        )));
    given(csvRepository.purgeFile(BUCKET, FILENAME)).willReturn(true);

    // when
    RegisterResult result = registerFromCsvCommand.register();

    // then
    BDDAssertions.then(result.isSuccess()).isFalse();
    verify(registerJobSupervisor).markFailureWithValidationErrors(anyInt(),
        eq(RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS), anyList());
    verify(asyncChargeCalculationStarter, never()).fireAndForget(any(), anyInt(), any(), anyInt(),
        anyBoolean());
  }

  @Test
  public void shouldMarkJobFailedWhenRegistrationFails() {
    // given
    List<AccountVehicleDto> vehicles = Collections.singletonList(
        AccountVehicleDto.builder().vrn("abc").build());
    CsvFindResult csvFindResult = new CsvFindResult(TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID,
        vehicles, Collections.emptyList());
    ConversionResults conversionResults = ConversionResults.from(Collections.emptyList());
    given(csvRepository.findAll(any(), any())).willReturn(csvFindResult);
    given(converter.convert(eq(vehicles), any(UUID.class))).willReturn(conversionResults);
    given(registerService.register(conversionResults.getAccountVehicles(),
        TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID))
        .willReturn(RegisterResult.failure(Collections.emptyList()));
    given(csvRepository.purgeFile(BUCKET, FILENAME)).willReturn(true);

    // when
    RegisterResult result = registerFromCsvCommand.register();

    // then
    BDDAssertions.then(result.isSuccess()).isFalse();
    verify(registerJobSupervisor).markFailureWithValidationErrors(anyInt(),
        eq(RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS), anyList());
    verify(asyncChargeCalculationStarter, never()).fireAndForget(any(), anyInt(), any(), anyInt(),
        anyBoolean());
  }

  @Test
  public void shouldNotSetJobStatusIfFileWasNotDeletedFromS3() {
    // given
    List<AccountVehicleDto> vehicles = Collections.singletonList(
        AccountVehicleDto.builder().vrn("abc").build());
    CsvFindResult csvFindResult = new CsvFindResult(TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID,
        vehicles, Collections.emptyList());
    ConversionResults conversionResults = ConversionResults.from(Collections.emptyList());
    given(csvRepository.findAll(any(), any())).willReturn(csvFindResult);
    given(converter.convert(eq(vehicles), any(UUID.class))).willReturn(conversionResults);
    given(registerService.register(conversionResults.getAccountVehicles(),
        TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID))
        .willReturn(RegisterResult.failure(Collections.emptyList()));
    given(csvRepository.purgeFile(BUCKET, FILENAME)).willReturn(false);

    // when
    RegisterResult result = registerFromCsvCommand.register();

    // then
    BDDAssertions.then(result.isSuccess()).isFalse();
    verify(registerJobSupervisor, never())
        .markFailureWithValidationErrors(anyInt(), any(), anyList());
    verify(asyncChargeCalculationStarter, never()).fireAndForget(any(), anyInt(), any(), anyInt(),
        anyBoolean());
  }

  @Test
  public void shouldMarksAsSuccess() {
    // given
    List<AccountVehicleDto> vehicles = Collections.singletonList(
        AccountVehicleDto.builder().vrn("abc").build());
    CsvFindResult csvFindResult = new CsvFindResult(TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID,
        vehicles, Collections.emptyList());
    ConversionResults conversionResults = ConversionResults.from(Collections.emptyList());
    given(csvRepository.findAll(any(), any())).willReturn(csvFindResult);
    given(converter.convert(eq(vehicles), any(UUID.class))).willReturn(conversionResults);
    given(registerService.register(conversionResults.getAccountVehicles(),
        TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID))
        .willReturn(RegisterResult.success());

    // when
    RegisterResult result = registerFromCsvCommand.register();

    // then
    BDDAssertions.then(result.isSuccess()).isTrue();
    verify(registerJobSupervisor, never())
        .markFailureWithValidationErrors(anyInt(), any(), anyList());
    verify(asyncChargeCalculationStarter).fireAndForget(any(), anyInt(), any(), eq(1),
        anyBoolean());
  }
}