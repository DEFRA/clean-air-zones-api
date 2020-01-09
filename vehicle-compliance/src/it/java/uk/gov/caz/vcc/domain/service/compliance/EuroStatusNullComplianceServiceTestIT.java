package uk.gov.caz.vcc.domain.service.compliance;

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
public class EuroStatusNullComplianceServiceTestIT {

  @Autowired
  private EuroStatusNullComplianceService cazComplianceService;

  @ParameterizedTest
  @MethodSource("NonEuroCompliantVehicles")
  void givenNonEuroCompliantVehicleThenPositiveComplianceCheck(Vehicle vehicle) {
    assertTrue(cazComplianceService.isVehicleCompliance(vehicle));
  }

  private static Stream<Arguments> NonEuroCompliantVehicles() throws ParseException {
    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    Vehicle privatePetrolCar = makeVehicle(VehicleType.PRIVATE_CAR, FuelTypeService.PETROL, null, 0, dateFormatter.parse("2006-01-02"));
    Vehicle privatePetrol2500Car = makeVehicle(VehicleType.PRIVATE_CAR, FuelTypeService.PETROL, null, 2500, dateFormatter.parse("2006-01-02"));
    Vehicle privatePetrol2500PlusCar = makeVehicle(VehicleType.PRIVATE_CAR, FuelTypeService.PETROL, null, 2501, dateFormatter.parse("2007-01-02"));
    Vehicle privateDieselCar = makeVehicle(VehicleType.PRIVATE_CAR, FuelTypeService.DIESEL, null, 0, dateFormatter.parse("2015-09-02"));

    Vehicle privatePetrolMiniBus = makeVehicle(VehicleType.MINIBUS, FuelTypeService.PETROL, null, 0, dateFormatter.parse("2006-01-02"));
    Vehicle privatePetrol2500MiniBus = makeVehicle(VehicleType.MINIBUS, FuelTypeService.PETROL, null, 2500, dateFormatter.parse("2006-01-02"));
    Vehicle privatePetrol2500PlusMiniBus = makeVehicle(VehicleType.MINIBUS, FuelTypeService.PETROL, null, 2501, dateFormatter.parse("2007-01-02"));
    Vehicle privateDieselMiniBus = makeVehicle(VehicleType.MINIBUS, FuelTypeService.DIESEL, null, 0, dateFormatter.parse("2015-09-02"));

    Vehicle privatePetrolBus = makeVehicle(VehicleType.BUS, FuelTypeService.PETROL, null, 0, dateFormatter.parse("2006-01-02"));
    Vehicle privateDieselBus = makeVehicle(VehicleType.BUS, FuelTypeService.DIESEL, null, 0, dateFormatter.parse("2014-01-01"));

    Vehicle privatePetrolCoach = makeVehicle(VehicleType.COACH, FuelTypeService.PETROL, null, 0, dateFormatter.parse("2006-01-02"));
    Vehicle privateDieselCoach = makeVehicle(VehicleType.COACH, FuelTypeService.DIESEL, null, 0, dateFormatter.parse("2014-01-01"));

    Vehicle privatePetrolHgv = makeVehicle(VehicleType.HGV, FuelTypeService.PETROL, null, 0, dateFormatter.parse("2006-01-02"));
    Vehicle privateDieselHgv = makeVehicle(VehicleType.HGV, FuelTypeService.DIESEL, null, 0, dateFormatter.parse("2014-01-01"));

    Vehicle privatePetrolSmallVan = makeVehicle(VehicleType.SMALL_VAN, FuelTypeService.PETROL, null, 0, dateFormatter.parse("2006-01-02"));
    Vehicle privateDieselSmallVan = makeVehicle(VehicleType.SMALL_VAN, FuelTypeService.DIESEL, null, 0, dateFormatter.parse("2015-09-02"));

    Vehicle privatePetrolLargeVan = makeVehicle(VehicleType.LARGE_VAN, FuelTypeService.PETROL, null, 0, dateFormatter.parse("2007-01-02"));
    Vehicle privateDieselLargeVan = makeVehicle(VehicleType.LARGE_VAN, FuelTypeService.DIESEL, null, 0, dateFormatter.parse("2016-09-02"));

    Vehicle privateMotorCycle = makeVehicle(VehicleType.MOTORCYCLE, FuelTypeService.PETROL, null, 0, dateFormatter.parse("2007-01-02"));
    
    return Stream.of(Arguments.of(privatePetrolCar),
                      Arguments.of(privatePetrol2500Car),
                      Arguments.of(privatePetrol2500PlusCar),
                      Arguments.of(privateDieselCar),
                      Arguments.of(privatePetrolMiniBus),
                      Arguments.of(privatePetrol2500MiniBus),
                      Arguments.of(privatePetrol2500PlusMiniBus),
                      Arguments.of(privateDieselMiniBus),
                      Arguments.of(privatePetrolBus),
                      Arguments.of(privateDieselBus),
                      Arguments.of(privatePetrolCoach),
                      Arguments.of(privateDieselCoach),
                      Arguments.of(privatePetrolHgv),
                      Arguments.of(privateDieselHgv),
                      Arguments.of(privatePetrolSmallVan),
                      Arguments.of(privateDieselSmallVan),
                      Arguments.of(privatePetrolLargeVan),
                      Arguments.of(privateDieselLargeVan),
                      Arguments.of(privateMotorCycle));
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