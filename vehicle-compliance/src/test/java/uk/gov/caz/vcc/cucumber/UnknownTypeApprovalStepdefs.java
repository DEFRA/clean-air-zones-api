package uk.gov.caz.vcc.cucumber;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.VehicleType;
import uk.gov.caz.vcc.domain.service.VehicleIdentificationService;
import uk.gov.caz.vcc.util.UnidentifiableVehicleExceptionHandler;

public class UnknownTypeApprovalStepdefs {

  @InjectMocks
  VehicleIdentificationService identificationService;

  @Mock
  UnidentifiableVehicleExceptionHandler exceptionHandler;

  private World world;

  public UnknownTypeApprovalStepdefs(World world) {
    this.world = world;
  }

  @Given("that my vehicle has an unknown type approval")
  public void vehicle_has_unknown_type_approval() {
    world.vehicle.setTypeApproval(null);
  }

  @Given("that my vehicle has a tax class of ([^\"]*)")
  public void vehicle_has_tax_class(String taxClass) {
    world.vehicle.setTaxClass(taxClass);
  }

  @Given("that my vehicle has a body type of ([^\"]*)")
  public void vehicle_has_body_type(String bodyType) {
    world.vehicle.setBodyType(bodyType);
  }

  @Then("my vehicle is checked for body type when I check my vehicle type")
  public void vehicle_checked_for_body_type() {
    MockitoAnnotations.initMocks(this);

    Vehicle mockedVehicle = mock(Vehicle.class);
    when(mockedVehicle.getTaxClass()).thenReturn(world.vehicle.getTaxClass());

    identificationService.setVehicleType(mockedVehicle);

    verify(mockedVehicle, times(1)).getBodyType();
  }

  @Then("my vehicle is identified as a agricultural")
  public void identified_as_being_agricultural() {
    assertEquals(VehicleType.AGRICULTURAL, world.vehicle.getVehicleType());
  }

  @Then("Then my vehicle is identified as a motorcycle/moped")
  public void identified_as_a_motorcycle_or_moped() {
    assertEquals(VehicleType.MOTORCYCLE, world.vehicle.getVehicleType());
  }

  @Then("my vehicle is identified as a motorcycle")
  public void identified_as_a_motorcycle() {
    assertEquals(VehicleType.MOTORCYCLE, world.vehicle.getVehicleType());
  }
}
