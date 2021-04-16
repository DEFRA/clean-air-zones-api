package uk.gov.caz.taxiregister.dto.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.caz.taxiregister.dto.validation.WheelchairAccessibleVehicleValidator.INVALID_BOOLEAN_VALUE_MESSAGE;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ValidationError;

@ExtendWith(MockitoExtension.class)
class WheelchairAccessibleVehicleValidatorTest {

  private static final String ANY_VRM = "ZC62OMB";

  private WheelchairAccessibleVehicleValidator validator =
      new WheelchairAccessibleVehicleValidator();

  @Test
  public void shouldAcceptNullValue() {
    // given
    VehicleDto licence = createLicenceWithWheelchairFlagSetTo(null);

    // when
    List<ValidationError> result = validator.validate(licence);

    // then
    assertThat(result).isEmpty();
  }

  @Test
  public void shouldAcceptEmptyValue() {
    // given
    VehicleDto licence = createLicenceWithWheelchairFlagSetTo("");

    // when
    List<ValidationError> result = validator.validate(licence);

    // then
    assertThat(result).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"true", "false", "TRUE", "FALSE", "TrUe", "falSE"})
  public void shouldAcceptValidValues(String flag) {
    // given
    VehicleDto licence = createLicenceWithWheelchairFlagSetTo(flag);

    // when
    List<ValidationError> result = validator.validate(licence);

    // then
    assertThat(result).isEmpty();
  }

  @Nested
  class WithoutLineNumber {

    @ParameterizedTest
    @ValueSource(strings = {"TrUEE", "FaLSe1", "something", "a", "b"})
    public void shouldRejectInvalidValues(String flag) {
      // given
      VehicleDto licence = createLicenceWithWheelchairFlagSetTo(flag);

      // when
      List<ValidationError> result = validator.validate(licence);

      // then
      assertThat(result).hasOnlyOneElementSatisfying(validationError -> {
        assertThat(validationError.getDetail()).isEqualTo(INVALID_BOOLEAN_VALUE_MESSAGE);
        assertThat(validationError.getLineNumber()).isEmpty();
      });
    }
  }

  @Nested
  class WithLineNumber {

    @ParameterizedTest
    @ValueSource(strings = {"TrUEe", "FLSe", "something", "a", "b"})
    public void shouldRejectInvalidValues(String flag) {
      // given
      int lineNumber = 18;
      VehicleDto licence = createLicenceWithLineNumber(flag, lineNumber);

      // when
      List<ValidationError> result = validator.validate(licence);

      // then
      assertThat(result).hasOnlyOneElementSatisfying(validationError -> {
        assertThat(validationError.getDetail()).isEqualTo("Line " + lineNumber
            + ": " + INVALID_BOOLEAN_VALUE_MESSAGE);
        assertThat(validationError.getLineNumber()).contains(lineNumber);
      });
    }
  }

  private VehicleDto createLicenceWithWheelchairFlagSetTo(String wheelchairAccessible) {
    return VehicleDto.builder()
        .vrm(ANY_VRM)
        .wheelchairAccessibleVehicle(wheelchairAccessible)
        .build();
  }

  private VehicleDto createLicenceWithLineNumber(String wheelchairAccessible, int lineNumber) {
    return VehicleDto.builder()
        .vrm(ANY_VRM)
        .wheelchairAccessibleVehicle(wheelchairAccessible)
        .lineNumber(lineNumber)
        .build();
  }
}