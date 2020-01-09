package uk.gov.caz.vcc.repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import uk.gov.caz.async.rest.AsyncResponse;
import uk.gov.caz.async.rest.AsyncResponses;
import uk.gov.caz.vcc.domain.Vehicle;
import uk.gov.caz.vcc.domain.exceptions.ExternalServiceCallException;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;

/**
 * Interface to call NTR and dvla api.
 */
public interface LicenseAndVehicleRepository {

  /**
   * Method to call ntr and dvla api for single vrn.
   *
   * @param vrn                 used to call api
   * @param authenticationToken needed to call dvla api
   * @param correlationId
   * @return {@link LicenseAndVehicleResponse}
   */
  LicenseAndVehicleResponse findLicenseAndVehicle(String vrn, String authenticationToken,
      String correlationId);

  /**
   * Method to call ntr and dvla api for multiple vrns.
   *
   * @param vrns                used to call api
   * @param authenticationToken needed to call dvla api
   * @return {@link LicenseAndVehicleResponse}
   */
  LicenseAndVehicleResponse findLicenseAndVehicle(
      List<String> vrns, String authenticationToken,
      String correlationId);

  default <T> Map<String, AsyncResponse<T>> getResponse(AsyncResponses<T> responses, long timeout) {
    try {
      return responses.get(timeout, TimeUnit.SECONDS);
    } catch (Exception exception) {
      throw new ExternalServiceCallException(exception);
    }
  }

  static long calculateTimeout(List<String> vrns) {
    return vrns.size() == 1 ? 1L : 20L;
  }

  /**
   * DTO helper class to transport two maps.
   */
  @Value
  @AllArgsConstructor
  @Builder
  public class LicenseAndVehicleResponse {

    /**
     * Map to transport {@link AsyncResponse} containing {@link TaxiPhvLicenseInformationResponse}
     */
    @Singular
    Map<String, AsyncResponse<TaxiPhvLicenseInformationResponse>> licensInfos;

    /**
     * Map to transport {@link AsyncResponse} containing {@link Vehicle}
     */
    @Singular
    Map<String, AsyncResponse<Vehicle>> vehicles;
  }
}

