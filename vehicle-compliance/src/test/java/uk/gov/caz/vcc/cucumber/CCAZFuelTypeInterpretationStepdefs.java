package uk.gov.caz.vcc.cucumber;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;

import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import uk.gov.caz.vcc.domain.VehicleType;
import uk.gov.caz.vcc.domain.service.ComplianceService;
import uk.gov.caz.vcc.domain.service.FuelTypeService;
import uk.gov.caz.vcc.domain.service.compliance.UnableToIdentifyVehicleComplianceException;
import uk.gov.caz.vcc.service.ExemptionService;

public class CCAZFuelTypeInterpretationStepdefs {
  
  private World world;
  private String rawFuelType;
  private String result;

  @Autowired
  private ExemptionService exemptionService;
  
  @Autowired
  private FuelTypeService fuelTypeService;

  @Autowired
  private ComplianceService complianceService;

  public CCAZFuelTypeInterpretationStepdefs(World world) {
    this.world = world;
  }

  @Given("^that my vehicle fuel type is ([^\"]*)$")
  public void that_my_vehicle_fuel_type_is(String rawFuelType) {
    this.rawFuelType = rawFuelType;
    this.world.vehicle.setFuelType(rawFuelType);
  }

  @When("I check my vehicle's fuel type")
  public void I_check_my_vehicles_fuel_type() {
    this.result = this.fuelTypeService.getFuelType(this.rawFuelType);
  }

  @When("I check my vehicle for exemption based on fuel type")
  public void check_vehicle_for_exemption_on_fuel_type() {
    MockitoAnnotations.initMocks(this);

    world.vehicle.setFuelType(this.rawFuelType);

    exemptionService.updateCalculationResult(world.vehicle, world.result);
  }

  @Then("^my vehicle is considered as a (PETROL|DIESEL) vehicle$")
  public void my_vehicle_is_considered_as_a_petrol_or_diesel_vehicle(
      String correctedFuelType) {
    assertTrue(this.result.equals(correctedFuelType));
  }

  @Then("my vehicle is delcared to be exempt based on fuel type")
  public void vehicle_declared_to_be_exempt() {
    assertTrue(world.result.getExempt());
  }

  @Then("an UnsupportedOperationException is thrown when I check my vehicle's compliance")
  public void an_unsupported_operation_exception_is_thrown_when_I_check_my_vehicle_compliance() {
    assertThrows(UnableToIdentifyVehicleComplianceException.class, () -> {
      complianceService.updateCalculationResult(world.vehicle, world.result);
    });
  }

  @Given("that my vehicle type is PRIVATE_CAR  # Arbitrary - added to avoid NullPointerException")
  public void that_my_vehicle_type_is_PRIVATE_CAR_Arbitrary_added_to_avoid_NullPointerException() {
    world.vehicle.setVehicleType(VehicleType.PRIVATE_CAR);
    world.vehicle.setIsTaxiOrPhv(false); 
    world.result.setCazIdentifier(UUID.randomUUID());  // To avoid NullPointerError when checking compliance.

    String testUuid = "39e54ed8-3ed2-441d-ffd5-38fc9b7048d3";
    ReflectionTestUtils.setField(complianceService, "leedsCazIdentifier",
        testUuid);
  }
}
