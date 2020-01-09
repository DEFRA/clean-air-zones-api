package uk.gov.caz.taxiregister.dto;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;

import uk.gov.caz.taxiregister.model.registerjob.RegisterJobTrigger;

public class VehicleDtoTest {

  @Test
  public void getRegisterJobReturnsIfPresent() {
    VehicleDto testVehicleDto = VehicleDto.builder()
        .registerJobTrigger(RegisterJobTrigger.CSV_FROM_S3)
        .build();

    RegisterJobTrigger jobTrigger = testVehicleDto.getRegisterJobTrigger();

    assertEquals(RegisterJobTrigger.CSV_FROM_S3, jobTrigger);
  }

  @Test
  public void getRegisterJobReturnsApiCallIfNotPresent() {
    VehicleDto testVehicleDto = VehicleDto.builder().build();

    RegisterJobTrigger jobTrigger = testVehicleDto.getRegisterJobTrigger();

    assertEquals(RegisterJobTrigger.API_CALL, jobTrigger);
  }
}
