package uk.gov.caz.vcc.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import uk.gov.caz.vcc.domain.exceptions.FailedIdentificationLogs;

/**
 * Repository for persisting information about UnidentifiableVehicleExceptions
 * raised by the VehicleIdentificationService.
 * 
 * @author informed
 */
@Repository
public interface IdentificationErrorRepository
    extends CrudRepository<FailedIdentificationLogs, String> {
}
