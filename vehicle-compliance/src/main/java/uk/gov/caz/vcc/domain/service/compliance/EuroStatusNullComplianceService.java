package uk.gov.caz.vcc.domain.service.compliance;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.BiPredicate;
import org.springframework.stereotype.Service;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.vcc.domain.exceptions.UnableToIdentifyVehicleComplianceException;
import uk.gov.caz.vcc.domain.service.FuelTypeService;

@Service
public class EuroStatusNullComplianceService implements CazComplianceService {

  private BiPredicate<Date,Date> isRegistrationDateAfter =
      (registrationDate, cutoverDate) -> !registrationDate.before(cutoverDate);
  private static final Date MOTORCYCLE_EMISSIONS_EURO_3;
  private static final Date EURO_4_LOW_GROSS_WEIGHT; 
  private static final Date EURO_4_HIGH_GROSS_WEIGHT;
  private static final Date EURO_6;
  private static final Date EURO_IV;
  private static final Date EURO_VI;
  private static final Date VAN_EURO_6_LOW_GROSS_WEIGHT;
  private static final Date VAN_EURO_6_HIGH_GROSS_WEIGHT;
  private FuelTypeService fuelTypeService;

  static {
    try {
      SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
      MOTORCYCLE_EMISSIONS_EURO_3 = dateFormatter.parse("2007-01-01");
      EURO_4_LOW_GROSS_WEIGHT = dateFormatter.parse("2006-01-01"); 
      EURO_4_HIGH_GROSS_WEIGHT = dateFormatter.parse("2007-01-01");
      EURO_6 = dateFormatter.parse("2015-09-01");
      EURO_IV = dateFormatter.parse("2006-01-01");
      EURO_VI = dateFormatter.parse("2013-12-31");
      VAN_EURO_6_LOW_GROSS_WEIGHT = dateFormatter.parse("2015-09-01");
      VAN_EURO_6_HIGH_GROSS_WEIGHT = dateFormatter.parse("2016-09-01");
    } catch (ParseException ex) {
      throw new IllegalStateException("Wrong setup");
    }
  }

  /**
   * Create an instance of {@link EuroStatusNullComplianceService}.
   * 
   * @param fuelTypeService FuelTypeService to be injected.
   */
  public EuroStatusNullComplianceService(FuelTypeService fuelTypeService) {
    this.fuelTypeService = fuelTypeService;
  }

  @Override
  public boolean isVehicleCompliant(Vehicle vehicle) {
    try {
      String fuelType = this.fuelTypeService.getFuelType(vehicle.getFuelType());
      Date vehicleRegistrationDate = vehicle.getDateOfFirstRegistration();
      Date checkedDate;

      switch (vehicle.getVehicleType()) {
        case PRIVATE_CAR:
        case MINIBUS:
          if (fuelType.equalsIgnoreCase(FuelTypeService.PETROL)) {
            checkedDate = EURO_4_HIGH_GROSS_WEIGHT;
            if (vehicle.getRevenueWeight() == null
                || vehicle.getRevenueWeight() <= 2500) {
              checkedDate = EURO_4_LOW_GROSS_WEIGHT;
            }
          } else { // diesel
            checkedDate = EURO_6;
          }
          break;
        case BUS:
        case COACH:
        case HGV:
          checkedDate = EURO_VI;
          if (fuelType.equalsIgnoreCase(FuelTypeService.PETROL)) {
            checkedDate = EURO_IV;
          } 
          break;
        case VAN:
          if (vehicle.getRevenueWeight() == null || vehicle.getRevenueWeight() <= 1330) {
            checkedDate = VAN_EURO_6_LOW_GROSS_WEIGHT;
            if (fuelType.equalsIgnoreCase(FuelTypeService.PETROL)) {
              checkedDate = EURO_4_LOW_GROSS_WEIGHT;
            }
          } else {
            checkedDate = VAN_EURO_6_HIGH_GROSS_WEIGHT;
            if (fuelType.equalsIgnoreCase(FuelTypeService.PETROL)) {
              checkedDate = EURO_4_HIGH_GROSS_WEIGHT;
            }
          }
          break;
        case MOTORCYCLE:
          checkedDate = MOTORCYCLE_EMISSIONS_EURO_3;
          break;
        default:
          checkedDate = new Date();
      }
      return isRegistrationDateAfter.test(vehicleRegistrationDate,checkedDate);
    } catch (Exception e) {
      throw new UnableToIdentifyVehicleComplianceException(e.getMessage());
    }
  }
}