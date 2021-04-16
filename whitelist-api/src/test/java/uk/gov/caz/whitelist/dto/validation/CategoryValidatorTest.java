package uk.gov.caz.whitelist.dto.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.caz.whitelist.dto.validation.CategoryValidator.ALLOWABLE_CATEGORIES;
import static uk.gov.caz.whitelist.dto.validation.CategoryValidator.CATEGORY_FIELD_ERROR_MESSAGE;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDto;
import uk.gov.caz.whitelist.model.ValidationError;

class CategoryValidatorTest {

  private CategoryValidator validator = new CategoryValidator();

  @ParameterizedTest
  @ValueSource(strings = {"Early Adopter", "Non-UK Vehicle", "Problematic VRN", "Exemption",
      "Other"})
  public void shouldAcceptEmptyFieldValue(String category) {
    //given
    WhitelistedVehicleDto whitelistedVehicle = vehicle(category);

    //when
    List<ValidationError> validationErrors = validator.validate(whitelistedVehicle);

    //then
    assertThat(validationErrors).isEmpty();
  }

  @Test
  public void shouldNotAllowNullFieldValue() {
    //given
    WhitelistedVehicleDto whitelistedVehicle = vehicle(null);

    //when
    List<ValidationError> validationErrors = validator.validate(whitelistedVehicle);

    //then
    assertThat(validationErrors).isNotEmpty();
    assertThat(validationErrors.get(0))
        .isEqualTo(ValidationError.valueError("VRN",
            CATEGORY_FIELD_ERROR_MESSAGE + String.join(", ", ALLOWABLE_CATEGORIES), 1));
  }

  @ParameterizedTest
  @ValueSource(strings = {"ą", "ź", "", "any"})
  public void shouldNotAllowCharactersExcludedByRegex(String action) {
    //given
    WhitelistedVehicleDto vehicle = vehicle(action);

    //when
    List<ValidationError> validationErrors = validator.validate(vehicle);

    //then
    assertThat(validationErrors).isNotEmpty();
    assertThat(validationErrors.get(0))
        .isEqualTo(ValidationError.valueError("VRN",
            CATEGORY_FIELD_ERROR_MESSAGE + String.join(", ", ALLOWABLE_CATEGORIES),1));
  }

  private WhitelistedVehicleDto vehicle(String category) {
    return WhitelistedVehicleDto.builder()
        .category(category)
        .vrn("VRN")
        .lineNumber(1)
        .build();
  }
}