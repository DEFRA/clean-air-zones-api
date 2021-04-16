package uk.gov.caz.whitelist.dto.validation;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDto;
import uk.gov.caz.whitelist.model.ValidationError;

class VrnValidatorTest {
  private VrnValidator validator = new VrnValidator();

  @Nested
  class MandatoryField {

    @Nested
    class WithLineNumber {
      @Test
      public void shouldReturnMissingFieldErrorWhenVrnIsNull() {
        // given
        int lineNumber = 91;
        String vrn = null;
        WhitelistedVehicleDto retrofittedVehicle = createRetrofittedVehicleWithLineNumber(vrn, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(retrofittedVehicle);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(
                vrn,
                VrnValidator.MISSING_VRN_MESSAGE,
                lineNumber
            )
        );
      }
    }

    @Nested
    class WithoutLineNumber {
      @Test
      public void shouldReturnMissingFieldErrorWhenVrnIsNull() {
        // given
        String vrn = null;
        WhitelistedVehicleDto retrofittedVehicle = createRetrofittedVehicle(vrn);

        // when
        List<ValidationError> validationErrors = validator.validate(retrofittedVehicle);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(
                vrn,
                VrnValidator.MISSING_VRN_MESSAGE
            )
        );
      }
    }
  }

  @Nested
  class Format {

    @Nested
    class WithLineNumber {
      @ParameterizedTest
      @ValueSource(strings = {"", "tooLongVrn12345"})
      public void shouldReturnValueErrorWhenVrnIsBlankOrTooLong(String vrn) {
        // given
        int lineNumber = 87;
        WhitelistedVehicleDto vehicle = createRetrofittedVehicleWithLineNumber(vrn, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(vehicle);

        // then
        then(validationErrors).contains(
            ValidationError.valueError(
                vrn,
                String.format(
                    VrnValidator.INVALID_LENGTH_MESSAGE_TEMPLATE,
                    2,
                    14,
                    vrn.length()
                ),
                lineNumber
            )
        );
      }
    }

    @Nested
    class WithoutLineNumber {
      @ParameterizedTest
      @ValueSource(strings = {"", "tooLongVrn123456"})
      public void shouldReturnValueErrorWhenVrnIsBlankOrTooLong(String vrn) {
        // given
        WhitelistedVehicleDto retrofittedVehicle = createRetrofittedVehicle(vrn);

        // when
        List<ValidationError> validationErrors = validator.validate(retrofittedVehicle);

        // then
        then(validationErrors).contains(
            ValidationError.valueError(
                vrn,
                String.format(
                    VrnValidator.INVALID_LENGTH_MESSAGE_TEMPLATE,
                    VrnValidator.MIN_LENGTH,
                    VrnValidator.MAX_LENGTH,
                    vrn.length()
                )
            )
        );
      }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "A9", "A99", "A999", "A9999", "AA9", "AA99", "AA999", "AA9999", "AAA9", "AAA99", "AAA999",
        "AAA9999", "AAA9A", "AAA99A", "AAA999A", "9A", "9AA", "9AAA", "99A", "99AA", "99AAA",
        "999A", "999AA", "999AAA", "9999A", "9999AA", "A9AAA", "A99AAA", "A999AAA", "AA99AAA",
        "9999AAA", "ABC123", "A123BCD", "GAD975C", "ZEA1436", "SK12JKL", "7429HER", "G5", "6W",
        "JK4", "P91", "9RA", "81U", "KAT7", "Y478", "LK31", "8RAD", "87KJ", "111Z", "A7CUD",
        "VAR7A", "FES23", "PG227", "30JFA", "868BO", "1289J", "B8659", "K97LUK", "MAN07U", "546BAR",
        "JU0043", "8839GF"
    })
    public void shouldAcceptValidVrn(String validVrn) {
      // given
      WhitelistedVehicleDto retrofittedVehicle = createRetrofittedVehicle(validVrn);

      // when
      List<ValidationError> validationErrors = validator.validate(retrofittedVehicle);

      // then
      then(validationErrors).isEmpty();
    }
  }

  private WhitelistedVehicleDto createRetrofittedVehicle(String vrn) {
    return WhitelistedVehicleDto.builder()
        .vrn(vrn)
        .build();
  }

  private WhitelistedVehicleDto createRetrofittedVehicleWithLineNumber(String vrn, int lineNumber) {
    return WhitelistedVehicleDto.builder()
        .vrn(vrn)
        .lineNumber(lineNumber)
        .build();
  }
}