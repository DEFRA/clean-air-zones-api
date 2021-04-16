package uk.gov.caz.cucumber;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.LocalDateTime;
import java.sql.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.caz.vcc.domain.GeneralWhitelistVehicle;
import uk.gov.caz.vcc.domain.RetrofittedVehicle;
import uk.gov.caz.vcc.repository.GeneralWhitelistRepository;
import uk.gov.caz.vcc.repository.RetrofitRepository;
import uk.gov.caz.vcc.service.ExemptionService;

public class VehicleExemptionStepdefs {

  @Autowired
  private RetrofitRepository retrofitRepository;

  @Autowired
  private GeneralWhitelistRepository generalWhitelistRepsitory;

  @Autowired
  private ExemptionService exemptionService;

  private World world;

  public VehicleExemptionStepdefs(World world) {
    this.world = world;
  }

  @After
  public void post() {
    retrofitRepository.deleteAll();
    generalWhitelistRepsitory.deleteAll();
  }

  @Given("that my vehicle belongs to the ([^\"]*) tax class")
  public void my_vehicle_belongs_to_tax_class(String taxClass) {
    world.vehicle.setTaxClass(taxClass);
  }

  @Given("I have a vehicle on the retrofit whitelist")
  public void I_have_a_vehicle_on_retrofit_whitelist() {
    RetrofittedVehicle retrofittedVehicle = new RetrofittedVehicle();
    retrofittedVehicle.setVrn(world.vehicle.getRegistrationNumber());
    retrofittedVehicle.setDateOfRetrofit(new Date(System.currentTimeMillis() - 1000L * 60L * 60L * 24L));
    retrofittedVehicle.setWhitelistDiscountCode("whitelistDiscountCode");

    retrofitRepository.save(retrofittedVehicle);
  }

  @Given("that I have an exempt vehicle on the general purpose whitelist")
  public void I_have_an_exempt_vehicle_on_general_purpose_whitelist() {
    GeneralWhitelistVehicle generalWhitelistVehicle =
        GeneralWhitelistVehicle.builder()
            .vrn(world.vehicle.getRegistrationNumber())
            .reasonUpdated("reasonUpdated")
            .uploaderId(UUID.randomUUID())
            .category("OTHER")
            .exempt(true)
            .compliant(false)
            .updateTimestamp(LocalDateTime.now())
            .build();

    generalWhitelistRepsitory.save(generalWhitelistVehicle);
  }

  @Given("that my vehicle is wheelchair accessible")
  public void that_my_vehicle_is_wheelchair_accessible() {
    world.vehicle.setIsWav(true);

    world.vehicle.setFuelType("DIESEL"); // Set to avoid later NullPointer
  }

  @When("I check my vehicle for exemption")
  public void check_vehicle_for_exemption() {
    world.result = this.exemptionService.updateCalculationResult(world.vehicle,
        world.result);
  }

  @Then("my vehicle is delcared to be exempt")
  public void vehicle_declared_to_be_exempt() {
    assertTrue(world.result.isExempt());
  }
  
  @Then("my vehicle is not declared to be exempt")
  public void vehicle_not_declared_to_be_exempt() {
    assertFalse(world.result.isExempt());
  }
}