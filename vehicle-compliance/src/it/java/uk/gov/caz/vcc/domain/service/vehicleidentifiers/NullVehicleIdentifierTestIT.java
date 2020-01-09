package uk.gov.caz.vcc.domain.service.vehicleidentifiers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.caz.vcc.annotation.IntegrationTest;
import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.VehicleType;

@IntegrationTest
public class NullVehicleIdentifierTestIT {

  @Autowired
  private NullVehicleIdentifier vehicleIdentifier;
  
  @ParameterizedTest
  @MethodSource("validVehicleBodyTypes")
  void givenValidVehicleBodyTypeThenVehicleTypeAreIdentified(String bodyType, String taxClass, VehicleType vehicleType) {
    Vehicle vehicle = new Vehicle();
    vehicle.setTaxClass(taxClass);
    vehicle.setBodyType(bodyType);
    vehicleIdentifier.identifyVehicle(vehicle);
    assertEquals(vehicle.getVehicleType(), vehicleType);
  }

  private static Stream<Arguments> validVehicleBodyTypes(){
    List<String> taxClassesRequiringBodyTypeCheck = Arrays.asList("bicycle", "private/light goods (plg)",
                    "crown vehicle", "not licensed", "exempt (no license)", "exempt (nil license)",
                    "consular", "diplomatic", "plg (old)", "disabled", "electric", "limited use",
                    "police", "tricycle", "nhsv", "ambulance", "mowing machine", "fire service",
                    "fire engine", "gritting machine", "steam", "lifeboat haulage", "snow plough",
                    "visiting forces", "lighthouse authority", "mine rescue", "digging machine",
                    "personal export private", "works truck", "direct export private");
    List<String> miniBusBodyTypes = Arrays.asList("minibus");
    List<String> motorcycleBodyTypes = Arrays.asList("tricycle", "goods tricycle", "moped", "scooter",
                    "scooter combination", "motorcycle", "m/c combination");
    List<String> agriculturalBodyTypes = Arrays.asList("tel material handler", "agricultural tractor",
                    "combine harvester", "root crop harvester", "forage harvester", "windrower",
                    "sprayer", "viner/picker", "agricultural machine", "mowing machine");
    List<String> coachBodyTypes = Arrays.asList("s/d bus/coach", "d/d bus/coach", "standee bus", "h/d bus/coach");
    List<String> privateCarBodyTypes = Arrays.asList("2 door saloon", "4 door saloon", "saloon",
                    "convertible", "coupe", "estate", "taxi", "hearse", "limousine", "3 door hatchback",
                    "5 door hatchback", "sports", "pick-up", "light 4x4 utility", "tourer", "mpv");

    return taxClassesRequiringBodyTypeCheck
      .stream()
      .flatMap(taxClass -> {
        Stream<Arguments> minuBuses = miniBusBodyTypes
                                .stream()
                                .map(bodyType -> Arguments.of(bodyType, taxClass,VehicleType.MINIBUS));
        Stream<Arguments> motorcyles = motorcycleBodyTypes
                                .stream()
                                .map(bodyType -> Arguments.of(bodyType, taxClass,VehicleType.MOTORCYCLE));
        Stream<Arguments> agriculturalVehicles = agriculturalBodyTypes
                                .stream()
                                .map(bodyType -> Arguments.of(bodyType, taxClass,VehicleType.AGRICULTURAL));
        Stream<Arguments> buses = coachBodyTypes
                                .stream()
                                .map(bodyType -> Arguments.of(bodyType, taxClass,VehicleType.BUS));                                
        Stream<Arguments> privateVehicles = privateCarBodyTypes
                                .stream()
                                .map(bodyType -> Arguments.of(bodyType, taxClass,VehicleType.PRIVATE_CAR));    
        
        return Stream.concat(Stream.concat(Stream.concat(Stream.concat(minuBuses, motorcyles),agriculturalVehicles),buses),privateVehicles);
      });
  }
}