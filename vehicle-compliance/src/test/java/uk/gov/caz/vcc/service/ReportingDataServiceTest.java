package uk.gov.caz.vcc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.definitions.domain.VehicleType;
import uk.gov.caz.vcc.domain.ReportingExemptionReason;
import uk.gov.caz.vcc.domain.ReportingFuelType;
import uk.gov.caz.vcc.domain.ReportingTypeApproval;
import uk.gov.caz.vcc.domain.ReportingVehicleType;
import uk.gov.caz.vcc.repository.EntrantExemptionRepository;
import uk.gov.caz.vcc.repository.EntrantTaxiPhvRepository;
import uk.gov.caz.vcc.repository.ReportingExemptionReasonRepository;
import uk.gov.caz.vcc.repository.ReportingFuelTypeRepository;
import uk.gov.caz.vcc.repository.ReportingTypeApprovalRepository;
import uk.gov.caz.vcc.repository.ReportingVehicleTypeRepository;
import uk.gov.caz.vcc.repository.VehicleEntrantReportingRepository;


@ExtendWith(MockitoExtension.class)
class ReportingDataServiceTest {

  @Mock
  private VehicleEntrantReportingRepository vehicleEntrantReportingRepository;
  
  @Mock
  private EntrantTaxiPhvRepository entrantTaxiPhvRepository;
  
  @Mock
  private EntrantExemptionRepository entrantExemptionRepository;
  
  @Mock
  private ReportingTypeApprovalRepository reportingTypeApprovalRepository;
  
  @Mock
  private ReportingFuelTypeRepository reportingFuelTypeRepository;
  
  @Mock
  private ReportingVehicleTypeRepository reportingVehicleTypeRepository;
  
  @Mock
  private ReportingExemptionReasonRepository reportingExemptionReasonRepository;

  @InjectMocks
  private ReportingDataService reportingDataService;
  
  @Test
  public void shouldReturnTypeApprovalId() {  
    mockTypeApprovalPresenceInDatabase("S7");
    UUID typeApprovalId = reportingDataService.getTypeApprovalId("S7");
    assertThat(typeApprovalId).isEqualTo(UUID.fromString("76ac1706-6078-4dd8-9aa3-679bdfbb2384"));
  }
  
  @Test
  public void shouldReturnFuelTypeId() { 
    mockFuelTypePresenceInDatabase("steam");
    UUID fuelTypeId = reportingDataService.getFuelTypeId("steam");
    assertThat(fuelTypeId).isEqualTo(UUID.fromString("76ac1706-6078-4dd8-9aa3-679bdfbb2384"));
  } 
  
  @Test
  public void shouldReturnCorrectExemptionReasonId() {
    mockExemptionReasonPresenceInDatabase("Historic Vehicle");
    UUID exemptionReasonId = reportingDataService.getExemptionReasonId("Historic Vehicle");
    assertThat(exemptionReasonId).isEqualTo(UUID.fromString("76ac1706-6078-4dd8-9aa3-679bdfbb2384"));
  } 
  
  @Test
  public void shouldReturnCorrectVehicleTypeId() {   
    mockVehicleTypePresenceInDatabase(VehicleType.BUS);
    UUID vehicleTypeId = reportingDataService.getCcazVehicleTypeId(VehicleType.BUS);
    assertThat(vehicleTypeId).isEqualTo(UUID.fromString("76ac1706-6078-4dd8-9aa3-679bdfbb2384"));
  }
  
  private void mockTypeApprovalPresenceInDatabase(String typeApproval) {
    when(reportingTypeApprovalRepository.findTypeApprovalId(typeApproval)).thenReturn(
        ReportingTypeApproval.builder().typeApprovalId(
            UUID.fromString("76ac1706-6078-4dd8-9aa3-679bdfbb2384")).build());
  }
  
  private void mockExemptionReasonPresenceInDatabase(String exemptionReason) {
    when(reportingExemptionReasonRepository.findExemptionReasonId(exemptionReason)).thenReturn(
        ReportingExemptionReason.builder().exemptionReasonId(
            UUID.fromString("76ac1706-6078-4dd8-9aa3-679bdfbb2384")).build());
  }
  
  private void mockFuelTypePresenceInDatabase(String fuelType) {
    when(reportingFuelTypeRepository.findFuelTypeId(fuelType)).thenReturn(
        ReportingFuelType.builder().fuelTypeId(
            UUID.fromString("76ac1706-6078-4dd8-9aa3-679bdfbb2384")).build());
  }
  
  private void mockVehicleTypePresenceInDatabase(VehicleType vehicleType) {
    when(reportingVehicleTypeRepository.findVehicleTypeId(vehicleType)).thenReturn(
        ReportingVehicleType.builder().vehicleTypeId(
            UUID.fromString("76ac1706-6078-4dd8-9aa3-679bdfbb2384")).build());
  }
}