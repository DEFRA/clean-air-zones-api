package uk.gov.caz.vcc.util;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.exceptions.FailedIdentificationLogs;
import uk.gov.caz.vcc.repository.IdentificationErrorRepository;

/**
 * Utility component for handling checked exceptions and persisting them as
 * LoggedIdentificationException.
 * 
 * @author informed
 */
@Component
public class UnidentifiableVehicleExceptionHandler {

  private final IdentificationErrorRepository identificationErrorRepository;

  @Value("${application.version}")
  private String applicationVersion;

  /**
   * Public constructor for the ExceptionHandler.
   * 
   * @param identificationErrorRepository repository implementation for
   *                                      persisting identification errors.
   */
  public UnidentifiableVehicleExceptionHandler(
      IdentificationErrorRepository identificationErrorRepository) {

    this.identificationErrorRepository = identificationErrorRepository;

  }

  /**
   * Method to catch and log errors during the vehicle identification process.
   */
  public void handleError(Throwable e, Vehicle vehicle) {

    FailedIdentificationLogs loggedException = new FailedIdentificationLogs();

    loggedException.setFailedidentificationId(UUID.randomUUID());
    loggedException.setRegistrationNumber(vehicle.getRegistrationNumber());
    loggedException.setApplicationVersion(applicationVersion);
    loggedException.setExceptionCause(e.getMessage());
    loggedException.setInsertTimestamp(Instant.now());

    identificationErrorRepository.save(loggedException);
  }

}
