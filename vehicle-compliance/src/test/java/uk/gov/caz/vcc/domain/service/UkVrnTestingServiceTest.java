package uk.gov.caz.vcc.domain.service;

import static org.junit.jupiter.api.Assertions.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

class UkVrnTestingServiceTest {

  @Test
  void nullVrnDeemedNonUk() {
    boolean ukVrnTestOutcome = UkVrnTestingService.isPotentialUkVrn(null);
    assertFalse(ukVrnTestOutcome);
  }

  @Test
  void emptyVrnDeemedNonUk() {
    boolean ukVrnTestOutcome = UkVrnTestingService.isPotentialUkVrn("");
    assertFalse(ukVrnTestOutcome);
  }
  
  @Test
  void vrnThatExceedsLengthDeemedNonUk() {
    String exampleVrnThatExceedsLength = RandomStringUtils.randomAlphanumeric(UkVrnTestingService.MAX_LENGTH + 1).toUpperCase();
    boolean ukVrnTestOutcome = UkVrnTestingService.isPotentialUkVrn(exampleVrnThatExceedsLength);
    assertFalse(ukVrnTestOutcome);
  }
  
  @Test
  void validUkVrnDeemedAsPotential() {
    String validVrn = "CU57ABC";
    boolean ukVrnTestOutcome = UkVrnTestingService.isPotentialUkVrn(validVrn);
    assertTrue(ukVrnTestOutcome);
  }
  
  @Test
  void validUkVrnWithWhitespaceDeemedAsPotential() {
    String validVrn = "CU57 ABC";
    boolean ukVrnTestOutcome = UkVrnTestingService.isPotentialUkVrn(validVrn);
    assertTrue(ukVrnTestOutcome);
  }
  
  @Test
  void shortVrnDeemedNonUk() {
    String validVrn = "1";
    boolean ukVrnTestOutcome = UkVrnTestingService.isPotentialUkVrn(validVrn);
    assertFalse(ukVrnTestOutcome);
  }
  
  @Test
  void invalidUkVrnDeemedNonUk() {
    String validVrn = "CU57222";
    boolean ukVrnTestOutcome = UkVrnTestingService.isPotentialUkVrn(validVrn);
    assertFalse(ukVrnTestOutcome);
  }
  
}
