package uk.gov.caz.taxiregister.service;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicence;

/**
 * An immutable value object holding a set of attributes which uniquely identifies a given
 * licence.
 */
@Value
@Builder
class UniqueLicenceAttributes {

  String vrm;
  LocalDate start;
  LocalDate end;
  String licensingAuthorityName;
  String licencePlateNumber;

  /**
   * Static factory method for {@link UniqueLicenceAttributes}.
   *
   * @param licence An instance of {@link TaxiPhvVehicleLicence} which will be mapped to
   *     {@link UniqueLicenceAttributes}.
   * @return An instance of {@link UniqueLicenceAttributes}.
   */
  public static UniqueLicenceAttributes from(TaxiPhvVehicleLicence licence) {
    return UniqueLicenceAttributes.builder()
        .vrm(licence.getVrm())
        .start(licence.getLicenseDates().getStart())
        .end(licence.getLicenseDates().getEnd())
        .licensingAuthorityName(licence.getLicensingAuthority().getName())
        .licencePlateNumber(licence.getLicensePlateNumber())
        .build();
  }
}
