package uk.gov.caz.accounts.dto.validation;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.caz.accounts.dto.AccountVehicleDto;
import uk.gov.caz.accounts.model.registerjob.ValidationError;

class VrnValidatorTest {

  private VrnValidator validator = new VrnValidator();

  @Nested
  class Format {

    @Nested
    class WithLineNumber {

      @ParameterizedTest
      @ValueSource(strings = {"", "tooLongVrn12345"})
      public void shouldReturnValueErrorWhenVrnIsBlankOrTooLong(String vrn) {
        // given
        int lineNumber = 87;
        AccountVehicleDto vehicle = createAccountVehicleWithLineNumber(vrn, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(vehicle);

        // then
        then(validationErrors).contains(
            ValidationError.valueError(
                vrn,
                String.format(
                    VrnValidator.INVALID_LENGTH_MESSAGE_TEMPLATE,
                    VrnValidator.MIN_LENGTH,
                    VrnValidator.MAX_LENGTH,
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
        AccountVehicleDto accountVehicleDto = createAccountVehicle(vrn);

        // when
        List<ValidationError> validationErrors = validator.validate(accountVehicleDto);

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
      AccountVehicleDto accountVehicleDto = createAccountVehicle(validVrn);

      // when
      List<ValidationError> validationErrors = validator.validate(accountVehicleDto);

      // then
      then(validationErrors).isEmpty();
    }
  }

  private AccountVehicleDto createAccountVehicle(String vrn) {
    return AccountVehicleDto.builder()
        .vrn(vrn)
        .build();
  }

  private AccountVehicleDto createAccountVehicleWithLineNumber(String vrn, int lineNumber) {
    return AccountVehicleDto.builder()
        .vrn(vrn)
        .lineNumber(lineNumber)
        .build();
  }
}