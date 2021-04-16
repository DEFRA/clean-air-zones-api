package uk.gov.caz.whitelist.dto.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.caz.whitelist.dto.validation.ActionValidator.INVALID_STRING_FORMAT;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDto;
import uk.gov.caz.whitelist.model.ValidationError;

class ActionValidatorTest {
  private ActionValidator validator = new ActionValidator();

  @ParameterizedTest
  @ValueSource(strings = {"C", "D", "U", ""})
  public void shouldAcceptEmptyFieldValue(String action) {
    //given
    WhitelistedVehicleDto whitelistedVehicle = vehicle(action);

    //when
    List<ValidationError> validationErrors = validator.validate(whitelistedVehicle);

    //then
    assertThat(validationErrors).isEmpty();
  }

  @Test
  public void shouldAcceptNullFieldValue() {
    //given
    WhitelistedVehicleDto whitelistedVehicle = vehicle(null);

    //when
    List<ValidationError> validationErrors = validator.validate(whitelistedVehicle);

    //then
    assertThat(validationErrors).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"ą", "ź"})
  public void shouldNotAllowCharactersExcludedByRegex(String action) {
    //given
    WhitelistedVehicleDto vehicle = vehicle(action);

    //when
    List<ValidationError> validationErrors = validator.validate(vehicle);

    //then
    assertThat(validationErrors).isNotEmpty();
    assertThat(validationErrors.get(0))
        .isEqualTo(ValidationError.valueError("VRN", INVALID_STRING_FORMAT, 1));
  }

  @ParameterizedTest
  @ValueSource(strings = {"A", "CC", "DDD", "Z", "1", "9"})
  public void shouldNotAcceptInvalidCharacters(String action) {
    //given
    WhitelistedVehicleDto whitelistedVehicle = vehicle(action);

    //when
    List<ValidationError> validationErrors = validator.validate(whitelistedVehicle);

    //then
    assertThat(validationErrors).isNotEmpty();
    assertThat(validationErrors.get(0)).isEqualTo(ValidationError.valueError(
        "VRN",
        "Action field should be empty or contain one of: C, D, U", 1
    ));
  }

  private WhitelistedVehicleDto vehicle(String action) {
    return WhitelistedVehicleDto.builder()
        .action(action)
        .vrn("VRN")
        .lineNumber(1)
        .build();
  }
}