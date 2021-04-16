package uk.gov.caz.vcc.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.caz.definitions.domain.VehicleType;
import uk.gov.caz.vcc.annotation.IntegrationTest;
import uk.gov.caz.vcc.dto.VehicleEntrantReportingRequest;
import uk.gov.caz.vcc.repository.EntrantExemptionRepository;
import uk.gov.caz.vcc.repository.EntrantTaxiPhvRepository;
import uk.gov.caz.vcc.repository.VehicleEntrantReportingRepository;
import uk.gov.caz.vcc.util.MockServerTestIT;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@IntegrationTest
public class ReportingDataServiceIT extends MockServerTestIT {
  
  @Autowired
  private ReportingDataService reportingDataService;
  
  @Autowired
  private JdbcTemplate jdbcTemplate;
  
  @Autowired
  private VehicleEntrantReportingRepository entrantReportRepository;
  
  @Autowired
  private EntrantExemptionRepository entrantExemptionRepository;
  
  @Autowired
  private EntrantTaxiPhvRepository entrantTaxiPhvRepository;
  
  @AfterEach
  @BeforeEach
  public void cleanup(){
    mockServer.reset();
    entrantTaxiPhvRepository.deleteAll();
    entrantExemptionRepository.deleteAll();
    entrantReportRepository.deleteAll();
  }
  
  @Test
  public void shouldProcessJson() throws JsonProcessingException {
    VehicleEntrantReportingRequest vehicleEntrantReportRequest =
        VehicleEntrantReportingRequest.builder()
        .vrnHash("1234")
        .hour("2020-01-01T16:00:00Z")
        .cleanAirZoneId(UUID.fromString("131af03c-f7f4-4aef-81ee-aae4f56dbeb5"))
        .typeApproval("M1")
        .fuelType("diesel")
        .vehicleType(VehicleType.PRIVATE_CAR)
        .chargeValidityCode("CVC04")
        .make("Fiat")
        .model("500")
        .colour("Fuschia")
        .nonStandardUkPlateFormat(false)
        .build();
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(Arrays.asList(vehicleEntrantReportRequest));
    String messageId = "1234";
    String messageBody = json;
    reportingDataService.process(messageBody, messageId);
    assertThat(countAllVehicleReports()).isEqualTo(1);
  }
  
  @Test
  public void shouldProcessJsonExemption() throws JsonProcessingException {
    VehicleEntrantReportingRequest vehicleEntrantReportRequest =
        VehicleEntrantReportingRequest.builder()
        .vrnHash("12345")
        .hour("2020-01-01T16:00:00Z")
        .cleanAirZoneId(UUID.fromString("131af03c-f7f4-4aef-81ee-aae4f56dbeb5"))
        .typeApproval("M1")
        .fuelType("diesel")
        .vehicleType(VehicleType.PRIVATE_CAR)
        .chargeValidityCode("CVC02")
        .make("Fiat")
        .model("500")
        .colour("Fuschia")
        .exemptionReason("STEAM")
        .nonStandardUkPlateFormat(false)
        .build();
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(Arrays.asList(vehicleEntrantReportRequest));
    String messageId = "1234";
    String messageBody = json;
    reportingDataService.process(messageBody, messageId);
    assertThat(countAllVehicleReports()).isEqualTo(1);
    assertThat(countAllExemptionReports()).isEqualTo(1);
    assertThat(countExemptionReasonSteam()).isEqualTo(1);
  }
  
  @Test
  public void shouldProcessJsonTaxi() throws JsonProcessingException {
    VehicleEntrantReportingRequest vehicleEntrantReportRequest =
        VehicleEntrantReportingRequest.builder()
        .vrnHash("12345")
        .hour("2020-01-01T16:00:00Z")
        .cleanAirZoneId(UUID.fromString("131af03c-f7f4-4aef-81ee-aae4f56dbeb5"))
        .typeApproval("M1")
        .fuelType("diesel")
        .vehicleType(VehicleType.TAXI_OR_PHV)
        .chargeValidityCode("CVC04")
        .make("Fiat")
        .model("500")
        .colour("Fuschia")
        .taxiPhvDescription("Taxi")
        .licensingAuthorities(Arrays.asList("Bath", "Birmingham"))
        .nonStandardUkPlateFormat(false)
        .build();
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(Arrays.asList(vehicleEntrantReportRequest));
    String messageId = "1234";
    String messageBody = json;
    reportingDataService.process(messageBody, messageId);
    assertThat(countAllVehicleReports()).isEqualTo(1);
    assertThat(countAllTaxiReports()).isEqualTo(2);
  }
  
  @Test
  public void shouldProcessJsonWithNullTypeApproval() throws JsonProcessingException {
    VehicleEntrantReportingRequest vehicleEntrantReportRequest =
        VehicleEntrantReportingRequest.builder()
        .vrnHash("1234")
        .hour("2020-01-01T16:00:00Z")
        .cleanAirZoneId(UUID.fromString("131af03c-f7f4-4aef-81ee-aae4f56dbeb5"))
        .fuelType("diesel")
        .vehicleType(VehicleType.PRIVATE_CAR)
        .chargeValidityCode("CVC04")
        .make("Fiat")
        .model("500")
        .colour("Fuschia")
        .nonStandardUkPlateFormat(true)
        .build();
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(Arrays.asList(vehicleEntrantReportRequest));
    String messageId = "1234";
    String messageBody = json;
    reportingDataService.process(messageBody, messageId);
    assertThat(countAllVehicleReports()).isEqualTo(1);
  }
  
  @Test
  public void shouldProcessJsonWithNullFuelType() throws JsonProcessingException {
    VehicleEntrantReportingRequest vehicleEntrantReportRequest =
        VehicleEntrantReportingRequest.builder()
        .vrnHash("1234")
        .hour("2020-01-01T16:00:00Z")
        .cleanAirZoneId(UUID.fromString("131af03c-f7f4-4aef-81ee-aae4f56dbeb5"))
        .typeApproval("M1")
        .vehicleType(VehicleType.PRIVATE_CAR)
        .chargeValidityCode("CVC04")
        .make("Fiat")
        .model("500")
        .colour("Fuschia")
        .nonStandardUkPlateFormat(false)
        .build();
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(Arrays.asList(vehicleEntrantReportRequest));
    String messageId = "1234";
    String messageBody = json;
    reportingDataService.process(messageBody, messageId);
    assertThat(countAllVehicleReports()).isEqualTo(1);
  }
  
  @Test
  public void shouldProcessJsonWithNullVehicleType() throws JsonProcessingException {
    VehicleEntrantReportingRequest vehicleEntrantReportRequest =
        VehicleEntrantReportingRequest.builder()
        .vrnHash("1234")
        .hour("2020-01-01T16:00:00Z")
        .cleanAirZoneId(UUID.fromString("131af03c-f7f4-4aef-81ee-aae4f56dbeb5"))
        .typeApproval("M1")
        .fuelType("diesel")
        .chargeValidityCode("CVC04")
        .make("Fiat")
        .model("500")
        .colour("Fuschia")
        .nonStandardUkPlateFormat(false)
        .build();
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(Arrays.asList(vehicleEntrantReportRequest));
    String messageId = "1234";
    String messageBody = json;
    reportingDataService.process(messageBody, messageId);
    assertThat(countAllVehicleReports()).isEqualTo(1);
  }
  
  @Test
  public void shouldProcessJsonExemptionWithNullExemptionReason() throws JsonProcessingException {
    VehicleEntrantReportingRequest vehicleEntrantReportRequest =
        VehicleEntrantReportingRequest.builder()
        .vrnHash("12345")
        .hour("2020-01-01T16:00:00Z")
        .cleanAirZoneId(UUID.fromString("131af03c-f7f4-4aef-81ee-aae4f56dbeb5"))
        .typeApproval("M1")
        .fuelType("diesel")
        .vehicleType(VehicleType.PRIVATE_CAR)
        .chargeValidityCode("CVC02")
        .make("Fiat")
        .model("500")
        .colour("Fuschia")
        .nonStandardUkPlateFormat(true)
        .build();
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(Arrays.asList(vehicleEntrantReportRequest));
    String messageId = "1234";
    String messageBody = json;
    reportingDataService.process(messageBody, messageId);
    assertThat(countAllVehicleReports()).isEqualTo(1);
    assertThat(countAllExemptionReports()).isEqualTo(1);
  }
  
  private int countAllVehicleReports() {
    return JdbcTestUtils
        .countRowsInTable(jdbcTemplate, "caz_reporting.t_vehicle_entrant_reporting");
  }
  
  private int countAllExemptionReports() {
    return JdbcTestUtils
        .countRowsInTable(jdbcTemplate, "caz_reporting.t_entrant_exemption");
  }
  
  private int countExemptionReasonSteam() {
    return JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, 
        "caz_reporting.t_entrant_exemption", 
        "exemption_reason_id = '5ca5228d-e968-4a15-bc7d-5366c9022491'");
  }
  
  private int countAllTaxiReports() {
    return JdbcTestUtils
        .countRowsInTable(jdbcTemplate, "caz_reporting.t_entrant_taxi_phv");
  }
  
}
