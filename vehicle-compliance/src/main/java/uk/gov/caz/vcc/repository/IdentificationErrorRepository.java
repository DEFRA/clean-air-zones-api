package uk.gov.caz.vcc.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
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

  /**
   * Remove log data that is older then the given date.
   *
   * @param inputDate given date
   */
  @Modifying
  @Query(
      value = "DELETE FROM caz_vehicle_entrant.t_failed_identification_logs "
            + "WHERE CAST (date_trunc('day', inserttimestamp) AS date) <= :input_date",
      nativeQuery = true)
  int deleteLogsBeforeDate(@Param("input_date") LocalDate inputDate);
}
