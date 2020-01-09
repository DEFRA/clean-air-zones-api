package uk.gov.caz.vcc.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.caz.vcc.domain.CalculationResult;
import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.service.FuelTypeService;
import uk.gov.caz.vcc.domain.service.TaxClassService;
import uk.gov.caz.vcc.domain.service.TypeApprovalService;

@ExtendWith(MockitoExtension.class)
public class ExemptionServiceTest {

  @Mock
  private MilitaryVehicleService militaryVehicleService;

  @Mock
  private RetrofitService retrofitService;

  @Mock
  private FuelTypeService fuelTypeService;

  @Mock
  private TaxClassService taxClassService;

  @Mock
  private TypeApprovalService typeApprovalService;
 
  @InjectMocks
  private ExemptionService exemptionService;
  
  @Mock
  private CalculationResult calculationResult;

  private Vehicle vehicle;

  @BeforeEach
  public void init() {
    this.vehicle = new Vehicle();
    this.vehicle.setRegistrationNumber("CAS301");
    this.vehicle.setFuelType("petrol");
    this.vehicle.setTaxClass("electric motorcycle");
    this.vehicle.setTypeApproval("T1");
  }

  @Test
  public void fuelTypeExempt() {
    when(fuelTypeService.isExemptFuelType(anyString())).thenReturn(true);
    
    exemptionService.updateCalculationResult(this.vehicle,
        this.calculationResult);
    verify(calculationResult).setExempt(true);
    verifyNoMoreInteractions(calculationResult);
  }

  @Test
  public void taxClassExempt() {
    when(taxClassService.isExemptTaxClass(anyString())).thenReturn(true);
    
    exemptionService.updateCalculationResult(this.vehicle,
        this.calculationResult);
    verify(calculationResult).setExempt(true);
    verifyNoMoreInteractions(calculationResult);
  }

  @Test
  public void typeApprovalExempt() {
    when(typeApprovalService.isExemptTypeApproval(anyString())).thenReturn(true);
    
    exemptionService.updateCalculationResult(this.vehicle,
        this.calculationResult);
    verify(calculationResult).setExempt(true);
    verifyNoMoreInteractions(calculationResult);
  }

  @Test
  public void modWhitelistExempt() {
    when(militaryVehicleService.isMilitaryVehicle(anyString()))
        .thenReturn(true);
    exemptionService.updateCalculationResult(vehicle, calculationResult);
    verify(calculationResult).setExempt(true);
    verifyNoMoreInteractions(calculationResult);
  }

  @Test
  public void retrofittedWhitelistExempt() {
    when(retrofitService.isRetrofitted(anyString()))
        .thenReturn(true);
    exemptionService.updateCalculationResult(vehicle, calculationResult);
    verify(calculationResult).setExempt(true);
    verifyNoMoreInteractions(calculationResult);
  }
}
