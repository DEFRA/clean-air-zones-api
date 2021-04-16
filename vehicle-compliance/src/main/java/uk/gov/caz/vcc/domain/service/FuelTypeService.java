package uk.gov.caz.vcc.domain.service;

import com.google.common.base.Strings;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.caz.definitions.exceptions.UnrecognisedFuelTypeException;

/**
 * Domain service for working with vehicle Fuel Types.
 *
 */
@Service
public class FuelTypeService {

  private static final String UNRECOGNISED_FUEL = "Unrecognised fuel type: ";

  public static final String PETROL = "PETROL";
  public static final String DIESEL = "DIESEL";

  private Set<String> exemptFuelTypes;
  private Set<String> petrolTypes;
  private Set<String> dieselTypes;

  /**
   * Helper method to check if a vehicle's fuel type is deemed exempt from charging.
   * 
   * @param fuelType the fuel type of the vehicle.
   * @return boolean indicator for whether the fuel type is deemed exempt.
   */
  public boolean isExemptFuelType(String fuelType) {
    if (fuelType == null || fuelType.isEmpty()) {
      return false;
    }

    return this.exemptFuelTypes.stream().anyMatch(fuelType::equalsIgnoreCase);
  }

  /**
   * Cast the fuel type of a vehicle to petrol or diesel.
   * 
   * @param type the type of fuel stored in the database.
   * @return either PETROL or DIESEL fuel types.
   * @throws NotFoundException If the fuel type cannot be converted throw NotFound.
   */
  public String getFuelType(String type) {
    if (Strings.isNullOrEmpty(type)) {
      throw new UnrecognisedFuelTypeException(String.format("%s%s", UNRECOGNISED_FUEL, "null"));
    }
    if (petrolTypes.contains(type.toLowerCase())) {
      return PETROL;
    } else if (dieselTypes.contains(type.toLowerCase())) {
      return DIESEL;
    } else {
      throw new UnrecognisedFuelTypeException(String.format("%s%s", UNRECOGNISED_FUEL, type));
    }
  }

  /**
   * Public constructor for the FuelTypeService.
   */
  public FuelTypeService(
      @Value("${application.exempt-fuel-types:steam,electricity,fuel cells,gas}")
          String[] exemptFuelTypes,
      @Value("${application.petrol-types:petrol,hybrid electric,gas bi-fuel,gas/petrol,petrol/gas}")
          String[] petrolTypes,
      @Value("${application.diesel-types:diesel,heavy oil,electric diesel,gas diesel}")
          String[] dieselTypes) {
    this.exemptFuelTypes = new HashSet<>(Arrays.asList(exemptFuelTypes));
    this.petrolTypes = new HashSet<>(Arrays.asList(petrolTypes));
    this.dieselTypes = new HashSet<>(Arrays.asList(dieselTypes));
  }
}
