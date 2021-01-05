package uk.gov.caz.accounts.model;

import java.util.UUID;
import lombok.ToString;
import lombok.Value;

/**
 * Simple DTO class that acts as a tuple for Account Vehicle ID and VRN.
 */
@Value
public class AccountVehicleBare {

  /**
   * Constructs new instance of {@link AccountVehicleBare} class.
   *
   * @param accountVehicleIdAsString Account Vehicle ID as string.
   * @param vrn Vehicle VRN.
   * @return Newly created and initialized {@link AccountVehicleBare} instance.
   */
  public static AccountVehicleBare from(String accountVehicleIdAsString, String vrn) {
    return new AccountVehicleBare(UUID.fromString(accountVehicleIdAsString), vrn);
  }

  /**
   * Account Vehicle ID.
   */
  UUID accountVehicleId;

  /**
   * Vehicle VRN.
   */
  @ToString.Exclude
  String vrn;
}
