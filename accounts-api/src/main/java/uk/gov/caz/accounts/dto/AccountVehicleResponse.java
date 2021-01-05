package uk.gov.caz.accounts.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.accounts.model.AccountVehicle;

/**
 * Class that represents JSON response after AccountVehicle creation.
 */
@Value
@Builder
public class AccountVehicleResponse {

  @ApiModelProperty(value = "${swagger.model.descriptions.account-vehicle.vrn}")
  String vrn;

  @ApiModelProperty(value = "${swagger.model.descriptions.account-vehicle.account-id")
  UUID accountId;

  /**
   * Creates {@link AccountVehicleResponse} object from passed {@AccountVehicle} object.
   */
  public static AccountVehicleResponse from(AccountVehicle accountVehicle) {
    return AccountVehicleResponse.builder()
        .accountId(accountVehicle.getAccountId())
        .vrn(accountVehicle.getVrn())
        .build();
  }
}
