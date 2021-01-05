package uk.gov.caz.accounts.repository;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import uk.gov.caz.accounts.service.exception.ExternalServiceCallException;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;

/**
 * Retrofit2 repository to create a vccs call.
 */
@Repository
public interface VccsRepository {

  @Slf4j
  final class Logger {

  }

  /**
   * Method to create retrofit2 vccs for cleanAirZones call.
   *
   * @return {@link Call}
   */
  @Headers("Accept: application/json")
  @GET("v1/compliance-checker/clean-air-zones")
  Call<CleanAirZonesDto> findCleanAirZones();

  @Headers({"Accept: application/json", "Content-type: application/json"})
  @POST("v1/compliance-checker/vehicles/bulk-compliance")
  Call<List<ComplianceResultsDto>> findComplianceInBulk(@Body Set<String> vrns);

  /**
   * Wraps REST API call in {@link Response} making synchronous request.
   *
   * @return {@link Response} with REST response.
   */
  default Response<CleanAirZonesDto> findCleanAirZonesSync() {
    try {
      Logger.log.info("Begin: Fetching clean air zones from VCCS");
      return findCleanAirZones().execute();
    } catch (IOException e) {
      throw new ExternalServiceCallException(e.getMessage());
    } finally {
      Logger.log.info("End: Fetching clean air zones from VCCS");
    }
  }

  /**
   * Wraps REST API call for finding compliance in bulk in {@link Response} by making a synchronous
   * request.
   *
   * @return {@link Response} with REST response.
   */
  default Response<List<ComplianceResultsDto>> findComplianceInBulkSync(Set<String> vrns) {
    try {
      return findComplianceInBulk(vrns).execute();
    } catch (IOException e) {
      throw new ExternalServiceCallException(e.getMessage());
    } finally {
      Logger.log.info("End: Fetching compliance result from VCCS");
    }
  }
}
