package uk.gov.caz.cucumber;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.caz.definitions.domain.VehicleType;
import uk.gov.caz.vcc.domain.service.ComplianceService;

public class VehicleComplianceStepdefs {
  private String bathCazIdentifier = "131af03c-f7f4-4aef-81ee-aae4f56dbeb5";
   
  @Autowired
  ComplianceService complianceService;
  
  private World world;

  public VehicleComplianceStepdefs(World world) {
    this.world = world;
  }

  @Given("that my vehicle has a gross weight less than or equal to {int} kg")
  public void that_my_vehicle_has_a_gross_weight_less_than_or_equal_to_5000_kg(
      int grossWeight) {
    world.vehicle.setRevenueWeight(grossWeight);
  }

  @Given("that my vehicle has a gross weight greater than {int} kg")
  public void that_my_vehicle_has_a_gross_weight_greater_than_5000_kg(
      int grossWeight) {
    world.vehicle.setRevenueWeight(grossWeight + 1);
  }

  @Given("that my vehicle's fuel type is petrol")
  public void that_my_vehicles_fuel_type_is_petrol() {
    world.vehicle.setFuelType("PETROL");
  }

  @Given("that my vehicle type is a ([^\"]*)$")
  public void that_my_vehicle_type_is_a(String vehicleType) {
    world.vehicle.setVehicleType(VehicleType.valueOf(vehicleType));
    if (world.vehicle.getVehicleType().equals(VehicleType.TAXI_OR_PHV)) {
      world.vehicle.setIsTaxiOrPhv(true);
    } else {
      world.vehicle.setIsTaxiOrPhv(false);
    }
    world.result.setCazIdentifier(
        UUID.fromString("131af03c-f7f4-4aef-81ee-aae4f56dbeb5"));
  }

  @Given("that my vehicle has a Euro status of ([^\"]*)$")
  public void that_my_vehicle_has_a_Euro_status_of_Euro_IV(String euroStatus) {
    world.vehicle.setEuroStatus(euroStatus);
  }

  @Given("that my vehicle has a fuel type of ([^\"]*)$")
  public void that_my_vehicle_has_a_fuel_type_of_X(String fuelType) {
    world.vehicle.setFuelType(fuelType);
  }

  @Given("that my vehicle does not have a Euro status")
  public void that_my_vehicle_does_not_have_a_euro_status() {
    world.vehicle.setEuroStatus(null);
  }

  @Given("that my vehicle has a null gross weight")
  public void that_my_vehicle_has_null_gross_weight() {
    world.vehicle.setRevenueWeight(null);
  }

  @Given("that my vehicle's date of first registration is ([^\"]*)$")
  public void that_date_of_registration_is(String firstRegistrationDateString) {
    LocalDate firstRegistrationLocalDate = LocalDate
        .parse(firstRegistrationDateString);

    Date dateOfFirstRegistrationDate = Date.from(firstRegistrationLocalDate
        .atStartOfDay(ZoneId.systemDefault()).toInstant());
    world.vehicle.setDateOfFirstRegistration(dateOfFirstRegistrationDate);
  }

  @Given("that my vehicle's date of first registration was before ([^\"]*)$")
  public void that_date_of_registration_before_X(
      String firstRegistrationDateString) {
    LocalDate firstRegistrationLocalDate = LocalDate
        .parse(firstRegistrationDateString);
    firstRegistrationLocalDate = firstRegistrationLocalDate.minusDays(1);

    Date dateOfFirstRegistrationDate = Date.from(firstRegistrationLocalDate
        .atStartOfDay(ZoneId.systemDefault()).toInstant());
    world.vehicle.setDateOfFirstRegistration(dateOfFirstRegistrationDate);
  }

  @Given("that my vehicle's date of first registration was on or after ([^\"]*)$")
  public void that_date_of_registration_on_or_later_than(
      String firstRegistrationDateString) {
    LocalDate firstRegistrationLocalDate = LocalDate
        .parse(firstRegistrationDateString);

    Date dateOfFirstRegistrationDate = Date.from(firstRegistrationLocalDate
        .atStartOfDay(ZoneId.systemDefault()).toInstant());
    world.vehicle.setDateOfFirstRegistration(dateOfFirstRegistrationDate);
  }

  @Given("that my vehicle is compliant")
  public void my_vehicle_is_compliant() {
    // set to the highest euro standard
    world.vehicle.setFuelType("petrol");
    world.vehicle.setEuroStatus("EURO 6");
  }
 
  @Given("that I have entered the BANES CAZ")
  public void i_have_entered_the_BANES_caz() {
    world.result.setCazIdentifier(UUID.fromString(bathCazIdentifier));
  }

  @Given("that my vehicle's fuel type is not ([^\"]*)$")
  public void fuel_type_is_not_hybrid_electric(String fuelType) {
    world.vehicle.setFuelType("DIESEL");
  }

  @When("I check my vehicle's compliance")
  public void when_I_check_my_vehicle_compliace() {
    world.result = complianceService.updateCalculationResult(world.vehicle,
        world.result);
  }

  @Then("my vehicle is not compliant")
  public void my_vehicle_compliance_is() {
    assertFalse(world.result.getCompliant());
  }

  @Then("I do not meet the ([^\"]*) standard")
  public void I_do_not_meet_the_required_standard(String requiredStandard) {
    assertFalse(world.result.getCompliant());
  }

  @Then("I meet the ([^\"]*) standard")
  public void I_meet_the_required_standard(String requiredStandard) {
    assertTrue(world.result.getCompliant());
  }

  @Then("I am declared to be compliant")
  public void I_am_declared_compliant() {
    assertTrue(world.result.getCompliant());
  }

  @Then("I am declared to be non-compliant")
  public void I_am_declared_non_nompliant() {
    assertFalse(world.result.getCompliant());
  }

}
