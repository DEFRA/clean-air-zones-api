package uk.gov.caz.vcc.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.vcc.domain.CalculationResult;
import uk.gov.caz.vcc.domain.service.AgriculturalExemptionService;
import uk.gov.caz.vcc.domain.service.FuelTypeService;
import uk.gov.caz.vcc.domain.service.TaxClassService;
import uk.gov.caz.vcc.domain.service.TypeApprovalService;

@ExtendWith(MockitoExtension.class)
public class ExemptionServiceTest {

  private final static String VRN = "CAS301";

  @Mock
  private FuelTypeService fuelTypeService;

  @Mock
  private TaxClassService taxClassService;

  @Mock
  private TypeApprovalService typeApprovalService;

  @Mock
  private MilitaryVehicleService militaryVehicleService;

  @Mock
  private GeneralWhitelistService generalWhitelistService;

  @InjectMocks
  private ExemptionService exemptionService;

  @Mock
  private CalculationResult calculationResult;
  
  @Mock
  private AgriculturalExemptionService agriculturalExemptionService;

  private Vehicle vehicle;

  @BeforeEach
  public void init() {
    this.vehicle = new Vehicle();
    this.vehicle.setRegistrationNumber(VRN);
    this.vehicle.setFuelType("petrol");
    this.vehicle.setTaxClass("electric motorcycle");
    this.vehicle.setTypeApproval("T1");
    this.vehicle.setTaxClass("agricultural Machine");
    lenient().when(militaryVehicleService.isMilitaryVehicle(VRN)).thenReturn(false);
    lenient().when(generalWhitelistService.exemptOnGeneralWhitelist(VRN)).thenReturn(false);
  }

  @Test
  public void fuelTypeExempt() {
    given(fuelTypeService.isExemptFuelType(anyString())).willReturn(true);

    callExemptionServiceAndValidateThatResultIsExempt();
  }

  @Test
  public void taxClassExempt() {
    given(taxClassService.isExemptTaxClass(anyString())).willReturn(true);

    callExemptionServiceAndValidateThatResultIsExempt();
  }

  @Test
  public void typeApprovalExempt() {
    given(typeApprovalService.isExemptTypeApproval(anyString())).willReturn(true);

    callExemptionServiceAndValidateThatResultIsExempt();
  }
  
  @Test
  public void agriculturalVehicleExempt() {
    given(agriculturalExemptionService.isExemptAgriculturalVehicle(this.vehicle)).willReturn(true);

    callExemptionServiceAndValidateThatResultIsExempt();
  }

  @Test
  public void modExempt() {
    given(militaryVehicleService.isMilitaryVehicle(VRN)).willReturn(true);

    callExemptionServiceAndValidateThatResultIsExempt();
  }

  @Test
  public void generalPurposeWhitelistExempt() {
    given(generalWhitelistService.exemptOnGeneralWhitelist(VRN)).willReturn(true);

    callExemptionServiceAndValidateThatResultIsExempt();
  }

  private void callExemptionServiceAndValidateThatResultIsExempt() {
    exemptionService.updateCalculationResult(this.vehicle, this.calculationResult);
    verify(calculationResult).setExempt(true);
    verifyNoMoreInteractions(calculationResult);
  }
}
