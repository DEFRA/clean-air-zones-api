package uk.gov.caz.vcc.cucumber;

import static org.junit.Assert.assertTrue;

import io.cucumber.java.BeforeStep;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.caz.vcc.domain.service.FuelTypeService;
import uk.gov.caz.vcc.domain.service.TaxClassService;
import uk.gov.caz.vcc.domain.service.TypeApprovalService;
import uk.gov.caz.vcc.service.ExemptionService;
import uk.gov.caz.vcc.service.MilitaryVehicleService;
import uk.gov.caz.vcc.service.RetrofitService;


public class VehicleExemptionStepdefs {
  
  @Mock
  private MilitaryVehicleService militaryVehicleService;

  @Mock
  private RetrofitService retrofitService;
  
  @Autowired
  private FuelTypeService fuelTypeService;

  @Autowired
  private TaxClassService taxClassService;

  @Autowired
  private TypeApprovalService typeApprovalService;

  private ExemptionService exemptionService;

  private World world;

  public VehicleExemptionStepdefs(World world) {
    this.world = world;
  }

  @BeforeStep
  public void setup() {
    MockitoAnnotations.initMocks(this);

    Mockito
        .when(militaryVehicleService.isMilitaryVehicle(world.vehicle.getRegistrationNumber()))
        .thenReturn(true);

    Mockito
      .when(retrofitService.isRetrofitted(world.vehicle.getRegistrationNumber()))
      .thenReturn(true);
    
    exemptionService = new ExemptionService(militaryVehicleService, retrofitService, fuelTypeService, taxClassService, typeApprovalService);
  }

  @Given("that my vehicle belongs to the ([^\"]*) tax class")
  public void my_vehicle_belongs_to_tax_class(String taxClass) {
    world.vehicle.setTaxClass(taxClass);
  }

  @Given("that I have a vehicle on the ([^\"]*) whitelist")
  public void I_have_a_vehicle_on_a_whitelist(String whitelist) {
  }

  @Given("that my vehicle is wheelchair accessible")
  public void that_my_vehicle_is_wheelchair_accessible() {
    world.vehicle.setIsWav(true);

    world.vehicle.setFuelType("DIESEL"); // Set to avoid later NullPointer
  }

  @When("I check my vehicle for exemption")
  public void check_vehicle_for_exemption() {
    world.result = exemptionService.updateCalculationResult(world.vehicle,
        world.result);
  }

  @Then("my vehicle is delcared to be exempt")
  public void vehicle_declared_to_be_exempt() {
    assertTrue(world.result.getExempt());
  }
}