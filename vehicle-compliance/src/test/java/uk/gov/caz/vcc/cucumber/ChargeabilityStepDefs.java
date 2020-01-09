package uk.gov.caz.vcc.cucumber;

import static org.junit.Assert.assertEquals;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.UUID;

import uk.gov.caz.vcc.domain.TariffDetails;
import uk.gov.caz.vcc.domain.service.ChargeabilityService;

public class ChargeabilityStepDefs {
  
  private ChargeabilityService chargeabilityService;

  private World world;

  public ChargeabilityStepDefs(World world) {
    this.world = world;
    
    UUID leedsCazId = UUID.randomUUID();
    world.result.setCazIdentifier(leedsCazId);
    
    chargeabilityService = new ChargeabilityService(leedsCazId.toString());
  }
  
  @When("I check my vehicle for chargeability")
  public void check_for_chargeability() {
    TariffDetails tariff = new TariffDetails();
    tariff.setCazId(world.result.getCazIdentifier());

    world.result.setCharge(chargeabilityService.getCharge(world.vehicle, tariff));
  }
  
  @Then("my vehicle is delcared to be not chargeable")
  public void vehicle_decalared_not_chargeable() {
    assertEquals(0, world.result.getCharge(), 0.001);
  }
}