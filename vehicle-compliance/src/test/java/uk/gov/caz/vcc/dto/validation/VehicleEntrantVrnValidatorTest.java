package uk.gov.caz.vcc.dto.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.caz.vcc.dto.VehicleEntrantDto;
import uk.gov.caz.vcc.dto.validation.VehicleEntrantVrnValidator.VrnNonEmptyValidator;
import uk.gov.caz.vcc.dto.validation.VehicleEntrantVrnValidator.VrnNonNullValidator;
import uk.gov.caz.vcc.dto.validation.VehicleEntrantVrnValidator.VrnWithinSizeRangeValidator;

class VehicleEntrantVrnValidatorTest {
  
  @ParameterizedTest
  @MethodSource("testCases")
  void shouldTestVrnValidator(TestCase testCase) {
    VehicleEntrantDto vehicleEntrantDto = new VehicleEntrantDto(testCase.vrn,
        "timestamp that is not tested");

    assertThat(VehicleEntrantVrnValidator.INSTANCE.validate(vehicleEntrantDto)).matches(
        shouldPass(testCase), "Tested VRN: " + testCase.vrn
    );
  }
  
  @ParameterizedTest
  @MethodSource("testCasesForVehicleEntrantValidator")
  void shouldTestSpecialisedVrnValidator(TestCase testCase) {
    VehicleEntrantDto vehicleEntrantDto = new VehicleEntrantDto(testCase.vrn,
        "timestamp that is not tested");

    assertThat(VehicleEntrantVrnValidator.INSTANCE.validate(vehicleEntrantDto)).matches(
        shouldPass(testCase), "Tested VRN: " + testCase.vrn
    );
  } 
  

  @ParameterizedTest
  @MethodSource("testCases")
  void shouldTestForbiddenVrnValuesWithEachValidatorSeparately(TestCase testCase) {
    //when
    Optional<ValidationError> validationError = testCase.validator.validate(testCase.vrn, testCase.vrn);

    //then
    assertThat(validationError)
        .matches(e -> e.isPresent() == testCase.error, "Tested VRN: " + testCase.vrn);
  }
  
  @ParameterizedTest
  @MethodSource("testCasesTestingSpecializedVrnValidators")
  void shouldTestSpecialisedForbiddenVrnValuesWithEachValidatorSeparately(TestCase testCase) {
    //when
    Optional<ValidationError> validationError = testCase.validator.validate(testCase.vrn, testCase.vrn);

    //then
    assertThat(validationError)
        .matches(e -> e.isPresent() == testCase.error, "Tested VRN: " + testCase.vrn);
  }

  private Predicate<List<? extends ValidationError>> shouldPass(TestCase testCase) {
    return e -> testCase.error ? e.size() > 0 : e.size() == 0;
  }

  static Stream<TestCase> testCasesForVehicleEntrantValidator() {
    return Stream.of(
        TestCase.withVrn(" ").shouldReturnError(true),
        TestCase.withVrn(" valid      VR").shouldReturnError(false)
    );
  }

  static Stream<TestCase> testCasesTestingSpecializedVrnValidators() {
    return Stream.of(
        TestCase.withVrn(" ").shouldReturnError(false).forValidator(new VrnNonEmptyValidator()),
        TestCase.withVrn(" valid      VRN ").shouldReturnError(true)
            .forValidator(new VrnWithinSizeRangeValidator())
    );
  }

  static Stream<TestCase> testCases() {
    return Stream.of(
        TestCase.withVrn(null).shouldReturnError(true).forValidator(new VrnNonNullValidator()),
        TestCase.withVrn("7charss").shouldReturnError(false)
            .forValidator(new VrnWithinSizeRangeValidator()),
        TestCase.withVrn("8charsss").shouldReturnError(false)
            .forValidator(new VrnWithinSizeRangeValidator()),
        TestCase.withVrn("9charssss").shouldReturnError(false)
            .forValidator(new VrnWithinSizeRangeValidator()),
        TestCase.withVrn("15charsssssssss").shouldReturnError(false)
            .forValidator(new VrnWithinSizeRangeValidator()),
        TestCase.withVrn("16charssssssssss").shouldReturnError(true)
            .forValidator(new VrnWithinSizeRangeValidator()),
        TestCase.withVrn("1").shouldReturnError(true)
            .forValidator(new VrnWithinSizeRangeValidator()),
        TestCase.withVrn("ch").shouldReturnError(false)
            .forValidator(new VrnWithinSizeRangeValidator())
    );
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static class TestCase {

    private String vrn;
    private SingleFieldValidator<String> validator;
    private boolean error;

    public static TestCase withVrn(String vrn) {
      TestCase testCase = new TestCase();
      testCase.vrn = vrn;
      return testCase;
    }

    public TestCase shouldReturnError(boolean error) {
      this.error = error;
      return this;
    }

    public TestCase forValidator(SingleFieldValidator<String> validator) {
      this.validator = validator;
      return this;
    }
  }
}
