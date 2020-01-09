package uk.gov.caz.taxiregister.dto.validation;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ValidationError;

class LicensePlateNumberValidatorTest {
  private static final String ANY_VRM = "ZC62OMB";

  private LicensePlateNumberValidator validator = new LicensePlateNumberValidator();

  @Nested
  class MandatoryField {

    @Nested
    class WithLineNumber {
      @Test
      public void shouldReturnMissingFieldErrorWhenPlateNumberIsNull() {
        // given
        int lineNumber = 14;
        String licencePlateNumber = null;
        VehicleDto licence = createLicenceWithLineNumber(licencePlateNumber, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(ANY_VRM, LicensePlateNumberValidator.MISSING_LICENCE_PLATE_NUMBER_MESSAGE, lineNumber)
        );
      }

      @Test
      public void shouldReturnMissingFieldErrorWhenPlateNumberIsEmpty() {
        // given
        int lineNumber = 14;
        String licencePlateNumber = "";
        VehicleDto licence = createLicenceWithLineNumber(licencePlateNumber, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(ANY_VRM, LicensePlateNumberValidator.MISSING_LICENCE_PLATE_NUMBER_MESSAGE, lineNumber)
        );
      }

    }

    @Nested
    class WithoutLineNumber {
      @Test
      public void shouldReturnMissingFieldErrorWhenPlateNumberIsNull() {
        // given
        String licencePlateNumber = null;
        VehicleDto licence = createLicence(licencePlateNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(ANY_VRM, LicensePlateNumberValidator.MISSING_LICENCE_PLATE_NUMBER_MESSAGE)
        );
      }

      @Test
      public void shouldReturnMissingFieldErrorWhenPlateNumberIsEmpty() {
        // given
        String licencePlateNumber = "";
        VehicleDto licence = createLicence(licencePlateNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(ANY_VRM, LicensePlateNumberValidator.MISSING_LICENCE_PLATE_NUMBER_MESSAGE)
        );
      }
    }
  }
  @Nested
  class Format {

    @Nested
    class WithLineNumber {
      @Test
      public void shouldReturnValueErrorWhenLicencePlateNumberIsInvalid() {
        // given
        String invalidLicencePlateNumber = "tooLongLicencePlateNumber";
        int lineNumber = 59;
        VehicleDto licence = createLicenceWithLineNumber(invalidLicencePlateNumber, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.valueError(
                ANY_VRM,
                LicensePlateNumberValidator.INVALID_PLATE_NUMBER_MESSAGE,
                lineNumber
            )
        );
      }
    }

    @Nested
    class WithoutLineNumber {
      @Test
      public void shouldReturnValueErrorWhenLicencePlateNumberIsInvalid() {
        // given
        String invalidLicencePlateNumber = "tooLongLicencePlateNumber";
        VehicleDto licence = createLicence(invalidLicencePlateNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.valueError(ANY_VRM,
                LicensePlateNumberValidator.INVALID_PLATE_NUMBER_MESSAGE)
        );
      }
    }
  }

  private VehicleDto createLicence(String licencePlateNumber) {
    return VehicleDto.builder()
        .vrm(ANY_VRM)
        .licensePlateNumber(licencePlateNumber)
        .build();
  }

  private VehicleDto createLicenceWithLineNumber(String licencePlateNumber, int lineNumber) {
    return VehicleDto.builder()
        .vrm(ANY_VRM)
        .licensePlateNumber(licencePlateNumber)
        .lineNumber(lineNumber)
        .build();
  }
}