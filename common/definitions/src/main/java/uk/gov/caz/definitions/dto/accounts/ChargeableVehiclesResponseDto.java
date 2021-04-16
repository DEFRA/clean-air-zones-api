package uk.gov.caz.definitions.dto.accounts;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.definitions.dto.accounts.VehiclesResponseDto.VehicleWithCharges;

/**
 * Class that represents the JSON structure for chargeable vehicle retrieval response, i.e. {@code
 * /v1/accounts/:accountId/vehicles/sorted-page} endpoint.
 */
@Value
@Builder
public class ChargeableVehiclesResponseDto {

  /**
   * The list of chargeable vehicles associated with the account ID provided in the request.
   */
  List<VehicleWithCharges> vehicles;

  /**
   * The total number of vehicles that are associated with the account.
   */
  long totalVehiclesCount;

}
