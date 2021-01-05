package uk.gov.caz.accounts.dto;

import static org.apache.logging.log4j.util.Strings.isNotBlank;

import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import java.util.function.Function;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.accounts.controller.exception.InvalidRequestPayloadException;
import uk.gov.caz.accounts.util.MapPreservingOrderBuilder;

/**
 * Class that represents incoming JSON payload for AccountVehicle creation.
 */
@Value
@Builder
public class AccountVehicleRequest {
  @ApiModelProperty(value = "${swagger.model.descriptions.account-vehicle.vrn}")
  String vrn;

  /**
   * Public method that validates given object and throws exceptions if validation doesn't pass.
   */
  private static final Map<Function<AccountVehicleRequest, Boolean>, String> validators =
      MapPreservingOrderBuilder.<Function<AccountVehicleRequest, Boolean>, String>builder()
          .put(request -> isNotBlank(request.vrn), "VRN cannot be blank")
          .put(request -> request.vrn.length() <= 15, "VRN is too long")
          .build();

  /**
   * Public method that validates given object and throws exceptions if validation doesn't pass.
   */
  public void validate() {
    validators.forEach((validator, message) -> {
      boolean isValid = validator.apply(this);

      if (!isValid) {
        throw new InvalidRequestPayloadException(message);
      }
    });
  }
}
