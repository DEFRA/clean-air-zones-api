package uk.gov.caz.vcc.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.caz.vcc.annotation.IntegrationTest;
import uk.gov.caz.vcc.domain.LocalVehicle;
import uk.gov.caz.definitions.domain.VehicleType;

@IntegrationTest
public class VehicleIdentificationServiceTestIT {

  @Autowired
  private VehicleIdentificationService vehicleIdentificationService;

  @Test
  public void givenUnidentifiableM2NoRevenueWeightCallNullIdentifier() {
    LocalVehicle vehicle = new LocalVehicle();
    vehicle.setRegistrationNumber("UNID001");
    vehicle.setTypeApproval("M2");
    vehicle.setSeatingCapacity(9);
    vehicle.setTaxClass("HGV");
    vehicleIdentificationService.setVehicleType(vehicle);

    assertEquals(VehicleType.HGV, vehicle.getVehicleType());
  }

  @Test
  public void givenUnidentifiableM2LargeRevenueWeightCallNullIdentifier() {
    LocalVehicle vehicle = new LocalVehicle();
    vehicle.setRegistrationNumber("UNID001");
    vehicle.setTypeApproval("M2");
    vehicle.setRevenueWeight(6000);
    vehicle.setSeatingCapacity(9);
    vehicle.setTaxClass("HGV");
    vehicleIdentificationService.setVehicleType(vehicle);

    assertEquals(VehicleType.HGV, vehicle.getVehicleType());
  }

  @Test
  public void givenUnidentifiableM2ZeroSeatingCapacityCallNullIdentifier() {
    LocalVehicle vehicle = new LocalVehicle();
    vehicle.setRegistrationNumber("UNID001");
    vehicle.setTypeApproval("M2");
    vehicle.setRevenueWeight(3000);
    vehicle.setSeatingCapacity(0);
    vehicle.setTaxClass("HGV");
    vehicleIdentificationService.setVehicleType(vehicle);

    assertEquals(VehicleType.HGV, vehicle.getVehicleType());
  }
  
  @ParameterizedTest
  @ValueSource(strings = {"T6", "T7", "T8", "T9"})
  public void givenUnidentifiableTTypeApprovalSetTypeApprovalNull(String typeApproval) {
    LocalVehicle vehicle = new LocalVehicle();
    vehicle.setRegistrationNumber("UNID001");
    vehicle.setTypeApproval(typeApproval);
    vehicle.setRevenueWeight(3000);
    vehicle.setSeatingCapacity(0);
    vehicle.setTaxClass("HGV");
    vehicleIdentificationService.setVehicleType(vehicle);

    assertEquals(null, vehicle.getTypeApproval());
  }
  
  @ParameterizedTest
  @ValueSource(strings = {"L8", "L9"})
  public void givenUnidentifiableLTypeApprovalSetTypeApprovalNull(String typeApproval) {
    LocalVehicle vehicle = new LocalVehicle();
    vehicle.setRegistrationNumber("UNID001");
    vehicle.setTypeApproval("L8");
    vehicle.setRevenueWeight(3000);
    vehicle.setSeatingCapacity(0);
    vehicle.setTaxClass("HGV");
    vehicleIdentificationService.setVehicleType(vehicle);

    assertEquals(null, vehicle.getTypeApproval());
  }

  @Test
  public void givenUnidentifiableM3NoRevenueWeightCallNullIdentifier() {
    LocalVehicle vehicle = new LocalVehicle();
    vehicle.setRegistrationNumber("UNID001");
    vehicle.setTypeApproval("M3");
    vehicle.setSeatingCapacity(9);
    vehicle.setTaxClass("HGV");
    vehicleIdentificationService.setVehicleType(vehicle);

    assertEquals(VehicleType.HGV, vehicle.getVehicleType());
  }

  @Test
  public void givenUnidentifiableN1NoRevenueWeightCallNullIdentifier() {
    LocalVehicle vehicle = new LocalVehicle();
    vehicle.setRegistrationNumber("UNID002");
    vehicle.setTypeApproval("N1");
    vehicle.setSeatingCapacity(9);
    vehicle.setTaxClass("HGV");
    vehicleIdentificationService.setVehicleType(vehicle);

    assertEquals(VehicleType.HGV, vehicle.getVehicleType());
  }

  @Test
  public void givenUnidentifiableN1LargeRevenueWeightCallNullIdentifier() {
    LocalVehicle vehicle = new LocalVehicle();
    vehicle.setRegistrationNumber("UNID001");
    vehicle.setTypeApproval("N1");
    vehicle.setRevenueWeight(5000);
    vehicle.setSeatingCapacity(9);
    vehicle.setTaxClass("HGV");
    vehicleIdentificationService.setVehicleType(vehicle);

    assertEquals(VehicleType.HGV, vehicle.getVehicleType());
  }

  @Test
  public void givenUnidentifiableN2NoRevenueWeightCallNullIdentifier() {
    LocalVehicle vehicle = new LocalVehicle();
    vehicle.setRegistrationNumber("UNID001");
    vehicle.setTypeApproval("N2");
    vehicle.setSeatingCapacity(9);
    vehicle.setTaxClass("HGV");
    vehicleIdentificationService.setVehicleType(vehicle);

    assertEquals(VehicleType.HGV, vehicle.getVehicleType());
  }

  @Test
  public void givenUnidentifiableN2SmallRevenueWeightCallNullIdentifier() {
    LocalVehicle vehicle = new LocalVehicle();
    vehicle.setRegistrationNumber("UNID001");
    vehicle.setTypeApproval("N2");
    vehicle.setRevenueWeight(300);
    vehicle.setSeatingCapacity(9);
    vehicle.setTaxClass("HGV");
    vehicleIdentificationService.setVehicleType(vehicle);

    assertEquals(VehicleType.HGV, vehicle.getVehicleType());
  }

  @Test
  public void givenUnidentifiableVehicleNoTaxClassCallNullIdentifier() {
    LocalVehicle vehicle = new LocalVehicle();
    vehicle.setRegistrationNumber("UNID001");
    vehicle.setTypeApproval("N2");
    vehicle.setRevenueWeight(300);
    vehicle.setSeatingCapacity(9);
    vehicleIdentificationService.setVehicleType(vehicle);

    assertNull(vehicle.getVehicleType());
  }

}
