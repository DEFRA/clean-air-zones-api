package uk.gov.caz.whitelist.dto.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDto;
import uk.gov.caz.whitelist.model.ValidationError;

class ManufacturerValidatorTest {
  private ManufacturerValidator validator = new ManufacturerValidator();

  @ParameterizedTest
  @ValueSource(strings = {"", "ABCD", "50charactersIsHere-blablablablablablablablablblabb"})
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
    WhitelistedVehicleDto whitelistedVehicle = vehicle("51charactersIsHere-blablablablablablablablablblabba");

    //when
    List<ValidationError> validationErrors = validator.validate(whitelistedVehicle);

    //then
    assertThat(validationErrors).isNotEmpty();
    assertThat(validationErrors.get(0)).isEqualTo(ValidationError.valueError(
        "VRN",
        "Invalid length of Manufacturer field (actual length: 51, max allowed length: 50)."
    ));
  }

  private WhitelistedVehicleDto vehicle(String manufacturer) {
    return WhitelistedVehicleDto.builder()
        .manufacturer(Optional.ofNullable(manufacturer))
        .vrn("VRN")
        .build();
  }
}