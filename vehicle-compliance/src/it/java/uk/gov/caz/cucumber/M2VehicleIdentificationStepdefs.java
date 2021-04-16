package uk.gov.caz.cucumber;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import org.junit.jupiter.api.Assertions;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.caz.definitions.exceptions.UnidentifiableVehicleException;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.definitions.domain.VehicleType;
import uk.gov.caz.vcc.domain.service.VehicleIdentificationService;
import uk.gov.caz.vcc.util.UnidentifiableVehicleExceptionHandler;

public class M2VehicleIdentificationStepdefs {

  @Autowired
  VehicleIdentificationService identificationService;

  @Mock
  UnidentifiableVehicleExceptionHandler exceptionHandler;

  private World world;

  public M2VehicleIdentificationStepdefs(World world) {
    this.world = world;
  }

  @Given("that my vehicle has a revenue weight greater than or equal to {int} kg")
  public void that_my_vehicle_has_a_revenue_weight_greater_than_or_equal_to_x_kg(
      int revenueWeight) {
    world.vehicle.setRevenueWeight(revenueWeight);
  }

  @Given("that my vehicle has a revenue weight less than {int} kg")
  public void that_my_vehicle_has_a_revenue_weight_less_than_x_kg(int revenueWeight) {
    world.vehicle.setRevenueWeight(revenueWeight - 1);
  }

  @Given("that my vehicle has a revenue weight greater than {int} kg")
  public void that_my_vehicle_has_a_revenue_weight_greater_than_x_kg(int revenueWeight) {
    world.vehicle.setRevenueWeight(revenueWeight + 1);
  }

  @Given("that my vehicle has a revenue weight less than or equal to {int} kg")
  public void that_my_vehicle_has_a_revenue_weight_less_than_or_equal_to_x_kg(
      int revenueWeight) {
    world.vehicle.setRevenueWeight(revenueWeight);
  }

  @Given("that my vehicle has a seating capacity less than or equal to {int} seats")
  public void that_my_vehicle_seating_capacity_less_than_x_seats(int seats) {
    world.vehicle.setSeatingCapacity(seats);
  }

  @Given("that my vehicle has a seating capacity greater than or equal to 10 seats")
  public void that_my_vehicle_seating_capacity_greater_than_or_equal_to_10_seats() {
    world.vehicle.setSeatingCapacity(10);
  }

  @Given("that my vehicle has a mass in service less than {int} kg")
  public void my_vehicle_mass_in_service_less_than(int setMassInService) {
    world.vehicle.setMassInService(setMassInService - 1);
  }

  @Given("that my vehicle has a mass in service greater than {int} kg")
  public void my_vehicle_mass_in_servce_greater_than(int setMassInService) {
    world.vehicle.setMassInService(setMassInService + 1);
  }

  @Given("that my vehicle has a mass in service less than or equal to {int} kg")
  public void my_vehicle_mass_in_service_less_than_or_equal_to(int setMassInService) {
    world.vehicle.setMassInService(setMassInService);
  }

  @Given("that my vehicle has a mass in service greater than or equal to {int} kg")
  public void my_vehicle_mass_in_servce_greater_than_or_equal_to(int setMassInService) {
    world.vehicle.setMassInService(setMassInService);
  }
  
  @Given("that my vehicle revenue weight is missing")
  public void my_vehicle_revenue_weight_is_missing() {
    world.vehicle.setRevenueWeight(null);
  }

  @Then("my vehicle is identified as a van")
  public void my_vehicle_is_identified_as_large_van() {
    assertEquals(VehicleType.VAN, world.vehicle.getVehicleType());
  }

  @Then("my vehicle is identified as a minibus")
  public void identified_as_minibus() {
    assertEquals(VehicleType.MINIBUS, world.vehicle.getVehicleType());
  }

  @Then("my vehicle is identified as a bus\\/coach")
  public void my_vehicle_is_identified_as_bus_or_coach() {
    assertEquals(VehicleType.BUS, world.vehicle.getVehicleType());
  }

  @Then("my vehicle is identified as an HGV")
  public void my_vehicle_is_identified_as_an_hgv() {
    assertEquals(VehicleType.HGV, world.vehicle.getVehicleType());
  }

  @Then("my vehicle cannot be identified")
  public void unsupported_operation_exception_thrown() {
    Assertions.assertThrows(UnidentifiableVehicleException.class, () -> {
      world.identifier.identifyVehicle(world.vehicle);
    });
  }

}
