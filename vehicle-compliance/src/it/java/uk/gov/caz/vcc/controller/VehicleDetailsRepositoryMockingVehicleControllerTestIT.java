package uk.gov.caz.vcc.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.vcc.annotation.MockedMvcIntegrationTest;
import uk.gov.caz.vcc.domain.exceptions.ExternalServiceCallException;
import uk.gov.caz.vcc.service.VehicleService;

@MockedMvcIntegrationTest
public class VehicleDetailsRepositoryMockingVehicleControllerTestIT {

  @MockBean
  private VehicleService vehicleService;

  @Autowired
  private MockMvc mockMvc;

  @Test
  public void shouldReturn404IfDetailsNotFound() throws Exception {
    String vrn = "CAS100";
    when(vehicleService.findVehicle(vrn)).thenThrow(ExternalServiceCallException.class);

    String testUrl = "/v1/compliance-checker/vehicles/{vrn}/details";

    mockMvc.perform(get(testUrl, vrn).header("X-Correlation-Id", UUID.randomUUID().toString()))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message", is("Service unavailable")));
  }
}