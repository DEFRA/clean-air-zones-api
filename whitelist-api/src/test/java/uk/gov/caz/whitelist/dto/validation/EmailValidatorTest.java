package uk.gov.caz.whitelist.dto.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDto;
import uk.gov.caz.whitelist.model.ValidationError;

class EmailValidatorTest {

  private EmailValidator validator = new EmailValidator();

  @ParameterizedTest
  @ValueSource(strings = {"", "ABCD", "EmailWith50Charactersdsadsafdsafdasdsafdafasfdafsf"})
  public void shouldAcceptGivenValues(String value) {
    //given
    WhitelistedVehicleDto whitelistedVehicle = vehicle(value);

    //when
    List<ValidationError> validationErrors = validator.validate(whitelistedVehicle);

    //then
    assertThat(validationErrors).isEmpty();
  }

  @Test
  public void shouldAcceptNullValue() {
    //given
    WhitelistedVehicleDto whitelistedVehicle = vehicle(null);

    //when
    List<ValidationError> validationErrors = validator.validate(whitelistedVehicle);

    //then
    assertThat(validationErrors).isEmpty();
  }

  @Test
  public void shouldNotAcceptStringBiggerThanMax() {
    //given
    WhitelistedVehicleDto whitelistedVehicle = vehicle(
        "EmailWith51Charactersdsadsafdsafdasdsafdafasfdafsfs");

    //when
    List<ValidationError> validationErrors = validator.validate(whitelistedVehicle);

    //then
    assertThat(validationErrors).isNotEmpty();
    assertThat(validationErrors.get(0)).isEqualTo(ValidationError.valueError(
        "VRN",
        "Invalid length of email field (actual length: 51, max allowed length: 50)."
    ));
  }

  private WhitelistedVehicleDto vehicle(String email) {
    return WhitelistedVehicleDto.builder()
        .email(email)
        .vrn("VRN")
        .build();
  }
}