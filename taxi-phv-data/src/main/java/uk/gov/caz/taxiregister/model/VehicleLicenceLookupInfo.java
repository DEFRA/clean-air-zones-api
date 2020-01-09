package uk.gov.caz.taxiregister.model;

import com.google.common.base.Preconditions;

import java.io.Serializable;
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

  Boolean wheelchairAccessible;

  private List<String> licensingAuthoritiesNames;

  private VehicleLicenceLookupInfo(boolean hasAnyOperatingLicenceActive,
      Boolean wheelchairAccessible, List<String> licensingAuthoritiesNames) {
    Preconditions.checkArgument(
        hasAnyOperatingLicenceActive || !Boolean.TRUE.equals(wheelchairAccessible),
        "Cannot have inactive operating licence with wheelchair accessible flag set to true"
    );
    this.hasAnyOperatingLicenceActive = hasAnyOperatingLicenceActive;
    this.wheelchairAccessible = wheelchairAccessible;
    this.licensingAuthoritiesNames = licensingAuthoritiesNames;
  }

  public boolean hasAnyOperatingLicenceActive() {
    return hasAnyOperatingLicenceActive;
  }
}
