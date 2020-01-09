package uk.gov.caz.vcc.domain.service.compliance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.caz.vcc.annotation.IntegrationTest;
import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.VehicleType;
import uk.gov.caz.vcc.domain.service.FuelTypeService;

@IntegrationTest
public class EuroStatusPresentComplianceServiceTestIT {
  
  @Autowired
  private EuroStatusPresentComplianceService cazComplianceService;
 
  @ParameterizedTest
  @MethodSource("EuroCompliantVehicles")
  void givenEuroCompliantVehicleThenPositiveComplianceCheck(Vehicle vehicle) {
    assertTrue(cazComplianceService.isVehicleCompliance(vehicle));
  }

  private static Stream<Arguments> EuroCompliantVehicles() throws ParseException {
    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    Vehicle petrolEuro4Car = makeVehicle(VehicleType.PRIVATE_CAR, FuelTypeService.PETROL, "EURO 4", 0, dateFormatter.parse("2006-01-02"));
    Vehicle petrolEuro9Car = makeVehicle(VehicleType.PRIVATE_CAR, FuelTypeService.PETROL, "EURO 9", 0, dateFormatter.parse("2006-01-02"));
    Vehicle petrolEuroIVBus = makeVehicle(VehicleType.BUS, FuelTypeService.PETROL, "EURO IV", 0, dateFormatter.parse("2006-01-02"));
    Vehicle petrolEuroVIBus = makeVehicle(VehicleType.BUS, FuelTypeService.PETROL, "EURO VI", 0, dateFormatter.parse("2006-01-02"));
    Vehicle petrolEuroVIIIBus = makeVehicle(VehicleType.BUS, FuelTypeService.PETROL, "EURO VIII", 0, dateFormatter.parse("2006-01-02"));
    Vehicle petrolEuroIXBus = makeVehicle(VehicleType.BUS, FuelTypeService.PETROL, "EURO IX", 0, dateFormatter.parse("2006-01-02"));
    
    Vehicle dieselEuro6Car = makeVehicle(VehicleType.PRIVATE_CAR, FuelTypeService.DIESEL, "EURO 6", 0, dateFormatter.parse("2015-09-02"));
    Vehicle dieselEuro9Car = makeVehicle(VehicleType.PRIVATE_CAR, FuelTypeService.DIESEL, "EURO 9", 0, dateFormatter.parse("2015-09-02"));
    Vehicle dieselEuroVIHgv = makeVehicle(VehicleType.HGV, FuelTypeService.DIESEL, "EURO VI", 0, dateFormatter.parse("2015-09-02"));
    Vehicle dieselEuroVIIIHgv = makeVehicle(VehicleType.HGV, FuelTypeService.DIESEL, "EURO VIII", 0, dateFormatter.parse("2015-09-02"));
    Vehicle dieselEuroIXHgv = makeVehicle(VehicleType.HGV, FuelTypeService.DIESEL, "EURO IX", 0, dateFormatter.parse("2015-09-02"));

    Vehicle euro3MotorCycle = makeVehicle(VehicleType.MOTORCYCLE, FuelTypeService.PETROL, "EURO 3", 0, dateFormatter.parse("2007-01-02"));
    Vehicle euro9MotorCycle = makeVehicle(VehicleType.MOTORCYCLE, FuelTypeService.PETROL, "EURO 9", 0, dateFormatter.parse("2007-01-02"));
       
    return Stream.of(Arguments.of(petrolEuro4Car),
                      Arguments.of(petrolEuro9Car),
                      Arguments.of(petrolEuroIVBus),
                      Arguments.of(petrolEuroVIBus),
                      Arguments.of(petrolEuroVIIIBus),
                      Arguments.of(petrolEuroIXBus),
                      Arguments.of(dieselEuro6Car),
                      Arguments.of(dieselEuro9Car),
                      Arguments.of(dieselEuroVIHgv),
                      Arguments.of(dieselEuroVIIIHgv),
                      Arguments.of(dieselEuroIXHgv),
                      Arguments.of(euro3MotorCycle),
                      Arguments.of(euro9MotorCycle));
  }  

  @ParameterizedTest
  @MethodSource("EuroNonCompliantVehicles")
  void givenEuroNonCompliantVehicleThenNegativeComplianceCheck(Vehicle vehicle) {
    assertFalse(cazComplianceService.isVehicleCompliance(vehicle));
  }

  private static Stream<Arguments> EuroNonCompliantVehicles() throws ParseException {
    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    Vehicle petrolEuro0Car = makeVehicle(VehicleType.PRIVATE_CAR, FuelTypeService.PETROL, "EURO 0", 0, dateFormatter.parse("2006-01-02"));
    Vehicle petrolEuro3Car = makeVehicle(VehicleType.PRIVATE_CAR, FuelTypeService.PETROL, "EURO 3", 0, dateFormatter.parse("2006-01-02"));
    
    Vehicle dieselEuro0Car = makeVehicle(VehicleType.PRIVATE_CAR, FuelTypeService.DIESEL, "EURO 0", 0, dateFormatter.parse("2015-09-02"));
    Vehicle dieselEuro5CVar = makeVehicle(VehicleType.PRIVATE_CAR, FuelTypeService.DIESEL, "EURO 5", 0, dateFormatter.parse("2015-09-02"));
    Vehicle dieselEuroIVHgv = makeVehicle(VehicleType.HGV, FuelTypeService.DIESEL, "EURO IV", 0, dateFormatter.parse("2015-09-02"));
    Vehicle dieselEuroVHgv = makeVehicle(VehicleType.HGV, FuelTypeService.DIESEL, "EURO V", 0, dateFormatter.parse("2015-09-02"));
    

    Vehicle euro0MotorCycle = makeVehicle(VehicleType.MOTORCYCLE, FuelTypeService.PETROL, "EURO 0", 0, dateFormatter.parse("2007-01-02"));
    Vehicle euro2MotorCycle = makeVehicle(VehicleType.MOTORCYCLE, FuelTypeService.PETROL, "EURO 2", 0, dateFormatter.parse("2007-01-02"));

    return Stream.of(Arguments.of(petrolEuro0Car),
                      Arguments.of(petrolEuro3Car),
                      Arguments.of(dieselEuro0Car),
                      Arguments.of(dieselEuro5CVar),
                      Arguments.of(dieselEuroIVHgv),
                      Arguments.of(dieselEuroVHgv),
                      Arguments.of(euro0MotorCycle),
                      Arguments.of(euro2MotorCycle));
  }

  @ParameterizedTest
  @MethodSource("EuroUnableToIdentifyVehicleCompliance")
  void givenLeedsUnqualifiedVehicleInfoThenExceptionIsThrown(Vehicle vehicle) {
    assertThrows(UnableToIdentifyVehicleComplianceException.class,
        () -> cazComplianceService.isVehicleCompliance(vehicle));
  }

  private static Stream<Arguments> EuroUnableToIdentifyVehicleCompliance() throws ParseException {
    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    Vehicle unrecognisedFuelTypeCar = makeVehicle(VehicleType.PRIVATE_CAR, "NON-EXISTENCE-FUEL-TYPE", "EURO 0", 0,
        dateFormatter.parse("2006-01-02"));
    Vehicle unrecognisedEuroStatusCar = makeVehicle(VehicleType.PRIVATE_CAR, FuelTypeService.PETROL,
        "NON-EXISTENCE-EURO-STATUS", 0, dateFormatter.parse("2006-01-02"));

    return Stream.of(Arguments.of(unrecognisedFuelTypeCar), Arguments.of(unrecognisedEuroStatusCar));
  }

  private static Vehicle makeVehicle(VehicleType vehicleType,
                              String fuelType,
                              String euroStatus,
                              int revenueWeight,
                              Date dateOfFirstRegistration) {
    Vehicle vehicle = new Vehicle();
    vehicle.setEuroStatus(euroStatus);
    vehicle.setVehicleType(vehicleType);
    vehicle.setFuelType(fuelType);
    vehicle.setRevenueWeight(Integer.valueOf(revenueWeight));
    vehicle.setDateOfFirstRegistration(dateOfFirstRegistration);
    return vehicle;
  }
}