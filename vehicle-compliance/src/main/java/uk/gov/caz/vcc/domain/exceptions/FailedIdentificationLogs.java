package uk.gov.caz.vcc.domain.exceptions;

import java.time.Instant;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * Entity for persisting UnidentifiableVehicleExceptions raised by the
 * VehicleIdentificationService.
 * 
 * @author informed
 */
@Getter
@Setter
@Entity
@Table(name = "t_failed_identification_logs")
public class FailedIdentificationLogs {

  @Id
  private UUID failedidentificationId;

  private String registrationNumber;

  private String exceptionCause;

  // Store API version for debugging of business rules in Production
  private String applicationVersion;

  private Instant insertTimestamp;
}
