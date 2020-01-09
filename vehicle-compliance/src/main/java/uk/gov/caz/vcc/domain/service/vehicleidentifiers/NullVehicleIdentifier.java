package uk.gov.caz.vcc.domain.service.vehicleidentifiers;

import java.util.ArrayList;

import org.springframework.stereotype.Service;

import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.VehicleType;
import uk.gov.caz.vcc.domain.exceptions.UnidentifiableVehicleException;

/**
 * VehicleIdentifer class for vehicles with null typeApproval.
 * 
 * @author informed
 */
@Service
public class NullVehicleIdentifier extends VehicleIdentifier {

  private ArrayList<String> hgvTaxClasses = new ArrayList<>(11);
  private ArrayList<String> taxClassesRequiringBodyTypeCheck = new ArrayList<>(
      32);
  private ArrayList<String> motorcycleBodyTypes = new ArrayList<>(8);
  private ArrayList<String> agriculturalBodyTypes = new ArrayList<>(10);
  private ArrayList<String> coachBodyTypes = new ArrayList<>(4);
  private ArrayList<String> privateCarBodyTypes = new ArrayList<>(15);

  /**
   * Default public constructor for NullVehicleIdentifier. Populates the
   * necessary collections for checking vehicle type.
   */
  public NullVehicleIdentifier() {

    hgvTaxClasses.add("HGV".toLowerCase());
    hgvTaxClasses.add("TRAILER HGV".toLowerCase());
    hgvTaxClasses.add("PRIVATE HGV".toLowerCase());
    hgvTaxClasses.add("SPECIAL VEHICLE".toLowerCase());
    hgvTaxClasses.add("SPECIAL TYPES VEHICLES".toLowerCase());    
    hgvTaxClasses.add("SMALL ISLANDS".toLowerCase());
    hgvTaxClasses.add("HGV CT".toLowerCase());
    hgvTaxClasses.add("RECOVERY VEHICLE".toLowerCase());
    hgvTaxClasses.add("RP GENERAL HAULAGE".toLowerCase());
    hgvTaxClasses.add("RP SPECIAL TYPES".toLowerCase());
    hgvTaxClasses.add("RP HGV".toLowerCase());
    hgvTaxClasses.add("SPECIAL VEHICLE TRAILER".toLowerCase());

    taxClassesRequiringBodyTypeCheck.add("BICYCLE".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("PRIVATE/LIGHT GOODS (PLG)".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("CROWN VEHICLE".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("NOT LICENSED".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("EXEMPT (NO LICENSE)".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("EXEMPT (NIL LICENSE)".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("CONSULAR".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("DIPLOMATIC".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("PLG (Old)".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("DISABLED".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("ELECTRIC".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("LIMITED USE".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("POLICE".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("TRICYCLE".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("NHSV".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("AMBULANCE".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("MOWING MACHINE".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("FIRE SERVICE".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("FIRE ENGINE".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("GRITTING MACHINE".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("STEAM".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("LIFEBOAT HAULAGE".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("SNOW PLOUGH".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("VISITING FORCES".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("LIGHTHOUSE AUTHORITY".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("MINE RESCUE".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("DIGGING MACHINE".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("PERSONAL EXPORT PRIVATE".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("RP BUS".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("WORKS TRUCK".toLowerCase());
    taxClassesRequiringBodyTypeCheck.add("DIRECT EXPORT PRIVATE".toLowerCase());

    motorcycleBodyTypes.add("Tricycle".toLowerCase());
    motorcycleBodyTypes.add("Goods Tricycle".toLowerCase());
    motorcycleBodyTypes.add("Moped".toLowerCase());
    motorcycleBodyTypes.add("Scooter".toLowerCase());
    motorcycleBodyTypes.add("Scooter Combination".toLowerCase());
    motorcycleBodyTypes.add("Motorcycle".toLowerCase());
    motorcycleBodyTypes.add("M/C combination".toLowerCase());

    agriculturalBodyTypes.add("Tel Material Handler".toLowerCase());
    agriculturalBodyTypes.add("Agricultural Tractor".toLowerCase());
    agriculturalBodyTypes.add("Combine Harvester".toLowerCase());
    agriculturalBodyTypes.add("Root Crop Harvester".toLowerCase());
    agriculturalBodyTypes.add("Forage Harvester".toLowerCase());
    agriculturalBodyTypes.add("Windrower".toLowerCase());
    agriculturalBodyTypes.add("Sprayer".toLowerCase());
    agriculturalBodyTypes.add("Viner/Picker".toLowerCase());
    agriculturalBodyTypes.add("Agricultural Machine".toLowerCase());
    agriculturalBodyTypes.add("Mowing Machine".toLowerCase());

    coachBodyTypes.add("S/D Bus/Coach".toLowerCase());
    coachBodyTypes.add("D/D Bus/Coach".toLowerCase());
    coachBodyTypes.add("Standee Bus".toLowerCase());
    coachBodyTypes.add("H/D Bus/Coach".toLowerCase());

    privateCarBodyTypes.add("2 Door Saloon".toLowerCase());
    privateCarBodyTypes.add("4 Door Saloon".toLowerCase());
    privateCarBodyTypes.add("Saloon".toLowerCase());
    privateCarBodyTypes.add("Convertible".toLowerCase());
    privateCarBodyTypes.add("Coupe".toLowerCase());
    privateCarBodyTypes.add("Estate".toLowerCase());
    privateCarBodyTypes.add("Taxi".toLowerCase());
    privateCarBodyTypes.add("Hearse".toLowerCase());
    privateCarBodyTypes.add("Limousine".toLowerCase());
    privateCarBodyTypes.add("3 Door Hatchback".toLowerCase());
    privateCarBodyTypes.add("5 Door Hatchback".toLowerCase());
    privateCarBodyTypes.add("Sports".toLowerCase());
    privateCarBodyTypes.add("Pick-up".toLowerCase());
    privateCarBodyTypes.add("Light 4x4 Utility".toLowerCase());
    privateCarBodyTypes.add("Tourer".toLowerCase());
    privateCarBodyTypes.add("MPV".toLowerCase());
  }

  @Override
  public void identifyVehicle(Vehicle vehicle) {

    testNotNull(checkTaxClass, vehicle, "taxClass");

    if (vehicle.getTaxClass().equalsIgnoreCase("electric motorcycle")) {
      vehicle.setVehicleType(VehicleType.MOTORCYCLE);
    } else if (vehicle.getTaxClass().equalsIgnoreCase("euro lgv")
        || vehicle.getTaxClass().equalsIgnoreCase("light goods vehicle")) {
      vehicle.setVehicleType(VehicleType.SMALL_VAN);
    } else if (vehicle.getTaxClass().equalsIgnoreCase("bus")
        || vehicle.getTaxClass().equalsIgnoreCase("rp bus")) {
      checkMinibusBodyTypes(vehicle);
    } else if (hgvTaxClasses.contains(vehicle.getTaxClass())) {
      vehicle.setVehicleType(VehicleType.HGV);
    } else if (taxClassesRequiringBodyTypeCheck
        .contains(vehicle.getTaxClass())) {
      checkBodyTypes(vehicle);
    } else {
      throw new UnidentifiableVehicleException("taxClass not recognised.");
    }
  }

  private void checkMinibusBodyTypes(Vehicle vehicle) {

    testNotNull(checkBodyType, vehicle, "bodyType");

    if (vehicle.getBodyType().equalsIgnoreCase("minibus")) {
      vehicle.setVehicleType(VehicleType.MINIBUS);
    } else if (coachBodyTypes.contains(vehicle.getBodyType())) {
      vehicle.setVehicleType(VehicleType.BUS);
    } else {
      throw new UnidentifiableVehicleException("bodyType not recognised.");
    }
  }

  private void checkBodyTypes(Vehicle vehicle) {

    testNotNull(checkBodyType, vehicle, "bodyType");

    if (vehicle.getBodyType().equalsIgnoreCase("minibus")) {
      vehicle.setVehicleType(VehicleType.MINIBUS);
    } else if (motorcycleBodyTypes.contains(vehicle.getBodyType())) {
      vehicle.setVehicleType(VehicleType.MOTORCYCLE);
    } else if (agriculturalBodyTypes.contains(vehicle.getBodyType())) {
      vehicle.setVehicleType(VehicleType.AGRICULTURAL);
    } else if (coachBodyTypes.contains(vehicle.getBodyType())) {
      vehicle.setVehicleType(VehicleType.BUS);
    } else if (privateCarBodyTypes.contains(vehicle.getBodyType())) {
      vehicle.setVehicleType(VehicleType.PRIVATE_CAR);
    } else {
      throw new UnidentifiableVehicleException("bodyType not recognised.");
    }
  }

}
