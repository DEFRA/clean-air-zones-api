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
public class TypeApprovalServiceTestIT {
  @Autowired
  private TypeApprovalService typeApprovalService;

  @ParameterizedTest
  @MethodSource("exemptionTypeApproval")
  void givenValidTypesThenPositiveExemptionCheck(String type) {
    assertTrue(typeApprovalService.isExemptTypeApproval(type));
  }

  private static Stream<Arguments> exemptionTypeApproval() {
    Collection<String> exemptTypesApproval = Arrays.asList("T1","T2","T3","T4","T5",
                                                           "t1","t2","t3","t4","t5");
    return exemptTypesApproval
            .stream()
            .map(fuelType -> Arguments.of(fuelType));
  }

  @ParameterizedTest
  @MethodSource("nonExemptedTypes")
  void givenInvalidTypesThenNegativeExemptionCheck(String type) {
    assertFalse(typeApprovalService.isExemptTypeApproval(type));
  }

  private static Stream<Arguments> nonExemptedTypes() {
    Collection<String> nonExemptedTypes = Arrays.asList("","non-exempted");
    return nonExemptedTypes
            .stream()
            .map(fuelType -> Arguments.of(fuelType));
  }
}