package uk.gov.caz.vcc.domain.service;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.vcc.domain.CalculationResult;
import uk.gov.caz.vcc.domain.service.compliance.EuroStatusPresentComplianceService;
import uk.gov.caz.vcc.service.GeneralWhitelistService;
import uk.gov.caz.vcc.service.RetrofitService;
import uk.gov.caz.vcc.domain.service.compliance.BathComplianceService;
import uk.gov.caz.vcc.domain.service.compliance.EuroStatusNullComplianceService;

@ExtendWith(MockitoExtension.class)
public class ComplianceServiceTest {
  
  @InjectMocks
  private ComplianceService complianceService;
  
  @Mock
  private EuroStatusPresentComplianceService euroStatusPresentComplianceService;
  
  @Mock
  private EuroStatusNullComplianceService euroStatusNullComplianceService;

  @Mock 
  private GeneralWhitelistService generalWhitelistService;
  
  @Mock
  private RetrofitService retrofitService;
  
  @Mock
  private BathComplianceService bathComplianceService;
  
  private String arbitratryCazIdentifier = "dd487c1e-11d3-11ea-8d71-362b9e155667";
  private String bathCazIdentifier = "131af03c-f7f4-4aef-81ee-aae4f56dbeb5";
  private CalculationResult result;
  
  @BeforeEach
  void init() {
    ReflectionTestUtils.setField(complianceService, "bathCazIdentifier", bathCazIdentifier);
    result = new CalculationResult();
  }

  @Test
  void givenEuroVehicleEnterCazThenEuroComplianceServiceIsInvoked() {
    Vehicle vehicle = new Vehicle();
    vehicle.setEuroStatus("EURO 0");
    result.setCazIdentifier(UUID.fromString(this.arbitratryCazIdentifier));
    complianceService.updateCalculationResult(vehicle, result);
    verify(euroStatusPresentComplianceService).isVehicleCompliant(vehicle);
    verifyNoInteractions(euroStatusNullComplianceService);
  }


  @Test
  void givenNonEuroVehicleEnterCazThenNonEuroComplianceServiceIsInvoked() {
    Vehicle vehicle = new Vehicle();
    vehicle.setEuroStatus(null);
    result.setCazIdentifier(UUID.fromString(this.arbitratryCazIdentifier));
    complianceService.updateCalculationResult(vehicle, result);
    verify(euroStatusNullComplianceService).isVehicleCompliant(vehicle);
    verifyNoInteractions(euroStatusPresentComplianceService);
  }
  
  @Test
  void givenWhitelistVehicleComplianceThenDoNotEnterOtherEuroStatusCompliance() {
    Vehicle vehicle = new Vehicle();
    vehicle.setRegistrationNumber("TEST100");
    mockGeneralWhitelistService(true);
    result.setCazIdentifier(UUID.fromString(this.arbitratryCazIdentifier));
    complianceService.updateCalculationResult(vehicle, result);
    verifyNoInteractions(euroStatusNullComplianceService);
    verifyNoInteractions(euroStatusPresentComplianceService);
  }
  
  @Test
  void givenWhitelistVehicleComplianceThenVehicleIsCompliant() {
    Vehicle vehicle = new Vehicle();
    vehicle.setRegistrationNumber("TEST100");
    mockGeneralWhitelistService(true);
    result.setCazIdentifier(UUID.fromString(this.arbitratryCazIdentifier));
    complianceService.updateCalculationResult(vehicle, result);
    assertTrue(result.getCompliant());
  }
  
  @Test
  void givenRetorfitVehicleThenVehicleIsCompliant() {
    Vehicle vehicle = new Vehicle();
    vehicle.setRegistrationNumber("TEST100");
    mockRetrofitService(true);
    result.setCazIdentifier(UUID.fromString(this.arbitratryCazIdentifier));
    complianceService.updateCalculationResult(vehicle, result);
    assertTrue(result.getCompliant());
  }
  
  @Test
  void givenUnrecognisedVehicleEnterBathCazThenEuroComplianceServiceIsInvoked() {
    Vehicle vehicle = new Vehicle();
    result.setCazIdentifier(UUID.fromString(this.bathCazIdentifier));
    
    //given
    when(bathComplianceService.isVehicleCompliant(vehicle))
        .thenReturn(Optional.empty());
    
    //when
    complianceService.updateCalculationResult(vehicle, result);

    //then
    verify(bathComplianceService).isVehicleCompliant(vehicle);
    verify(euroStatusNullComplianceService).isVehicleCompliant(vehicle);
    verifyNoInteractions(euroStatusPresentComplianceService);
  }
  
  @Test
  void givenVehicleEnterBathCazThenBathComplianceServiceIsInvoked() {
    Vehicle vehicle = new Vehicle();
    result.setCazIdentifier(UUID.fromString(this.bathCazIdentifier));

    complianceService.updateCalculationResult(vehicle, result);

    verify(bathComplianceService).isVehicleCompliant(vehicle);
  }

  private void mockGeneralWhitelistService(boolean isCompliantOnGeneralWhitelist) {
    given(generalWhitelistService.compliantOnGeneralWhitelist(anyString()))
        .willReturn(isCompliantOnGeneralWhitelist);
  }
  
  private void mockRetrofitService(boolean isRetrofitted) {
    given(retrofitService.isRetrofitted(anyString()))
        .willReturn(isRetrofitted);
  }

}
