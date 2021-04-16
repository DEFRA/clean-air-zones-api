package uk.gov.caz.vcc.domain.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.caz.vcc.annotation.IntegrationTest;

@IntegrationTest
public class TaxClassServiceTestIT {
  @Autowired
  private TaxClassService taxClassService;

  @ParameterizedTest
  @MethodSource("exemptedTaxClasses")
  void givenValidExemptedTaxClassesThenPositiveExemptionCheck(String taxClass) {
    assertTrue(taxClassService.isExemptTaxClass(taxClass));
  }

  private static Stream<Arguments> exemptedTaxClasses() {
    Collection<String> exemptedTaxClass = 
      Arrays.asList("electric motorcycle","electric","disabled passenger vehicle","historic vehicle","disabled",
                    "ELECTRIC MOTORCYCLE","ELECTRIC","DISABLED PASSENGER VEHICLE","HISTORIC VEHICLE","DISABLED");
    return exemptedTaxClass
            .stream()
            .map(taxClass -> Arguments.of(taxClass));
  }

  @ParameterizedTest
  @MethodSource("nonExemptedTaxClasses")
  void givenInvalidExemptedTaxClassesThenNegativeExemptionCheck(String taxClass) {
    assertFalse(taxClassService.isExemptTaxClass(taxClass));
  }

  private static Stream<Arguments> nonExemptedTaxClasses() {
    return Stream.of(Arguments.of(""),
                    Arguments.of("gas"),
                    Arguments.of("non-exempted-tax-class"));
  }
}