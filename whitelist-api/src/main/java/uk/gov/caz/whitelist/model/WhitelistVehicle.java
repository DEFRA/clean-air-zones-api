package uk.gov.caz.whitelist.model;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Entity which contain vehicles identified by vrn, which are on whitelist.
 */
@Value
@Builder(toBuilder = true)
public class WhitelistVehicle {

  /**
   * A timestamp when record was updated.
   */
  LocalDateTime updateTimestamp;

  /**
   * A timestamp when record was insert.
   */
  LocalDateTime insertTimestamp;

  /**
   * Reason which describes why record was created.
   */
  String reasonUpdated;

  /**
   * A company, which build vehicle, i.e. Fiat, Audi
   */
  String manufacturer;

  /**
   * A category of vehicle.
   */
  String category;

  /**
   * The unique whitelist identifier.
   */
  String vrn;

  /**
   * Id of person who update record.
   */
  UUID uploaderId;

  /**
   * Email of person who update record.
   */
  String uploaderEmail;

  /**
   * Specify if vehicle is exempt.
   */
  boolean exempt;

  /**
   * Specify if vehicle is compliant.
   */
  boolean compliant;

  /**
   * Getter which returns Optional value, as manufacturer may be nullable.
   *
   * @return {@link Optional} value of manufacturer.
   */
  public Optional<String> getManufacturer() {
    return Optional.ofNullable(manufacturer);
  }
}
