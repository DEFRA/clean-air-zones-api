package uk.gov.caz.taxiregister.service;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ConversionResult;
import uk.gov.caz.taxiregister.model.ConversionResults;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobTrigger;

@ExtendWith(MockitoExtension.class)
class VehicleToLicenceConverterTest {

  private VehicleToLicenceConverter converter = new VehicleToLicenceConverter();

  @Nested
  class WhenConvertingOneLicence {

    @Test
    public void shouldReturnSuccessWhenConvertVehicleWithWheelchairAccessibleVehicle() {
      // given
      VehicleDto licence = createValidLicenceWithWheelchairAccessible();

      // when
      ConversionResult conversionResult = converter.toLicence(licence);

      // then
      then(conversionResult.isSuccess()).isTrue();
      then(conversionResult.isFailure()).isFalse();
      then(conversionResult.getValidationErrors()).isEmpty();
      then(conversionResult.getLicence()).isNotNull();
    }

    @Test
    public void shouldReturnSuccessWhenDDMMYYYYDateSupplied() {
      // given
      VehicleDto licence = createValidLicenceWithDDMMYYYYDate();

      // when
      ConversionResult conversionResult = converter.toLicence(licence);

      // then
      then(conversionResult.isSuccess()).isTrue();
      then(conversionResult.isFailure()).isFalse();
      then(conversionResult.getValidationErrors()).isEmpty();
      then(conversionResult.getLicence()).isNotNull();
    }

    @Test
    public void shouldReturnSuccessWhenConvertVehicleWithoutWheelchairAccessibleVehicle() {
      // given
      VehicleDto licence = createValidLicenceWithoutWheelchairAccessible();

      // when
      ConversionResult conversionResult = converter.toLicence(licence);

      // then
      then(conversionResult.isSuccess()).isTrue();
      then(conversionResult.isFailure()).isFalse();
      then(conversionResult.getValidationErrors()).isEmpty();
      then(conversionResult.getLicence()).isNotNull();
    }

    @Test
    public void shouldReturnFailureWhenValidationFails() {
      // given
      VehicleDto licence = createInvalidLicence();

      // when
      ConversionResult conversionResult = converter.toLicence(licence);

      // then
      then(conversionResult.isSuccess()).isFalse();
      then(conversionResult.isFailure()).isTrue();
      then(conversionResult.getValidationErrors()).isNotEmpty();
      then(conversionResult.getLicence()).isNull();
    }

    @Test
    public void invalidWheelchairAccessibleErrorDoesNotImmediatelyStopValidationProcess() {
      // given
      VehicleDto licence = createLicenceWithInvalidDateAndWheelchairAccessibleFlag();

      // when
      ConversionResult conversionResult = converter.toLicence(licence);

      // then
      then(conversionResult.isSuccess()).isFalse();
      then(conversionResult.isFailure()).isTrue();
      then(conversionResult.getValidationErrors()).hasSize(2);
      then(conversionResult.getLicence()).isNull();
    }
  }

  @Nested
  class WhenConvertingListOfLicences {

    @Test
    public void shouldReturnSuccessWhenDDMMYYYYDateSupplied() {
      // givens
      List<VehicleDto> licences = Collections.singletonList(createValidLicenceWithDDMMYYYYDate());

      // when
      ConversionResults conversionResults = converter.convert(licences);

      // then
      then(conversionResults.hasValidationErrors()).isFalse();
      then(conversionResults.getLicences()).hasSize(1);
    }

    @Test
    public void shouldReturnConvertedLicencesWithWheelchairAccessibleVehicle() {
      // given
      List<VehicleDto> licences = Collections
          .singletonList(createValidLicenceWithWheelchairAccessible());

      // when
      ConversionResults conversionResults = converter.convert(licences);

      // then
      then(conversionResults.hasValidationErrors()).isFalse();
      then(conversionResults.getLicences()).hasSize(1);
    }

    @Test
    public void shouldReturnConvertedLicencesWhenWheelchairAccessibleVehicleIsNull() {
      // given
      List<VehicleDto> licences = Collections.singletonList(
          createValidLicenceWithoutWheelchairAccessible(null));

      // when
      ConversionResults conversionResults = converter.convert(licences);

      // then
      then(conversionResults.hasValidationErrors()).isFalse();
      then(conversionResults.getLicences()).hasSize(1);
    }

    @Test
    public void shouldReturnConvertedLicencesWhenWheelchairAccessibleVehicleIsEmpty() {
      // given
      List<VehicleDto> licences = Collections.singletonList(
          createValidLicenceWithoutWheelchairAccessible(""));

      // when
      ConversionResults conversionResults = converter.convert(licences);

      // then
      then(conversionResults.hasValidationErrors()).isFalse();
      then(conversionResults.getLicences()).hasSize(1);
    }

    @Test
    public void shouldReturnValidationErrorsAndConvertedLicences() {
      // given
      List<VehicleDto> licences = Arrays.asList(
          createValidLicenceWithWheelchairAccessible(),
          createValidLicenceWithoutWheelchairAccessible(),
          createInvalidLicence()
      );

      // when
      ConversionResults conversionResults = converter.convert(licences);

      // then
      then(conversionResults.hasValidationErrors()).isTrue();
      then(conversionResults.getValidationErrors()).hasSize(1);
      then(conversionResults.getLicences()).hasSize(2);
    }

    @Test
    public void shouldFlattenValidationErrorsFromMoreThanOneLicence() {
      // given
      List<VehicleDto> licences = Arrays.asList(
          createValidLicenceWithWheelchairAccessible(),
          createValidLicenceWithoutWheelchairAccessible(),
          createInvalidLicence()
      );

      // when
      ConversionResults conversionResults = converter.convert(licences);

      // then
      then(conversionResults.hasValidationErrors()).isTrue();
      then(conversionResults.getValidationErrors()).hasSize(1);
      then(conversionResults.getLicences()).hasSize(2);
    }

    @Test
    public void shouldConvertTenErrors() {
      // given
      List<VehicleDto> licences = Arrays.asList(
          createInvalidLicenceWithTwoAttributes(),
          createInvalidLicenceWithTwoAttributes(),
          createInvalidLicenceWithThreeAttributes(),
          createInvalidLicenceWithThreeAttributes()
      );

      // when
      ConversionResults conversionResults = converter.convert(licences);

      // then
      then(conversionResults.getValidationErrors()).hasSize(10);
      then(conversionResults.getLicences()).isEmpty();
    }
  }

  @Nested
  class WhenConvertingLicencesWithDuplicatedUniqueAttributes {

    @Test
    public void shouldNoticeDuplicateEvenIfDescriptionFieldIsDifferent() {
      //given
      VehicleDto vehicle1 = createVehicleWithDescription("desc 1");
      VehicleDto vehicle2 = createVehicleWithDescription("desc 2");

      //when
      ConversionResults conversionResults = converter
          .convert(Lists.newArrayList(vehicle1, vehicle2));

      //then
      then(conversionResults.hasValidationErrors()).isTrue();
      then(conversionResults.getValidationErrors()).hasSize(1);
    }

    @Test
    public void shouldNoticeDuplicateEvenIfWheelchairAccessibleFieldIsDifferent() {
      //given
      VehicleDto vehicle1 = createVehicleWithWheelchairAccessibleField(true);
      VehicleDto vehicle2 = createVehicleWithWheelchairAccessibleField(false);

      //when
      ConversionResults conversionResults = converter
          .convert(Lists.newArrayList(vehicle1, vehicle2));

      //then
      then(conversionResults.hasValidationErrors()).isTrue();
      then(conversionResults.getValidationErrors()).hasSize(1);
    }
  }

  @Test
  public void shouldReturnNonUniqueErrorForDuplicatedVehicle() {
    // given
    List<VehicleDto> licences = Arrays.asList(
        createValidLicenceWithDDMMYYYYDate(),
        createValidLicenceWithDDMMYYYYDate()
    );

    // when
    ConversionResults conversionResults = converter.convert(licences);

    // then
    then(conversionResults.getValidationErrors()).isNotEmpty();
  }

  private VehicleDto createVehicleWithWheelchairAccessibleField(boolean wheelchairAccessible) {
    return createValidLicenceWithDDMMYYYYDate().toBuilder()
        .wheelchairAccessibleVehicle(Boolean.toString(wheelchairAccessible)).build();
  }

  private static VehicleDto createVehicleWithDescription(String description) {
    return VehicleDto.builder()
        .vrm("AAA999A")
        .start("01/01/2019")
        .end("01/01/2019")
        .description(description)
        .licensingAuthorityName("la-name-1")
        .licensePlateNumber("plate-1")
        .wheelchairAccessibleVehicle("true")
        .registerJobTrigger(RegisterJobTrigger.CSV_FROM_S3)
        .build();
  }

  private VehicleDto createValidLicenceWithDDMMYYYYDate() {
    return VehicleDto.builder()
        .vrm("AAA999A")
        .start("01/01/2019")
        .end("01/01/2019")
        .description("taxi")
        .licensingAuthorityName("la-name-1")
        .licensePlateNumber("plate-1")
        .wheelchairAccessibleVehicle("true")
        .registerJobTrigger(RegisterJobTrigger.CSV_FROM_S3)
        .build();
  }

  private VehicleDto createValidLicenceWithWheelchairAccessible() {
    return VehicleDto.builder()
        .vrm("AAA999A")
        .start("2019-01-01")
        .end("2019-02-01")
        .description("taxi")
        .licensingAuthorityName("la-name-1")
        .licensePlateNumber("plate-1")
        .wheelchairAccessibleVehicle("true")
        .build();
  }

  private VehicleDto createValidLicenceWithoutWheelchairAccessible() {
    return VehicleDto.builder()
        .vrm("BW91HUN")
        .start("2019-03-09")
        .end("2019-05-06")
        .description("taxi")
        .licensingAuthorityName("la-1")
        .licensePlateNumber("yGSJC")
        .build();
  }

  private VehicleDto createValidLicenceWithoutWheelchairAccessible(String nullOrEmpty) {
    return VehicleDto.builder()
        .vrm("BW91HUN")
        .start("2019-03-09")
        .end("2019-05-06")
        .description("taxi")
        .licensingAuthorityName("la-1")
        .licensePlateNumber("yGSJC")
        .wheelchairAccessibleVehicle(nullOrEmpty)
        .build();
  }

  private VehicleDto createInvalidLicence() {
    return VehicleDto.builder()
        .vrm("9AAAA99")
        .start("2019-01-01")
        .end("2019-02-01")
        .description("taxi")
        .licensingAuthorityName("la-name-1")
        .licensePlateNumber("plate-1")
        .wheelchairAccessibleVehicle("true")
        .build();
  }

  private VehicleDto createLicenceWithInvalidDateAndWheelchairAccessibleFlag() {
    return VehicleDto.builder()
        .vrm("9AAAA99")
        .start("2019-01-01")
        .end("2019-02-01")
        .description("taxi")
        .licensingAuthorityName("la-name-1")
        .licensePlateNumber("plate-1")
        .wheelchairAccessibleVehicle("invalid")
        .build();
  }

  private VehicleDto createInvalidLicenceWithThreeAttributes() {
    String invalidStartDate = "2019-01-01-01";
    String invalidEndDate = "2019-02-01-01";
    String invalidVrm = "8AAAA99";
    return VehicleDto.builder()
        .vrm(invalidVrm)
        .start(invalidStartDate)
        .end(invalidEndDate)
        .description("taxi")
        .licensingAuthorityName("la-name-1")
        .licensePlateNumber("plate-1")
        .wheelchairAccessibleVehicle("true")
        .build();
  }

  private VehicleDto createInvalidLicenceWithTwoAttributes() {
    String invalidVrm = "AAAA99";
    String invalidStartDate = "2019-01-01-01";
    return VehicleDto.builder()
        .vrm(invalidVrm)
        .start(invalidStartDate)
        .end("2019-02-06")
        .description("phv")
        .licensingAuthorityName("la-name-1")
        .licensePlateNumber("plate-1")
        .wheelchairAccessibleVehicle("true")
        .build();
  }
}