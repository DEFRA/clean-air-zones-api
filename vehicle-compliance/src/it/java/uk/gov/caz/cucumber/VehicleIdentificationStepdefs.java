package uk.gov.caz.cucumber;

import static org.junit.Assert.assertEquals;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.caz.definitions.domain.VehicleType;
import uk.gov.caz.vcc.domain.service.VehicleIdentificationService;
import uk.gov.caz.vcc.domain.service.vehicleidentifiers.LVehicleIdentifier;
import uk.gov.caz.vcc.domain.service.vehicleidentifiers.M1VehicleIdentifier;
import uk.gov.caz.vcc.domain.service.vehicleidentifiers.M2VehicleIdentifier;
import uk.gov.caz.vcc.domain.service.vehicleidentifiers.M3VehicleIdentifier;
import uk.gov.caz.vcc.domain.service.vehicleidentifiers.N1VehicleIdentifier;
import uk.gov.caz.vcc.domain.service.vehicleidentifiers.N2VehicleIdentifier;
import uk.gov.caz.vcc.domain.service.vehicleidentifiers.N3VehicleIdentifier;
import uk.gov.caz.vcc.util.UnidentifiableVehicleExceptionHandler;

public class VehicleIdentificationStepdefs {

  @Autowired
  VehicleIdentificationService identificationService;

  @Mock
  UnidentifiableVehicleExceptionHandler exceptionHandler;

  private World world;

  public VehicleIdentificationStepdefs(World world) {
    this.world = world;
  }

  @Given("that my vehicle has a type approval of L{int}")
  public void that_my_vehicle_has_a_type_approval_of_L(int typeApproval) {
    world.vehicle.setTypeApproval("L" + Integer.toString(typeApproval));
    world.identifier = new LVehicleIdentifier();
  }

  @Given("that my vehicle has a type approval of T{int}")
  public void that_my_vehicle_has_a_type_approval_of_T(int typeApproval) {
    world.vehicle.setTypeApproval("T" + Integer.toString(typeApproval));
  }

  @Given("that my vehicle has a type approval of M1")
  public void that_my_vehicle_has_a_type_approval_of_m1() {
    world.vehicle.setTypeApproval("M1");
    world.identifier = new M1VehicleIdentifier();
  }

  @Given("that my vehicle has a type approval of M2")
  public void that_my_vehicle_has_a_type_approval_of_m2() {
    world.vehicle.setTypeApproval("M2");
    world.identifier = new M2VehicleIdentifier();
  }

  @Given("that my vehicle has a type approval of M3")
  public void that_my_vehicle_has_a_type_approval_of_m3() {
    world.vehicle.setTypeApproval("M3");
    world.identifier = new M3VehicleIdentifier();
  }

  @Given("that my vehicle has a type approval of N1")
  public void that_my_vehicle_has_a_type_approval_of_n1() {
    world.vehicle.setTypeApproval("N1");
    world.identifier = new N1VehicleIdentifier();
  }

  @Given("that my vehicle has a type approval of N2")
  public void that_my_vehicle_has_a_type_approval_of_n2() {
    world.vehicle.setTypeApproval("N2");
    world.identifier = new N2VehicleIdentifier();
  }

  @Given("that my vehicle has a type approval of N3")
  public void that_my_vehicle_has_a_type_approval_of_n3() {
    world.vehicle.setTypeApproval("N3");
    world.identifier = new N3VehicleIdentifier();
  }

  @Given("that my vehicle has 8 passenger seats \\(in addition to the driver seat) or fewer")
  public void that_my_vehicle_has_8_passenger_seats_or_fewer() {
    world.vehicle.setSeatingCapacity(8);
  }

  @Given("that my vehicle is not on the taxi\\/PHV register")
  public void that_my_vehicle_is_not_on_the_taxi_PHV_register() {
    world.vehicle.setIsTaxiOrPhv(false);
  }

  @Then("if my vehicle is on the taxi\\/PHV register")
  public void if_my_vehicle_is_on_the_taxi_PHV_register() {
    world.vehicle.setIsTaxiOrPhv(true);
  }

  @Then("my vehicle is identified as a taxi\\/PHV")
  public void my_vehicle_is_identified_as_a_taxi_PHV() {
      // Write code here that turns the phrase above into concrete actions
      throw new cucumber.api.PendingException();
  }

  @When("I check my vehicle type")
  public void I_check_my_vehicle_type() {
    MockitoAnnotations.initMocks(this);

    identificationService.setVehicleType(world.vehicle);
  }

  @Then("my vehicle is identified as a car")
  public void my_vehicle_is_identified_as_a_car() {
    assertEquals(VehicleType.PRIVATE_CAR, world.vehicle.getVehicleType());
  }

  @Then("my vehicle is identified as a moped\\/motorbike")
  public void my_vehicle_is_identified_as_a_moped_motorbike() {
    assertEquals(VehicleType.MOTORCYCLE, world.vehicle.getVehicleType());
  }

  @Then("my vehicle is identified as an agricultural vehicle")
  public void my_vehicle_is_identified_as_an_agricultural_vehicle() {
    assertEquals(VehicleType.AGRICULTURAL, world.vehicle.getVehicleType());
  }

  @Then("my vehicle is identified as a HGV")
  public void my_vehicle_is_identified_as_a_HGV() {
    assertEquals(VehicleType.HGV, world.vehicle.getVehicleType());
  }
  
  @Then("my vehicle is identified as a bus")
  public void my_vehicle_is_identified_as_a_bus() {
    assertEquals(VehicleType.BUS, world.vehicle.getVehicleType());
  }

}