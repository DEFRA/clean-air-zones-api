package uk.gov.caz.taxiregister.service;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicence;

/**
 * An exception which indicates that there was a licence whose licensing authority was not present
 * in the database.
 */
@AllArgsConstructor
@Getter
class LicensingAuthorityMismatchException extends IllegalArgumentException {

  private static final long serialVersionUID = -5365630128856068164L;

  private final List<TaxiPhvVehicleLicence> licencesWithNonMatchingLicensingAuthority;
}
