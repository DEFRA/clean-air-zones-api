package uk.gov.caz.taxiregister.dto.validation;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ValidationError;

class LicensingAuthorityNameValidatorTest {
  private static final String ANY_VRM = "ZC62OMB";

  private LicensingAuthorityNameValidator validator = new LicensingAuthorityNameValidator();

  @Nested
  class MandatoryField {
    @Nested
    class WithoutLineNumber {
      @Test
      public void shouldReturnMissingFieldErrorWhenNameIsNull() {
        // given
        String licensingAuthorityName = null;
        VehicleDto licence = createLicence(licensingAuthorityName);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(
                ANY_VRM,
                LicensingAuthorityNameValidator.MISSING_LICENCING_AUTHORITY_NAME_MESSAGE
            )
        );
      }

      @Test
      public void shouldReturnMissingFieldErrorWhenNameIsEmpty() {
        // given
        String licensingAuthorityName = "";
        VehicleDto licence = createLicence(licensingAuthorityName);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(
                ANY_VRM,
                LicensingAuthorityNameValidator.MISSING_LICENCING_AUTHORITY_NAME_MESSAGE
            )
        );
      }
    }

    @Nested
    class WithLineNumber {
      @Test
      public void shouldReturnMissingFieldErrorWhenNameIsNull() {
        // given
        int lineNumber = 63;
        String licensingAuthorityName = null;
        VehicleDto licence = createLicenceWithLineNumber(licensingAuthorityName, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(ANY_VRM, LicensingAuthorityNameValidator.MISSING_LICENCING_AUTHORITY_NAME_MESSAGE, lineNumber)
        );
      }

      @Test
      public void shouldReturnMissingFieldErrorWhenNameIsEmpty() {
        // given
        int lineNumber = 63;
        String licensingAuthorityName = null;
        VehicleDto licence = createLicenceWithLineNumber(licensingAuthorityName, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(ANY_VRM, LicensingAuthorityNameValidator.MISSING_LICENCING_AUTHORITY_NAME_MESSAGE, lineNumber)
        );
      }
    }
  }

  @Nested
  class Format {
    @Nested
    class WithoutLineNumber {
      @Test
      public void shouldReturnValueErrorWhenNameIsInvalid() {
        // given
        String invalidLicensingAuthorityName = "tooooooooooooLooooooooooooongLicensingAuthorityName";
        VehicleDto licence = createLicence(invalidLicensingAuthorityName);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.valueError(
                ANY_VRM,
                LicensingAuthorityNameValidator.INVALID_LICENCING_AUTHORITY_NAME_MESSAGE
            )
        );
      }
    }

    @Nested
    class WithLineNumber {
      @Test
      public void shouldReturnValueErrorWhenNameIsInvalid() {
        // given
        String invalidLicensingAuthorityName = "tooooooooooooLooooooooooooongLicensingAuthorityName";
        int lineNumber = 90;
        VehicleDto licence = createLicenceWithLineNumber(invalidLicensingAuthorityName, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.valueError(
                ANY_VRM,
                LicensingAuthorityNameValidator.INVALID_LICENCING_AUTHORITY_NAME_MESSAGE,
                lineNumber
            )
        );
      }
    }
  }

  private VehicleDto createLicence(String licensingAuthorityNameValidator) {
    return VehicleDto.builder()
        .vrm(ANY_VRM)
        .licensingAuthorityName(licensingAuthorityNameValidator)
        .build();
  }

  private VehicleDto createLicenceWithLineNumber(String licensingAuthorityNameValidator, int lineNumber) {
    return VehicleDto.builder()
        .vrm(ANY_VRM)
        .licensingAuthorityName(licensingAuthorityNameValidator)
        .lineNumber(lineNumber)
        .build();
  }
}