package uk.gov.caz.definitions.dto.accounts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Class that represents the JSON structure for vehicle retrieval response, i.e. {@code
 * /v1/accounts/:accountId/vehicles} endpoint.
 */
@Value
@Builder
public class VehiclesResponseDto {

  /**
   * The list of vehicles associated with the account ID provided in the request.
   */
  List<VehicleWithCharges> vehicles;

  /**
   * The total number of vehicles that are associated with the account.
   */
  long totalVehiclesCount;

  /**
   * The total number of pages that can be queried.
   */
  int pageCount;

  /**
   * Flag that tells whether ANY vehicle of the given account has undetermined charge (i.e. it
   * does not exist in DVLA repository)
   */
  boolean anyUndeterminedVehicles;

  @Value
  @Builder
  public static class VehicleWithCharges {

    /**
     * Vehicle registration number.
     */
    String vrn;

    /**
     * Flag indicating whether this vehicle is exempt.
     */
    @JsonProperty("isExempt")
    boolean exempt;

    /**
     * Flag indicating whether this vehicle is retrofitted.
     */
    @JsonProperty("isRetrofitted")
    boolean retrofitted;

    /**
     * Type of this vehicle. Nullable.
     */
    String vehicleType;

    /**
     * Cached compliance data for this vehicle. Can be empty.
     */
    List<VehicleCharge> cachedCharges;

    /**
     * Class containing this vehicle's compliance data.
     */
    @Value
    @Builder
    public static class VehicleCharge {

      /**
       * Clean Air Zone identifier.
       */
      UUID cazId;

      /**
       * Charge to pay by this vehicle for entering the CAZ.
       */
      BigDecimal charge;

      /**
       * Tariff identifier.
       */
      String tariffCode;
    }
  }
}
