package uk.gov.caz.testutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.LicenseDates;
import uk.gov.caz.taxiregister.model.LicensingAuthority;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicence;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJob;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobName;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobStatus;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobTrigger;

public class TestObjects {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static final int S3_REGISTER_JOB_ID = 123;
  public static final int API_CALL_REGISTER_JOB_ID = 456;
  public static final String S3_REGISTER_JOB_NAME = "20190809_154821_CSV_FROM_S3_FILENAME";
  public static final String API_REGISTER_JOB_NAME = "20190809_154821_API_CALL";
  public static final String NOT_EXISTING_REGISTER_JOB_NAME = "NOT_EXISTING_JOB";
  public static final List<RegisterJobError> TYPICAL_REGISTER_JOB_ERRORS = ImmutableList.of(
      RegisterJobError.withDetailOnly("error 1"),
      RegisterJobError.withDetailOnly("error 2")
  );
  public static final String TYPICAL_REGISTER_JOB_ERRORS_JOINED = convertToString(
      TYPICAL_REGISTER_JOB_ERRORS);

  public static final List<RegisterJobError> MODIFIED_REGISTER_JOB_ERRORS = ImmutableList.of(
      RegisterJobError.withDetailOnly("new error 1"),
      RegisterJobError.withDetailOnly("new error 2"),
      RegisterJobError.withDetailOnly("new error 3")
  );

  public static final List<ValidationError> MODIFIED_REGISTER_JOB_VALIDATION_ERRORS = MODIFIED_REGISTER_JOB_ERRORS
      .stream()
      .map(ValidationError::from)
      .collect(Collectors.toList());
  public static final RegisterJobTrigger S3_REGISTER_JOB_TRIGGER = RegisterJobTrigger.CSV_FROM_S3;
  public static final RegisterJobStatus TYPICAL_RUNNING_REGISTER_JOB_STATUS = RegisterJobStatus.RUNNING;
  public static final RegisterJobStatus TYPICAL_STARTING_REGISTER_JOB_STATUS = RegisterJobStatus.STARTING;
  public static final UUID TYPICAL_REGISTER_JOB_UPLOADER_ID = UUID
      .fromString("11111111-2222-3333-4444-555555555555");
  public static final String TYPICAL_CORRELATION_ID = "CorrelationId";
  public static String VALID_API_KEY = TYPICAL_REGISTER_JOB_UPLOADER_ID.toString();

  public static final UUID TYPICAL_UPLOADER_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
  public static final List<UUID> TYPICAL_UPLOADER_IDS = Lists
      .newArrayList(UUID.fromString("11111111-2222-3333-4444-555555555555"),
          UUID.fromString("11111111-2222-3333-4444-555555555556"));

  public static final RegisterJob S3_FINISHED_REGISTER_JOB = RegisterJob.builder()
      .id(S3_REGISTER_JOB_ID)
      .uploaderId(TYPICAL_REGISTER_JOB_UPLOADER_ID)
      .jobName(new RegisterJobName(S3_REGISTER_JOB_NAME))
      .status(RegisterJobStatus.FINISHED_SUCCESS)
      .trigger(S3_REGISTER_JOB_TRIGGER)
      .correlationId(TYPICAL_CORRELATION_ID)
      .build();

  public static final RegisterJob S3_RUNNING_REGISTER_JOB = S3_FINISHED_REGISTER_JOB.toBuilder()
      .id(S3_REGISTER_JOB_ID)
      .status(TYPICAL_RUNNING_REGISTER_JOB_STATUS)
      .errors(TYPICAL_REGISTER_JOB_ERRORS)
      .build();

  public static class Registration {

    public static String correlationId() {
      return "63be7528-7efd-4f31-ae68-11a6b709ff1c";
    }

    public static UUID uploaderId() {
      return UUID.fromString("11111111-2222-3333-4444-555555555555");
    }

    public static class Csv {
      public static String bucket() {
        return "NTR-DATA";
      }

      public static String filename() {
        return "file-with-data.csv";
      }

      public static int registerJobId() {
        return 123;
      }
    }

    public static class VehicleDtos {
      private static final VehicleDto LICENCE = VehicleDto.builder()
          .vrm("OI64EFO")
          .start("2019-04-30")
          .end("2019-05-22")
          .description("taxi")
          .licensingAuthorityName("la-1")
          .licensePlateNumber("dJfRR")
          .wheelchairAccessibleVehicle(true)
          .build();

      private static final List<VehicleDto> TO_BE_REGISTERED = ImmutableList.of(
          LICENCE.toBuilder().vrm("1839GF").build(),
          LICENCE.toBuilder().vrm("2839GF").build(),
          LICENCE.toBuilder().vrm("3839GF").build()
      );

      public static List<VehicleDto> toBeRegistered() {
        return TO_BE_REGISTERED;
      }
    }
  }

  public static class Licences {

    public static String validVrm() {
      return "8839GF";
    }

    public static TaxiPhvVehicleLicence existing() {
      return TaxiPhvVehicleLicence
          .builder()
          .id(1)
          .uploaderId(TestObjects.Registration.uploaderId())
          .vrm("8839GF")
          .wheelchairAccessible(true)
          .licensePlateNumber("old")
          .description("TAXI")
          .licenseDates(new LicenseDates(LocalDate.now(), LocalDate.now().plusDays(1)))
          .licensingAuthority(LicensingAuthorities.existing())
          .build();
    }

    public static TaxiPhvVehicleLicence toBeRegistered() {
      return existing()
          .toBuilder()
          .id(null)
          .uploaderId(null)
          .licensingAuthority(
              LicensingAuthority.withNameOnly(existing().getLicensingAuthority().getName()))
          .build();
      }
    }

  public static class LicensingAuthorities {
    public static LicensingAuthority existing() {
      return new LicensingAuthority(1, "la-1");
    }

    public static Set<LicensingAuthority> existingAsSingleton() {
      return Collections.singleton(existing());
    }
  }

  @SneakyThrows
  private static String convertToString(List<RegisterJobError> typicalRegisterJobErrors) {
    return objectMapper.writeValueAsString(typicalRegisterJobErrors);
  }
}
