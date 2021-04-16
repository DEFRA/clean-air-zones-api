package uk.gov.caz.vcc.dto.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.caz.vcc.dto.VehicleEntrantDto;
import uk.gov.caz.vcc.dto.validation.VehicleEntrantTimestampValidatorV2.TimestampNotNullValidator;
import uk.gov.caz.vcc.dto.validation.VehicleEntrantTimestampValidatorV2.TimestampValidFormatValidator;

class VehicleEntrantTimestampValidatorV2Test {

  static Stream<TestCase> testCases() {
    return Stream.of(
        TestCase
            .withTimestamp(null)
            .withVrn("PO4CK71")
            .shouldReturnError(true)
            .forValidator(new TimestampNotNullValidator()),
        TestCase
            .withTimestamp("")
            .withVrn("PO4CK71")
            .shouldReturnError(true)
            .forValidator(new TimestampValidFormatValidator()),
        TestCase
            .withTimestamp(" ")
            .withVrn("PO4CK71")
            .shouldReturnError(true)
            .forValidator(new TimestampValidFormatValidator()),
        TestCase
            .withTimestamp("2017-10-01T15:53:01Z")
            .withVrn("PO4CK71")
            .shouldReturnError(false)
            .forValidator(new TimestampValidFormatValidator())
    );
  }

  @ParameterizedTest
  @MethodSource("testCases")
  public void shouldTestTimestampValidator(TestCase testCase) {
    VehicleEntrantDto vehicleEntrantDto = new VehicleEntrantDto(testCase.vrn, testCase.timestamp);

    assertThat(VehicleEntrantTimestampValidatorV2.INSTANCE.validate(vehicleEntrantDto))
        .matches(shouldPass(testCase), "Tested timestamp: " + testCase.timestamp);
  }

  @ParameterizedTest
  @MethodSource("testCases")
  public void shouldTestForbiddenTimestampValues(TestCase testCase) {
    // when
    Optional<ValidationError> validationError = testCase.validator
        .validate(testCase.vrn, testCase.timestamp);

    assertThat(validationError)
        .matches(e -> e.isPresent() == testCase.error, "Tested timestamp: " + testCase.timestamp);
  }

  private Predicate<List<? extends ValidationError>> shouldPass(TestCase testCase) {
    return e -> testCase.error ? e.size() > 0 : e.size() == 0;
  }

  @AllArgsConstructor
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static class TestCase {

    private String timestamp;
    private SingleFieldValidator<String> validator;
    private boolean error;
    private String vrn;

    static TestCase withTimestamp(String timestamp) {
      TestCase testCase = new TestCase();
      testCase.timestamp = timestamp;
      return testCase;
    }

    TestCase withVrn(String vrn) {
      this.vrn = vrn;
      return this;
    }

    TestCase shouldReturnError(boolean error) {
      this.error = error;
      return this;
    }

    TestCase forValidator(SingleFieldValidator<String> validator) {
      this.validator = validator;
      return this;
    }
  }
}