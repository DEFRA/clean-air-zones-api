package uk.gov.caz.taxiregister.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class TaxiPhvVehicleLicence implements Serializable {

  private static final long serialVersionUID = -1697344242305227788L;

  Integer id;

  UUID uploaderId;

  @ToString.Exclude
  @NonNull
  String vrm;

  @NonNull
  LicenseDates licenseDates;

  @NonNull
  String description;

  @NonNull
  LicensingAuthority licensingAuthority;

  @NonNull
  String licensePlateNumber;

  Boolean wheelchairAccessible;

  LocalDateTime addedTimestamp;

  /**
   * Checks whether this licence is valid, i.e. {@code startDate() <= now <= endDate()}
   *
   * @return true if this licence is valid, false otherwise
   */
  public boolean isActive() {
    Today today = new Today();
    LicenseDates dates = getLicenseDates();

    return today.isAfterOrEqual(dates.getStart())
        && today.isBeforeOrEqual(dates.getEnd());
  }

  private static class Today {

    private final LocalDate now = LocalDate.now();

    private boolean isAfterOrEqual(LocalDate date) {
      // !(now < date) <=> now >= date
      return !now.isBefore(date);
    }

    private boolean isBeforeOrEqual(LocalDate date) {
      // !(now > date) <=> now <= date
      return !now.isAfter(date);
    }
  }
}
