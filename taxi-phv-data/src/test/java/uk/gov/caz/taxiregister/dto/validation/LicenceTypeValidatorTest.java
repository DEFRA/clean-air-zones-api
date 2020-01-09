package uk.gov.caz.taxiregister.dto.validation;

import static org.assertj.core.api.BDDAssertions.then;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ValidationError;

class LicenceTypeValidatorTest {

  private static final String ANY_VRM = "ZC62OMB";

  private LicenceTypeValidator validator = new LicenceTypeValidator();

  @Nested
  class MandatoryField {

    @Nested
    class WithoutLineNumber {

      @ParameterizedTest
      @MethodSource("uk.gov.caz.taxiregister.dto.validation.LicenceTypeValidatorTest#licenceTypes")
      public void shouldReturnMissingFieldErrorWhenLicenceTypeIsNullOrEmpty(
          String licenceType) {
        // given
        VehicleDto licence = createLicence(licenceType);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors)
            .containsExactly(ValidationError.missingFieldError(ANY_VRM,
                LicenceTypeValidator.MISSING_LICENCE_TYPE_MESSAGE));
      }
    }
  }

  @Nested
  class WithLineNumber {

    @ParameterizedTest
    @MethodSource("uk.gov.caz.taxiregister.dto.validation.LicenceTypeValidatorTest#licenceTypes")
    public void shouldReturnMissingFieldErrorWhenLicenceTypeIsNullOrEmpty(
        String licenceType) {
      // given
      int lineNumber = 45;
      VehicleDto licence = createLicenceWithLineNumber(licenceType, lineNumber);

      // when
      List<ValidationError> validationErrors = validator.validate(licence);

      // then
      then(validationErrors)
          .containsExactly(ValidationError.missingFieldError(ANY_VRM,
              LicenceTypeValidator.MISSING_LICENCE_TYPE_MESSAGE, lineNumber));
    }
    
    @Test
    @MethodSource("uk.gov.caz.taxiregister.dto.validation.LicenceTypeValidatorTest#licenceTypes")
    public void shouldReturnErrorWhenLicenceTypeExceeds100Characters() {
      // given
      
      // Create license with string that exceeds length of permitted value
      VehicleDto licence = createLicence(RandomStringUtils.randomAlphabetic(LicenceTypeValidator.MAX_LENGTH + 1));

      // when
      List<ValidationError> validationErrors = validator.validate(licence);

      // then
      then(validationErrors).containsExactly(
          ValidationError
              .valueError(ANY_VRM, LicenceTypeValidator.LICENCE_TYPE_TOO_LONG_MESSAGE)
      );
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"taxi", "PHV", "other"})
  public void shouldReturnEmptyListWhenLicenceTypeIsValid(String licenceType) {
    // given
    VehicleDto licence = createLicence(licenceType);

    // when
    List<ValidationError> validationErrors = validator.validate(licence);

    // then
    then(validationErrors).isEmpty();
  }

  private VehicleDto createLicence(String licenceType) {
    return VehicleDto.builder().vrm(ANY_VRM).description(licenceType).build();
  }

  private VehicleDto createLicenceWithLineNumber(String licenceType,
      int lineNumber) {
    return VehicleDto.builder().vrm(ANY_VRM).description(licenceType)
        .lineNumber(lineNumber).build();
  }

  static Stream<String> licenceTypes() {
    return Stream.of("", null);
  }
}
