package uk.gov.caz.vcc.domain.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.caz.definitions.exceptions.UnidentifiableVehicleException;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.definitions.domain.VehicleType;
import uk.gov.caz.vcc.domain.service.vehicleidentifiers.NullVehicleIdentifier;
import uk.gov.caz.vcc.util.UnidentifiableVehicleExceptionHandler;

@ExtendWith(MockitoExtension.class)
public class VehicleIdentificationServiceTest {

  @Mock
  UnidentifiableVehicleExceptionHandler exceptionHandler;

  @Mock
  NullVehicleIdentifier nullVehicleIdentifier;

  @InjectMocks
  VehicleIdentificationService vehicleIdentificationService;

  private Vehicle unidentifiableVehicle;

  @BeforeEach
  void init() {
    Vehicle unidentifiableVehicle = new Vehicle();

    this.unidentifiableVehicle = unidentifiableVehicle;
  }

  @Test
  void unidentifiableVehicleExceptionIsCaught() {
    // Vehicle with below attributes not be identifiable => throws exception
    unidentifiableVehicle.setTaxClass("RP BUS");
    unidentifiableVehicle.setBodyType("Unidentifiable body type.");

    vehicleIdentificationService.setVehicleType(unidentifiableVehicle);
  }

  @Test
  void unidentifiableVehicleExceptionSetsNullvehicleType() {
    // Vehicle with below attributes not be identifiable => throws exception
    unidentifiableVehicle.setTaxClass("RP BUS");
    unidentifiableVehicle.setBodyType("Unidentifiable body type.");

    vehicleIdentificationService.setVehicleType(unidentifiableVehicle);

    assertNull(unidentifiableVehicle.getVehicleType());
  }

  @Test
  void unidentifiableVehicleExceptionCallsExceptionHandler() {
    // Vehicle with below attributes not be identifiable => throws exception
    unidentifiableVehicle.setTaxClass("RP BUS");
    unidentifiableVehicle.setBodyType("Unidentifiable body type.");

    vehicleIdentificationService.setVehicleType(unidentifiableVehicle);

    verify(exceptionHandler, times(1)).handleError(
        Mockito.any(UnidentifiableVehicleException.class),
        Mockito.eq(unidentifiableVehicle));
  }

  @Test
  void unrecognisedTypeApprovalCallsNullIdentifier() {
    unidentifiableVehicle.setTypeApproval("unrecognised");
    unidentifiableVehicle.setTaxClass("HGV");

    vehicleIdentificationService.setVehicleType(unidentifiableVehicle);

    // Checks implicitly, as Mockito cannot mock calls to new MyClass();
    assertEquals(VehicleType.HGV, unidentifiableVehicle.getVehicleType());
  }

  @Test
  void unrecognisedTypeApprovalSetsNullTypeApproval() {
    unidentifiableVehicle.setTypeApproval("unrecognised");
    unidentifiableVehicle.setTaxClass("HGV");

    vehicleIdentificationService.setVehicleType(unidentifiableVehicle);

    assertNull(unidentifiableVehicle.getTypeApproval());
  }

  @Test
  void unrecognisedTaxClassIsCaught() {
    unidentifiableVehicle.setTaxClass("MOBILE CRANE");

    // For inspecting the caught error.
    ArgumentCaptor<UnidentifiableVehicleException> exceptionArgument = ArgumentCaptor
        .forClass(UnidentifiableVehicleException.class);

    vehicleIdentificationService.setVehicleType(unidentifiableVehicle);

    verify(exceptionHandler, times(1)).handleError(exceptionArgument.capture(),
        Mockito.eq(unidentifiableVehicle));
    assertEquals("taxClass not recognised.",
        exceptionArgument.getValue().getMessage());
  }

  @Test
  void unrecognisedBodyTypeIsCaught() {
    unidentifiableVehicle.setTaxClass("RP BUS");
    unidentifiableVehicle.setBodyType("Transformer");

    // For inspecting the caught error.
    ArgumentCaptor<UnidentifiableVehicleException> exceptionArgument = ArgumentCaptor
        .forClass(UnidentifiableVehicleException.class);

    vehicleIdentificationService.setVehicleType(unidentifiableVehicle);

    verify(exceptionHandler, times(1)).handleError(exceptionArgument.capture(),
        Mockito.eq(unidentifiableVehicle));
    assertEquals("bodyType not recognised.",
        exceptionArgument.getValue().getMessage());
  }

  @Test
  void nullRevenueWeightM2AttemptsToIdentifyUsingNullIdentifier()  {
    unidentifiableVehicle.setTypeApproval("M2");
    unidentifiableVehicle.setTaxClass("RP HGV");

    vehicleIdentificationService.setVehicleType(unidentifiableVehicle);

    // Checks implicitly, as Mockito cannot mock calls to new MyClass();
    assertEquals(VehicleType.HGV, unidentifiableVehicle.getVehicleType());
  }


  @Test
  void zeroSeatingCapacityM2AttemptsToIdentifyUsingNullIdentifier() {
    unidentifiableVehicle.setTypeApproval("M2");
    unidentifiableVehicle.setRevenueWeight(3051);
    unidentifiableVehicle.setTaxClass("RP HGV");
    unidentifiableVehicle.setSeatingCapacity(0);


    vehicleIdentificationService.setVehicleType(unidentifiableVehicle);

    // Checks implicitly, as Mockito cannot mock calls to new MyClass();
    assertEquals(VehicleType.HGV, unidentifiableVehicle.getVehicleType());
  }

  @Test
  void tooLargeRevenueWeightM2AttemptsToIdentifyUsingNullIdentifier() {
    unidentifiableVehicle.setTypeApproval("M2");
    unidentifiableVehicle.setRevenueWeight(10000);
    unidentifiableVehicle.setTaxClass("RP HGV");
    unidentifiableVehicle.setSeatingCapacity(0);


    vehicleIdentificationService.setVehicleType(unidentifiableVehicle);

    // Checks implicitly, as Mockito cannot mock calls to new MyClass();
    assertEquals(VehicleType.HGV, unidentifiableVehicle.getVehicleType());
  }

  @Test
  void nullRevenueWeightM3AttemptsToIdentifyUsingNullIdentifier()  {
    unidentifiableVehicle.setTypeApproval("M3");
    unidentifiableVehicle.setTaxClass("RP HGV");

    vehicleIdentificationService.setVehicleType(unidentifiableVehicle);

    // Checks implicitly, as Mockito cannot mock calls to new MyClass();
    assertEquals(VehicleType.HGV, unidentifiableVehicle.getVehicleType());
  }

  @Test
  void tooLargeRevenueWeightN1AttemptsToIdentifyUsingNullIdentifier() {
    unidentifiableVehicle.setTypeApproval("N1");
    unidentifiableVehicle.setRevenueWeight(10000);
    unidentifiableVehicle.setTaxClass("RP HGV");
    unidentifiableVehicle.setSeatingCapacity(0);


    vehicleIdentificationService.setVehicleType(unidentifiableVehicle);

    // Checks implicitly, as Mockito cannot mock calls to new MyClass();
    assertEquals(VehicleType.HGV, unidentifiableVehicle.getVehicleType());
  }

  @Test
  void nullRevenueWeightN1AttemptsToIdentifyUsingNullIdentifier()  {
    unidentifiableVehicle.setTypeApproval("N1");
    unidentifiableVehicle.setTaxClass("RP HGV");

    vehicleIdentificationService.setVehicleType(unidentifiableVehicle);

    // Checks implicitly, as Mockito cannot mock calls to new MyClass();
    assertEquals(VehicleType.HGV, unidentifiableVehicle.getVehicleType());
  }

  @Test
  void nullRevenueWeightN2AttemptsToIdentifyUsingNullIdentifier()  {
    unidentifiableVehicle.setTypeApproval("N2");
    unidentifiableVehicle.setTaxClass("RP HGV");

    vehicleIdentificationService.setVehicleType(unidentifiableVehicle);

    // Checks implicitly, as Mockito cannot mock calls to new MyClass();
    assertEquals(VehicleType.HGV, unidentifiableVehicle.getVehicleType());
  }

  @Test
  void tooSmallRevenueWeightN2AttemptsToIdentifyUsingNullIdentifier() {
    unidentifiableVehicle.setTypeApproval("N2");
    unidentifiableVehicle.setRevenueWeight(3500);
    unidentifiableVehicle.setTaxClass("RP HGV");
    unidentifiableVehicle.setSeatingCapacity(0);


    vehicleIdentificationService.setVehicleType(unidentifiableVehicle);

    // Checks implicitly, as Mockito cannot mock calls to new MyClass();
    assertEquals(VehicleType.HGV, unidentifiableVehicle.getVehicleType());
  }



  @Test
  void nullTaxClassIsCaught() {
    // For inspecting the caught error.
    ArgumentCaptor<UnidentifiableVehicleException> exceptionArgument = ArgumentCaptor
        .forClass(UnidentifiableVehicleException.class);

    vehicleIdentificationService.setVehicleType(unidentifiableVehicle);

    verify(exceptionHandler, times(1)).handleError(exceptionArgument.capture(),
        Mockito.eq(unidentifiableVehicle));
    assertEquals("Cannot identify vehicle with null: taxClass",
        exceptionArgument.getValue().getMessage());
  }

  @Test
  void nullBodyTypeIsCaught() {
    unidentifiableVehicle.setTaxClass("CROWN VEHICLE");

    // For inspecting the caught error.
    ArgumentCaptor<UnidentifiableVehicleException> exceptionArgument = ArgumentCaptor
        .forClass(UnidentifiableVehicleException.class);

    vehicleIdentificationService.setVehicleType(unidentifiableVehicle);

    verify(exceptionHandler, times(1)).handleError(exceptionArgument.capture(),
        Mockito.eq(unidentifiableVehicle));
    assertEquals("Cannot identify vehicle with null: bodyType",
        exceptionArgument.getValue().getMessage());
  }

  @Test
  void zeroRevenueWeightIsCaughtNullIdentifierIsCalled() {
    unidentifiableVehicle.setTypeApproval("M2");
    unidentifiableVehicle.setRevenueWeight(0);

    unidentifiableVehicle.setTaxClass("RP HGV");

    vehicleIdentificationService.setVehicleType(unidentifiableVehicle);

    // Checks implicitly, as Mockito cannot mock calls to new MyClass();
    assertEquals(VehicleType.HGV, unidentifiableVehicle.getVehicleType());
  }

  @Test
  void zeroSeatingCapacityIsCaught() {
    unidentifiableVehicle.setTypeApproval("M2");
    unidentifiableVehicle.setRevenueWeight(42);
    unidentifiableVehicle.setMassInService(42);
    unidentifiableVehicle.setSeatingCapacity(0);
    unidentifiableVehicle.setTaxClass("RP HGV");

    vehicleIdentificationService.setVehicleType(unidentifiableVehicle);

    // Checks implicitly, as Mockito cannot mock calls to new MyClass();
    assertEquals(VehicleType.HGV, unidentifiableVehicle.getVehicleType());
  }

  @Test
  void emptyTaxClassIsCaught() {
    unidentifiableVehicle.setTaxClass("");

    // For inspecting the caught error.
    ArgumentCaptor<UnidentifiableVehicleException> exceptionArgument = ArgumentCaptor
        .forClass(UnidentifiableVehicleException.class);

    vehicleIdentificationService.setVehicleType(unidentifiableVehicle);

    verify(exceptionHandler, times(1)).handleError(exceptionArgument.capture(),
        Mockito.eq(unidentifiableVehicle));
    assertEquals("Cannot identify vehicle with null: taxClass",
        exceptionArgument.getValue().getMessage());
  }

  @Test
  void emptyBodyTypeIsCaught() {
    unidentifiableVehicle.setTaxClass("CROWN VEHICLE");
    unidentifiableVehicle.setBodyType("");

    // For inspecting the caught error.
    ArgumentCaptor<UnidentifiableVehicleException> exceptionArgument = ArgumentCaptor
        .forClass(UnidentifiableVehicleException.class);

    vehicleIdentificationService.setVehicleType(unidentifiableVehicle);

    verify(exceptionHandler, times(1)).handleError(exceptionArgument.capture(), Mockito.eq(unidentifiableVehicle));
    assertEquals("Cannot identify vehicle with null: bodyType", exceptionArgument.getValue().getMessage());
  }

  @Test
  void emptySeatingCapacityAndEmptyTaxClassIsCaught() {
    unidentifiableVehicle.setTypeApproval("M2");
    unidentifiableVehicle.setRevenueWeight(42);
    unidentifiableVehicle.setMassInService(42);

    // For inspecting the caught error.
    ArgumentCaptor<UnidentifiableVehicleException> exceptionArgument = ArgumentCaptor
        .forClass(UnidentifiableVehicleException.class);

    vehicleIdentificationService.setVehicleType(unidentifiableVehicle);

    verify(exceptionHandler, times(1)).handleError(exceptionArgument.capture(), Mockito.eq(unidentifiableVehicle));
    assertEquals("Cannot identify vehicle with null: taxClass", exceptionArgument.getValue().getMessage());

  }

  @Test
  void emptySeatingCapacityAndEmptyBodyTypeIsCaught() {
    unidentifiableVehicle.setTypeApproval("M2");
    unidentifiableVehicle.setRevenueWeight(42);
    unidentifiableVehicle.setMassInService(42);
    unidentifiableVehicle.setTaxClass("CROWN VEHICLE");
    unidentifiableVehicle.setBodyType("");

    // For inspecting the caught error.
    ArgumentCaptor<UnidentifiableVehicleException> exceptionArgument = ArgumentCaptor
        .forClass(UnidentifiableVehicleException.class);

    vehicleIdentificationService.setVehicleType(unidentifiableVehicle);

    verify(exceptionHandler, times(1)).handleError(exceptionArgument.capture(), Mockito.eq(unidentifiableVehicle));
    assertEquals("Cannot identify vehicle with null: bodyType", exceptionArgument.getValue().getMessage());

  }

  @Test
  void emptyRevenueWeightAndEmptyTaxClassIsCaught() {
    unidentifiableVehicle.setTypeApproval("M2");

    // For inspecting the caught error.
    ArgumentCaptor<UnidentifiableVehicleException> exceptionArgument = ArgumentCaptor
        .forClass(UnidentifiableVehicleException.class);

    vehicleIdentificationService.setVehicleType(unidentifiableVehicle);

    verify(exceptionHandler, times(1)).handleError(exceptionArgument.capture(), Mockito.eq(unidentifiableVehicle));
    assertEquals("Cannot identify vehicle with null: taxClass", exceptionArgument.getValue().getMessage());

  }

  @Test
  void emptyRevenueWeightAndEmptyBodyTypeIsCaught() {
    unidentifiableVehicle.setTypeApproval("M2");
    unidentifiableVehicle.setTaxClass("CROWN VEHICLE");
    unidentifiableVehicle.setBodyType("");

    // For inspecting the caught error.
    ArgumentCaptor<UnidentifiableVehicleException> exceptionArgument = ArgumentCaptor
        .forClass(UnidentifiableVehicleException.class);

    vehicleIdentificationService.setVehicleType(unidentifiableVehicle);

    verify(exceptionHandler, times(1)).handleError(exceptionArgument.capture(), Mockito.eq(unidentifiableVehicle));
    assertEquals("Cannot identify vehicle with null: bodyType", exceptionArgument.getValue().getMessage());

  }


}
