package uk.gov.caz.vcc.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.exceptions.FailedIdentificationLogs;
import uk.gov.caz.vcc.domain.exceptions.UnidentifiableVehicleException;
import uk.gov.caz.vcc.repository.IdentificationErrorRepository;

@ExtendWith(MockitoExtension.class)
public class UnidentifiableVehicleExceptionHandlerTest {

  @Mock
  IdentificationErrorRepository identificationErrorRepository;

  @InjectMocks
  UnidentifiableVehicleExceptionHandler exceptionHandler;

  private Vehicle vehicle;
  private UnidentifiableVehicleException ex;

  @BeforeEach
  void init() {
    Vehicle vehicle = new Vehicle();
    vehicle.setRegistrationNumber("CAS300");

    UnidentifiableVehicleException e = new UnidentifiableVehicleException(
        "Vehicle not identifiable");

    String applicationVersion = "v0.1";

    ReflectionTestUtils.setField(exceptionHandler, "applicationVersion",
        applicationVersion);

    this.vehicle = vehicle;
    this.ex = e;
  }

  @Test
  void unidentifiableVehicleExceptionIsSaved() {
    exceptionHandler.handleError(ex, vehicle);

    verify(identificationErrorRepository, times(1))
        .save(any(FailedIdentificationLogs.class));
  }

  @Test
  void unidentifiableVehicleLogContainsUuid() {
    // For inspecting the caught error.
    ArgumentCaptor<FailedIdentificationLogs> exceptionLogArgument = ArgumentCaptor
        .forClass(FailedIdentificationLogs.class);

    exceptionHandler.handleError(ex, vehicle);

    verify(identificationErrorRepository, times(1))
        .save(exceptionLogArgument.capture());
    assertNotNull(exceptionLogArgument.getValue().getFailedidentificationId());
  }

  @Test
  void unidentifiableVehicleLogContainsApplicationVersion() {
    // For inspecting the caught error.
    ArgumentCaptor<FailedIdentificationLogs> exceptionLogArgument = ArgumentCaptor
        .forClass(FailedIdentificationLogs.class);

    exceptionHandler.handleError(ex, vehicle);

    verify(identificationErrorRepository, times(1))
        .save(exceptionLogArgument.capture());
    assertEquals("v0.1",
        exceptionLogArgument.getValue().getApplicationVersion());
  }

  @Test
  void unidentifiableVehicleLogContainsErrorMessage() {
    // For inspecting the caught error.
    ArgumentCaptor<FailedIdentificationLogs> exceptionLogArgument = ArgumentCaptor
        .forClass(FailedIdentificationLogs.class);

    exceptionHandler.handleError(ex, vehicle);

    verify(identificationErrorRepository, times(1))
        .save(exceptionLogArgument.capture());
    assertEquals(ex.getMessage(),
        exceptionLogArgument.getValue().getExceptionCause());
  }

  @Test
  void unidentifiableVehicleLogContainsRegistrationNumber() {
    // For inspecting the caught error.
    ArgumentCaptor<FailedIdentificationLogs> exceptionLogArgument = ArgumentCaptor
        .forClass(FailedIdentificationLogs.class);

    exceptionHandler.handleError(ex, vehicle);

    verify(identificationErrorRepository, times(1))
        .save(exceptionLogArgument.capture());
    assertEquals(vehicle.getRegistrationNumber(),
        exceptionLogArgument.getValue().getRegistrationNumber());

  }

  @Test
  void unidentifiableVehicleLogContainsInsertTimestamp() {
    // For inspecting the caught error.
    ArgumentCaptor<FailedIdentificationLogs> exceptionLogArgument = ArgumentCaptor
        .forClass(FailedIdentificationLogs.class);

    exceptionHandler.handleError(ex, vehicle);

    verify(identificationErrorRepository, times(1))
        .save(exceptionLogArgument.capture());
    assertEquals(Instant.class,
        exceptionLogArgument.getValue().getInsertTimestamp().getClass());
  }

}
