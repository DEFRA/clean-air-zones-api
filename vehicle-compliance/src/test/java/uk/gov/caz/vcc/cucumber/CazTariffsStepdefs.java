package uk.gov.caz.vcc.cucumber;

import static org.junit.Assert.assertTrue;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;

import uk.gov.caz.vcc.domain.CazFrameworkClassAStrategy;
import uk.gov.caz.vcc.domain.CazFrameworkClassBStrategy;
import uk.gov.caz.vcc.domain.CazFrameworkClassCStrategy;
import uk.gov.caz.vcc.domain.CazFrameworkClassDStrategy;
import uk.gov.caz.vcc.domain.VehicleType;

public class CazTariffsStepdefs {

  World world;

  public CazTariffsStepdefs(World world) {
    this.world = world;
  }

  @Given("that my vehicle is a ([^\"]*)$")
  public void that_my_vehicle_is_a_X(String type) {
    world.vehicle.setVehicleType(VehicleType.valueOf(type));

    if (VehicleType.valueOf(type).equals(VehicleType.TAXI_OR_PHV)) {
      world.vehicle.setIsTaxiOrPhv(true);
    } else {
      world.vehicle.setIsTaxiOrPhv(false);
    }
  }

  @When("I check if my vehicle is chargeable in zone ([^\"]*)$")
  public void check_if_my_vehcile_is_chargeable(String zone) {
    switch (zone) {
    case "A":
      new CazFrameworkClassAStrategy().execute(world.vehicle, world.result);

    case "B":
      new CazFrameworkClassBStrategy().execute(world.vehicle, world.result);

    case "C":
      new CazFrameworkClassCStrategy().execute(world.vehicle, world.result);

    case "D":
      new CazFrameworkClassDStrategy().execute(world.vehicle, world.result);
    }
  }

  @Then("I am declared to be chargeable")
  public void I_am_declared_to_be_chargeable_in_zones_implementing_tariffs() {
    assertTrue(world.result.getChargeable());
  }
}
