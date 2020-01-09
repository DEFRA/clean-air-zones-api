package uk.gov.caz.vcc.domain.service.compliance;

import java.text.ParseException;

import org.springframework.stereotype.Service;

import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.VehicleType;
import uk.gov.caz.vcc.domain.service.FuelTypeService;
import uk.gov.caz.vcc.util.EuroStatusParser;
import uk.gov.caz.vcc.util.RomanNumeralConverter;

@Service
public class EuroStatusPresentComplianceService implements CazComplianceService {
  
  private final RomanNumeralConverter romanNumeralConverter;
  private FuelTypeService fuelTypeService;

  /**
   * Create an instance of {@link EuroStatusPresentComplianceService}.
   * @param romanNumeralConverter an instance of RomanNumeralConverter 
   * @param fuelTypeService an instance of FuelTypeService
   */
  public EuroStatusPresentComplianceService(RomanNumeralConverter romanNumeralConverter,
                              FuelTypeService fuelTypeService) {
    this.romanNumeralConverter = romanNumeralConverter;
    this.fuelTypeService = fuelTypeService;
  }

  /**
 * Method to determine the minimum euroStandard to be met in order to be
 * deemed compliant, given some fuel type.
 * 
 * @param fuelType String containing the fuel type for which the required
 *                 standard must be determined.
 * @return int determining the minimum standard for being compliant
 */
  private int requiredStandardByFuelType(String fuelType) {
    fuelType = this.fuelTypeService.getFuelType(fuelType);
    if (fuelType.equalsIgnoreCase(FuelTypeService.PETROL)) {
      return 4;
    } 
    return 6; // fuelType.equalsIgnoreCase(FuelTypeService.DIESEL)
  }

  /**
   * Method to determine the euroStandard to be met in order to be deemed
   * compliant, given some vehicle type.
   * 
   * @param vehicle Vehicle whose minimum euroStandard is to be determined.
   * @return int determining the minimum standard for being compliant
   */
  private int requiredStandardByVehicleType(Vehicle vehicle) {
    VehicleType type = vehicle.getVehicleType();
    String fuel = vehicle.getFuelType();

    if (type == VehicleType.MOTORCYCLE) {
      return 3;
    } else {
      return requiredStandardByFuelType(fuel);
    }
  }

  /**
   * Method to parse a euroStatus value and return an Integer, using which
   * inequality comparisons can be made.
   * 
   * @param euroStatusString String value to be parsed. For example, "Euro 3" or
   *                         "Euro IV"
   * @return int of representation of the euroStatus.
   */
  private int parseEuroStatus(String euroStatusString) {
    String parsedEuroStatus;
    
    try {
      parsedEuroStatus = EuroStatusParser.parse(euroStatusString);
    } catch (ParseException e) {
      throw new UnableToIdentifyVehicleComplianceException(e.getMessage());
    }

    if (romanNumeralConverter.matchesRomanNumeralRegex(parsedEuroStatus)) {
      return romanNumeralConverter.romanToArabic(parsedEuroStatus);
    } else {
      return Integer.parseInt(parsedEuroStatus);
    }
  }

  @Override
  public boolean isVehicleCompliance(Vehicle vehicle) {
    try {
      int euroStatus = parseEuroStatus(vehicle.getEuroStatus());
      int requiredStandardByVehicleType = requiredStandardByVehicleType(vehicle);
      return euroStatus >= requiredStandardByVehicleType;
    } catch (Exception ex) {
      throw new UnableToIdentifyVehicleComplianceException(ex.getMessage());
    }
  }
}