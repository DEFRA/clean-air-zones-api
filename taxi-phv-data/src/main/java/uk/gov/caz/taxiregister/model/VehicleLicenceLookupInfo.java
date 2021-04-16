package uk.gov.caz.taxiregister.model;

import com.google.common.base.Preconditions;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Value
@Builder
public class VehicleLicenceLookupInfo implements Serializable {

  /**
   * Serialization UID for redis.
   */
  private static final long serialVersionUID = 6766258509541203200L;

  @Getter(AccessLevel.NONE)
  boolean hasAnyOperatingLicenceActive;
  
  String description;

  Boolean wheelchairAccessible;

  LocalDate licensedStatusExpires;

  LocalDateTime addedTimestamp;

  private List<String> licensingAuthoritiesNames;

  private VehicleLicenceLookupInfo(boolean hasAnyOperatingLicenceActive, String description,
      Boolean wheelchairAccessible, LocalDate licenceStatusExpires, LocalDateTime addedTimestamp,
      List<String> licensingAuthoritiesNames) {
    Preconditions.checkArgument(
        hasAnyOperatingLicenceActive || !Boolean.TRUE.equals(wheelchairAccessible),
        "Cannot have inactive operating licence with wheelchair accessible flag set to true"
    );
    this.hasAnyOperatingLicenceActive = hasAnyOperatingLicenceActive;
    this.description = description;
    this.wheelchairAccessible = wheelchairAccessible;
    this.licensedStatusExpires = licenceStatusExpires;
    this.addedTimestamp = addedTimestamp;
    this.licensingAuthoritiesNames = licensingAuthoritiesNames;
  }

  public boolean hasAnyOperatingLicenceActive() {
    return hasAnyOperatingLicenceActive;
  }
}
