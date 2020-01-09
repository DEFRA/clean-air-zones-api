package uk.gov.caz.taxiregister.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.caz.testutils.TestObjects.S3_REGISTER_JOB_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_CORRELATION_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.caz.csv.model.CsvValidationError;
import uk.gov.caz.taxiregister.DateHelper;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ConversionResult;
import uk.gov.caz.taxiregister.model.ConversionResults;
import uk.gov.caz.taxiregister.model.CsvFindResult;
import uk.gov.caz.taxiregister.model.LicenseDates;
import uk.gov.caz.taxiregister.model.LicensingAuthority;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicence;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobStatus;
import uk.gov.caz.taxiregister.repository.TaxiPhvLicenceCsvRepository;
import uk.gov.caz.taxiregister.service.exception.S3MetadataException;

@ExtendWith(MockitoExtension.class)
class RegisterFromCsvServiceTest {

  private static final int ANY_MAX_ERRORS_COUNT = 10;
  private static final String INSUFFICIENT_PERMISSIONS_ERROR_DETAILS = "Insufficient Permissions";

  @Mock
  private RegisterService registerService;

  @Mock
  private RegisterJobSupervisor registerJobSupervisor;

  @Mock
  private TaxiPhvLicenceCsvRepository csvRepository;

  @Mock
  private RegisterFromCsvExceptionResolver exceptionResolver;

  @Mock
  private VehicleToLicenceConverter vehicleToLicenceConverter;

  @Mock
  private LicencesRegistrationSecuritySentinel securitySentinel;

  private SourceAwareRegisterService registerFromCsvService;

  @BeforeEach
  public void setup() {
    RegisterServicesContext registerServicesContext = new RegisterServicesContext(
        registerService, exceptionResolver, registerJobSupervisor, vehicleToLicenceConverter,
        csvRepository, securitySentinel, null, ANY_MAX_ERRORS_COUNT, 0, 0, null
    );
    registerFromCsvService = new SourceAwareRegisterService(new RegisterCommandFactory(
        registerServicesContext));
  }

  @Test
  public void shouldGetDataFromS3AndDelegateToVehicleRegistrationService() throws SQLException {
    // given
    String bucket = "bucket-1";
    String filename = "records.csv";
    mockSecuritySentinelForNoErrors();
    List<VehicleDto> licences = mockDataAtS3(bucket, filename,
        TYPICAL_REGISTER_JOB_UPLOADER_ID);
    ConversionResults conversionResults = mockConversionResult(licences);
    RegisterResult registerResult = SuccessRegisterResult.with(extractLicensingAuthorityFrom(licences));
    given(
        registerService.register(conversionResults.getLicences(), TYPICAL_REGISTER_JOB_UPLOADER_ID))
        .willReturn(registerResult);
    
    given(
        registerJobSupervisor.hasActiveJobs(ArgumentMatchers.<Set<LicensingAuthority>>any()))
        .willReturn(false);

    // when
    RegisterResult actualRegisterResult = registerFromCsvService
        .register(bucket, filename, S3_REGISTER_JOB_ID, TYPICAL_CORRELATION_ID);

    // then
    assertThat(actualRegisterResult).isEqualTo(registerResult);
    verify(registerJobSupervisor).updateStatus(S3_REGISTER_JOB_ID, RegisterJobStatus.RUNNING);
    verify(registerJobSupervisor,times(1)).hasActiveJobs(ArgumentMatchers.<Set<LicensingAuthority>>any());
    verify(registerJobSupervisor,times(1)).lockImpactedLocalAuthorities(anyInt(),ArgumentMatchers.<Set<LicensingAuthority>>any());
    verify(registerJobSupervisor).markSuccessfullyFinished(
        S3_REGISTER_JOB_ID, registerResult.getAffectedLicensingAuthorities());
    verifyNoMoreInteractions(registerJobSupervisor);
  }

  private Set<LicensingAuthority> extractLicensingAuthorityFrom(
      List<VehicleDto> taxiPhvVehicleLicences) {
    return Collections.singleton(new LicensingAuthority(1,
        taxiPhvVehicleLicences.get(0).getLicensingAuthorityName()));
  }

  @Test
  public void shouldProperlyHandleValidationError() throws SQLException {
    // given
    String bucket = "bucket-1";
    String filename = "records.csv";
    mockSecuritySentinelForNoErrors();
    List<VehicleDto> taxiPhvVehicleLicences = mockDataAtS3(bucket, filename,
        TYPICAL_REGISTER_JOB_UPLOADER_ID);
    ConversionResults conversionResults = mockConversionResult(taxiPhvVehicleLicences);
    ValidationError validationError = ValidationError.s3Error("some error");
    RegisterResult registerResult = FailureRegisterResult.with(validationError);
    given(
        registerService.register(conversionResults.getLicences(), TYPICAL_REGISTER_JOB_UPLOADER_ID))
        .willReturn(registerResult);

    given(csvRepository.purgeFile(bucket, filename)).willReturn(true);

    given(
        registerJobSupervisor.hasActiveJobs(ArgumentMatchers.<Set<LicensingAuthority>>any()))
        .willReturn(false);

    // when
    RegisterResult actualRegisterResult = registerFromCsvService
        .register(bucket, filename, S3_REGISTER_JOB_ID, TYPICAL_CORRELATION_ID);

    // then
    assertThat(actualRegisterResult).isEqualTo(registerResult);
    verify(registerJobSupervisor).updateStatus(S3_REGISTER_JOB_ID, RegisterJobStatus.RUNNING);
    verify(registerJobSupervisor,times(1)).hasActiveJobs(ArgumentMatchers.<Set<LicensingAuthority>>any());
    verify(registerJobSupervisor,times(1)).lockImpactedLocalAuthorities(anyInt(),ArgumentMatchers.<Set<LicensingAuthority>>any());
    verify(registerJobSupervisor).markFailureWithValidationErrors(S3_REGISTER_JOB_ID,
        RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS,
        Collections.singletonList(validationError));
    verifyNoMoreInteractions(registerJobSupervisor);
  }

  @Test
  public void shouldProperlyHandleInsufficientPermissionsError() {
    // given
    String bucket = "bucket-1";
    String filename = "records.csv";
    List<VehicleDto> taxiPhvVehicleLicences = mockDataAtS3(bucket, filename,
        TYPICAL_REGISTER_JOB_UPLOADER_ID);
    mockConversionResult(taxiPhvVehicleLicences);
    mockSecuritySentinelForError(INSUFFICIENT_PERMISSIONS_ERROR_DETAILS);
    ValidationError validationError = ValidationError
        .insufficientPermissionsError(INSUFFICIENT_PERMISSIONS_ERROR_DETAILS);
    RegisterResult registerResult = FailureRegisterResult.with(validationError);
    given(csvRepository.purgeFile(bucket, filename)).willReturn(true);

    // when
    RegisterResult actualRegisterResult = registerFromCsvService
        .register(bucket, filename, S3_REGISTER_JOB_ID, TYPICAL_CORRELATION_ID);

    // then
    assertThat(actualRegisterResult).isEqualTo(registerResult);
    verify(registerJobSupervisor).updateStatus(S3_REGISTER_JOB_ID, RegisterJobStatus.RUNNING);
    verify(registerJobSupervisor).markFailureWithValidationErrors(S3_REGISTER_JOB_ID,
        RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS,
        Collections.singletonList(validationError));
    verifyNoMoreInteractions(registerJobSupervisor);
    verifyZeroInteractions(registerService);
  }

  @ParameterizedTest
  @MethodSource("validationErrorsProvider")
  public void shouldConcatenateValidationErrors(List<CsvValidationError> csvParseErrors,
      List<ValidationError> dtoValidationError) {
    // given
    String bucket = "bucket-1";
    String filename = "records.csv";
    ConversionResults conversionResults = ConversionResults.from(
        Collections.singletonList(ConversionResult.failure(dtoValidationError))
    );
    when(csvRepository.findAll(bucket, filename))
        .thenReturn(new CsvFindResult(TYPICAL_REGISTER_JOB_UPLOADER_ID, Collections.emptyList(),
            csvParseErrors));
    when(vehicleToLicenceConverter.convert(anyList(), anyInt())).thenReturn(conversionResults);

    // when
    RegisterResult actualRegisterResult = registerFromCsvService
        .register(bucket, filename, S3_REGISTER_JOB_ID, TYPICAL_CORRELATION_ID);

    assertThat(actualRegisterResult.isSuccess()).isFalse();
    Iterable<ValidationError> allValidationErrors = Iterables
        .concat(toValidationErrors(csvParseErrors), dtoValidationError);
    assertThat(actualRegisterResult.getValidationErrors())
        .containsExactlyInAnyOrderElementsOf(allValidationErrors);
  }

  private List<ValidationError> toValidationErrors(List<CsvValidationError> csvParseErrors) {
    return csvParseErrors.stream().map(ValidationError::valueErrorFrom)
        .collect(Collectors.toList());
  }

  @Test
  public void shouldMapExceptionFromResolver() {
    // given
    String bucket = "bucket-1";
    String filename = "records.csv";
    S3MetadataException exception = new S3MetadataException("a");
    ValidationError validationError = ValidationError.s3Error("some error");
    given(csvRepository.findAll(bucket, filename)).willThrow(exception);
    given(exceptionResolver.resolve(exception)).willReturn(FailureRegisterResult.with(validationError));
    given(exceptionResolver.resolveToRegisterJobFailureStatus(exception))
        .willReturn(RegisterJobStatus.STARTUP_FAILURE_INVALID_UPLOADER_ID);
    given(csvRepository.purgeFile(bucket, filename)).willReturn(true);

    // when
    RegisterResult result = registerFromCsvService
        .register(bucket, filename, S3_REGISTER_JOB_ID, TYPICAL_CORRELATION_ID);

    // then
    then(result.getValidationErrors()).containsExactly(validationError);
    verify(registerJobSupervisor).updateStatus(S3_REGISTER_JOB_ID, RegisterJobStatus.RUNNING);
    verify(registerJobSupervisor).markFailureWithValidationErrors(S3_REGISTER_JOB_ID,
        RegisterJobStatus.STARTUP_FAILURE_INVALID_UPLOADER_ID,
        Collections.singletonList(validationError));
    verify(registerJobSupervisor).updateStatus(S3_REGISTER_JOB_ID, RegisterJobStatus.RUNNING);
    verify(registerJobSupervisor).markFailureWithValidationErrors(S3_REGISTER_JOB_ID,
        RegisterJobStatus.STARTUP_FAILURE_INVALID_UPLOADER_ID,
        Collections.singletonList(validationError));
    verifyNoMoreInteractions(registerJobSupervisor);
  }

  private static Stream<Arguments> validationErrorsProvider() {
    return Stream.of(
        Arguments.arguments(
            Arrays.asList(
                CsvValidationError.with("csv parse error 1", 1),
                CsvValidationError.with("csv parse error 2", 2)
            ),
            Arrays.asList(
                ValidationError.valueError("3", "dto validation error 1"),
                ValidationError.valueError("4", "dto validation error 2")
            )
        ),
        Arguments.arguments(
            Collections.emptyList(),
            Arrays.asList(
                ValidationError.valueError("3", "dto validation error 1"),
                ValidationError.valueError("4", "dto validation error 2")
            )
        ),
        Arguments.arguments(
            Arrays.asList(
                CsvValidationError.with("csv parse error 1", 1),
                CsvValidationError.with("csv parse error 2", 2)
            ),
            Collections.emptyList()
        )
    );
  }

  private ConversionResults mockConversionResult(List<VehicleDto> vehicles) {
    VehicleDto vehicleDto = vehicles.iterator().next();
    ConversionResults conversionResults = ConversionResults.from(
        Collections.singletonList(ConversionResult.success(
            TaxiPhvVehicleLicence.builder()
                .vrm(vehicleDto.getVrm())
                .licenseDates(
                    new LicenseDates(
                        LocalDate.parse(vehicleDto.getStart()), LocalDate.parse(vehicleDto.getEnd())
                    )
                )
                .description(vehicleDto.getDescription().toUpperCase())
                .licensingAuthority(
                    LicensingAuthority.withNameOnly(vehicleDto.getLicensingAuthorityName()))
                .licensePlateNumber(vehicleDto.getLicensePlateNumber())
                .wheelchairAccessible(vehicleDto.getWheelchairAccessibleVehicle())
                .build()
            )
        )
    );
    given(vehicleToLicenceConverter.convert(vehicles, ANY_MAX_ERRORS_COUNT))
        .willReturn(conversionResults);
    return conversionResults;
  }

  private List<VehicleDto> mockDataAtS3(String bucket, String filename, UUID uploaderId) {
    VehicleDto licence = VehicleDto.builder()
        .vrm("8839GF")
        .start(DateHelper.today().toString())
        .end(DateHelper.tomorrow().toString())
        .description("taxi")
        .licensingAuthorityName("la-name")
        .licensePlateNumber("plate")
        .wheelchairAccessibleVehicle(true)
        .build();
    List<VehicleDto> licences = Collections.singletonList(licence);

    when(csvRepository.findAll(bucket, filename))
        .thenReturn(new CsvFindResult(uploaderId, licences, Collections.emptyList()));
    return licences;
  }

  private void mockSecuritySentinelForNoErrors() {
    given(securitySentinel
        .checkUploaderPermissionsToModifyLicensingAuthorities(TYPICAL_REGISTER_JOB_UPLOADER_ID,
            Sets.newHashSet(LicensingAuthority.withNameOnly("la-name"))))
        .willReturn(Optional.empty());
  }

  private void mockSecuritySentinelForError(String error) {
    given(securitySentinel
        .checkUploaderPermissionsToModifyLicensingAuthorities(TYPICAL_REGISTER_JOB_UPLOADER_ID,
            Sets.newHashSet(LicensingAuthority.withNameOnly("la-name"))))
        .willReturn(Optional.of(error));
  }
}