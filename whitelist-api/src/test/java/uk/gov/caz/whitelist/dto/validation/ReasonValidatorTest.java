package uk.gov.caz.whitelist.dto.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.caz.whitelist.dto.validation.ReasonValidator.INVALID_STRING_FORMAT;
import static uk.gov.caz.whitelist.dto.validation.ReasonValidator.MAX_LENGTH;
import static uk.gov.caz.whitelist.dto.validation.ReasonValidator.MISSING_REASON_MESSAGE;
import static uk.gov.caz.whitelist.dto.validation.ReasonValidator.invalidLengthMessage;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleDto;
import uk.gov.caz.whitelist.model.ValidationError;

class ReasonValidatorTest {

  private static final String VRN = "VRN";

  private ReasonValidator validator = new ReasonValidator();

  @Test
  public void shouldNotAllowNullReason() {
    //given
    WhitelistedVehicleDto vehicle = vehicle(null);

    //when
    List<ValidationError> validationErrors = validator.validate(vehicle);

    //then
    assertThat(validationErrors).isNotEmpty();
    assertThat(validationErrors.get(0))
        .isEqualTo(ValidationError.valueError("VRN", MISSING_REASON_MESSAGE, 1));
  }

  @Test
  public void shouldNotAllowBlankReason() {
    //given
    WhitelistedVehicleDto vehicle = vehicle("");

    //when
    List<ValidationError> validationErrors = validator.validate(vehicle);

    //then
    assertThat(validationErrors).isNotEmpty();
    assertThat(validationErrors.get(0))
        .isEqualTo(ValidationError.valueError("VRN", MISSING_REASON_MESSAGE, 1));
  }

  @ParameterizedTest
  @ValueSource(strings = {"ą", "ź"})
  public void shouldNotAllowCharactersExcludedByRegex(String reason) {
    //given
    WhitelistedVehicleDto vehicle = vehicle(reason);

    //when
    List<ValidationError> validationErrors = validator.validate(vehicle);

    //then
    assertThat(validationErrors).isNotEmpty();
    assertThat(validationErrors.get(0))
        .isEqualTo(ValidationError.valueError("VRN", INVALID_STRING_FORMAT, 1));
  }

  @Test
  public void shouldNotAllowLongReason() {
    //given
    String tooLongReason = IntStream.range(0, MAX_LENGTH).mapToObj(Integer::toString).collect(
        Collectors.joining());
    WhitelistedVehicleDto vehicle = vehicle(tooLongReason);

    //when
    List<ValidationError> validationErrors = validator.validate(vehicle);

    //then
    assertThat(validationErrors).isNotEmpty();
    assertThat(validationErrors.get(0))
        .isEqualTo(ValidationError.valueError("VRN", invalidLengthMessage(tooLongReason), 1));
  }

  @Test
  public void shouldAllowReasonWithSpaces() {
    //given
    WhitelistedVehicleDto vehicle = vehicle("THIS IS REASON WITH SPACE");

    //when
    List<ValidationError> validationErrors = validator.validate(vehicle);

    //then
    assertThat(validationErrors).isEmpty();
  }

  private WhitelistedVehicleDto vehicle(String reason) {
    return WhitelistedVehicleDto.builder()
        .reason(reason)
        .vrn(VRN)
        .lineNumber(1)
        .build();
  }
}