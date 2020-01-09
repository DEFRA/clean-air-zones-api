package uk.gov.caz.vcc.domain.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.caz.vcc.domain.CalculationResult;
import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.service.compliance.EuroStatusPresentComplianceService;
import uk.gov.caz.vcc.domain.service.compliance.LeedsComplianceService;
import uk.gov.caz.vcc.domain.service.compliance.EuroStatusNullComplianceService;
import uk.gov.caz.vcc.domain.service.compliance.UnableToIdentifyVehicleComplianceException;

@ExtendWith(MockitoExtension.class)
public class ComplianceServiceTest {
  private ComplianceService complianceService;
  private String leedsCazIdentifier = "39e54ed8-3ed2-441d-be3f-38fc9b70c8d3";
  private String arbitratryCazIdentifier = "dd487c1e-11d3-11ea-8d71-362b9e155667";
  private CalculationResult result;
  
  @Mock
  private EuroStatusPresentComplianceService euroStatusPresentComplianceService;
  @Mock
  private EuroStatusNullComplianceService euroStatusNullComplianceService;
  @Mock
  private LeedsComplianceService leedsComplianceService;

  
  @BeforeEach
  void init() {
    complianceService = new ComplianceService(leedsCazIdentifier,
                                              leedsComplianceService,
                                              euroStatusNullComplianceService,
                                              euroStatusPresentComplianceService);
    result = new CalculationResult();
  }

  @Test
  void givenVehicleEnterLeedsCazThenLeedsComplianceServiceIsInvoked() {
    Vehicle vehicle = new Vehicle();
    result.setCazIdentifier(UUID.fromString(this.leedsCazIdentifier));
    complianceService.updateCalculationResult(vehicle, result);
    verify(leedsComplianceService).isVehicleCompliance(vehicle);
    verifyNoInteractions(euroStatusNullComplianceService);
    verifyNoInteractions(euroStatusPresentComplianceService);
  }

  @Test
  void givenUnrecognisedVehicleEnterLeedsCazThenEuroComplianceServiceIsInvoked() {
    Vehicle vehicle = new Vehicle();
    result.setCazIdentifier(UUID.fromString(this.leedsCazIdentifier));
    
    //given
    when(leedsComplianceService.isVehicleCompliance(vehicle)).thenThrow(UnableToIdentifyVehicleComplianceException.class);
    
    //when
    complianceService.updateCalculationResult(vehicle, result);

    //then
    verify(leedsComplianceService).isVehicleCompliance(vehicle);
    verify(euroStatusNullComplianceService).isVehicleCompliance(vehicle);
    verifyNoInteractions(euroStatusPresentComplianceService);
  }

  @Test
  void givenEuroVehicleEnterOtherThanLeedsCazThenEuroComplianceServiceIsInvoked() {
    Vehicle vehicle = new Vehicle();
    vehicle.setEuroStatus("EURO 0");
    result.setCazIdentifier(UUID.fromString(this.arbitratryCazIdentifier));
    complianceService.updateCalculationResult(vehicle, result);
    verify(euroStatusPresentComplianceService).isVehicleCompliance(vehicle);
    verifyNoInteractions(leedsComplianceService);
    verifyNoInteractions(euroStatusNullComplianceService);
  }


  @Test
  void givenNonEuroVehicleEnterOtherThanLeedsCazThenNonEuroComplianceServiceIsInvoked() {
    Vehicle vehicle = new Vehicle();
    vehicle.setEuroStatus(null);
    result.setCazIdentifier(UUID.fromString(this.arbitratryCazIdentifier));
    complianceService.updateCalculationResult(vehicle, result);
    verify(euroStatusNullComplianceService).isVehicleCompliance(vehicle);
    verifyNoInteractions(leedsComplianceService);
    verifyNoInteractions(euroStatusPresentComplianceService);
  }
}
