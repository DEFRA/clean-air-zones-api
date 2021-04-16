package uk.gov.caz.vcc.domain.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.caz.definitions.exceptions.UnrecognisedFuelTypeException;
import uk.gov.caz.vcc.annotation.IntegrationTest;

@IntegrationTest
public class FuelTypeServiceTestIT {

  @Autowired
  private FuelTypeService fuelTypeService;

  @ParameterizedTest
  @MethodSource("exemptionFuelType")
  void givenValidFuelThenPositiveExemptionCheck(String fuelType) {
    assertTrue(fuelTypeService.isExemptFuelType(fuelType));
  }

  private static Stream<Arguments> exemptionFuelType() {
    Collection<String> exemptFuelTypes = Arrays.asList("steam","electricity","fuel cells","gas",
                                                       "STEAM","ELECTRICITY","FUEL CELLS","GAS");
    return exemptFuelTypes
            .stream()
            .map(fuelType -> Arguments.of(fuelType));
  }

  @ParameterizedTest
  @MethodSource("nonExemptionFuelType")
  void givenInValidFuelThenNegativeExemptionCheck(String fuelType) {
    assertFalse(fuelTypeService.isExemptFuelType(fuelType));
  }

  private static Stream<Arguments> nonExemptionFuelType() {
    return Stream.of(Arguments.of(""),
                    Arguments.of("non-exemption-fuel-type"));
  }


  @ParameterizedTest
  @MethodSource("validFuelTypes")
  void givenValidFuelsThenGetFuelTypesDoesNotThrowException(String fuelType) {
    assertDoesNotThrow(() -> fuelTypeService.getFuelType(fuelType));
  }

  private static Stream<Arguments> validFuelTypes() {
    Collection<String> fuelTypes = Arrays.asList("petrol", "hybrid electric", "gas bi-fuel",
      "gas/petrol","diesel", "heavy oil", "electric diesel", "gas diesel");
    return fuelTypes
            .stream()
            .map(fuelType -> Arguments.of(fuelType));
  }

  @ParameterizedTest
  @MethodSource("invalidFuelTypes")
  void givenInvalidFuelsThenGetFuelTypesDoesThrowException(String fuelType) {
    assertThrows(UnrecognisedFuelTypeException.class, () -> fuelTypeService.getFuelType(fuelType));
  }
  
  private static Stream<Arguments> invalidFuelTypes() {
    Collection<String> fuelTypes = Arrays.asList("", "invalid-fuel-type");
    return fuelTypes
            .stream()
            .map(fuelType -> Arguments.of(fuelType));
  }
}