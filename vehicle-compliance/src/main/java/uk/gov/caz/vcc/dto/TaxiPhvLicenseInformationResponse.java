package uk.gov.caz.vcc.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TaxiPhvLicenseInformationResponse implements Serializable {

  private static final long serialVersionUID = 74042876269358105L;

  boolean active;

  String description;

  Boolean wheelchairAccessible;

  List<String> licensingAuthoritiesNames;

  LocalDate licensedStatusExpires;

  /**
   * Method to return whether the taxi license is still active.
   * @return boolean indicating if license is active.
   */
  public boolean isActiveAndNotExpired() {
    return isActive() && !licensedStatusExpires.isBefore(LocalDate.now());
  }
}