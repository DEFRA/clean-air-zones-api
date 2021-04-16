package uk.gov.caz.vcc.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.Value;
import org.springframework.http.HttpStatus;
import uk.gov.caz.async.rest.AsyncOp;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.vcc.dto.DvlaVehiclesInformation;
import uk.gov.caz.vcc.dto.SingleDvlaVehicleData;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;

/**
 * Interface to call NTR and DVLA API.
 */
public interface LicenseAndVehicleRepository {

  /**
   * Method to call ntr and dvla api for single vrn.
   *
   * @param vrn used to call api
   * @param authenticationToken needed to call dvla api
   * @return {@link NtrAndDvlaData}
   */
  NtrAndDvlaData findLicenseAndVehicle(String vrn, String authenticationToken);

  /**
   * Method to call ntr and dvla api for multiple vrns.
   *
   * @param vrns used to call api
   * @param authenticationToken needed to call dvla api
   * @return {@link NtrAndDvlaData}
   */
  NtrAndDvlaData findLicenseAndVehicle(List<String> vrns, String authenticationToken);

  /**
   * Load vehicles information from dvla api.
   *
   * @param vrns list of vehicle registration number
   * @param authenticationToken needed to call dvla api
   * @return {@link NtrAndDvlaData}
   */
  NtrAndDvlaData findVehicles(List<String> vrns, String authenticationToken);

  /**
   * Loads vehicle information from dvla api.
   *
   * @return {@link SingleDvlaVehicleData}
   */
  SingleDvlaVehicleData findDvlaVehicle(String vrn, String authenticationToken);

  /**
   * For specified list of VRNs returns {@link DvlaVehiclesInformation} instance that holds {@link
   * Vehicle} or error details (if network failure) for each VRN.
   *
   * @param vrns list of vehicle registration numbers.
   */
  DvlaVehiclesInformation findDvlaVehiclesInBulk(Collection<String> vrns);

  static long defaultTimeoutInSeconds() {
    return 20L;
  }

  /**
   * DTO helper class to transport async ops/calls to NTR and DVLA.
   */
  @Value
  @AllArgsConstructor
  @Builder
  class NtrAndDvlaData {

    /**
     * Map transporting {@link AsyncOp} instances containing
     * {@link TaxiPhvLicenseInformationResponse}.
     */
    @Singular
    Map<String, AsyncOp<TaxiPhvLicenseInformationResponse>> ntrLicences;

    /**
     * Map transporting {@link AsyncOp} instances containing {@link Vehicle}.
     */
    @Singular
    Map<String, AsyncOp<Vehicle>> dvlaVehicles;

    /**
     * Helper class that provides data for NTR or DVLA async call response.
     *
     * @param <T> Defines type of result object.
     */
    @Value
    public static class Data<T> {

      @Getter(AccessLevel.NONE)
      boolean hasError;
      HttpStatus httpStatus;
      T result;
      String error;

      /**
       * Returns true if response did not succeed.
       *
       * @return true if response did not succeed. In that case 'error' has details and result is
       *     null.
       */
      public boolean hasError() {
        return hasError;
      }
    }

    /**
     * Gets {@link Data} for NTR call for specified VRN.
     *
     * @param vrn VRN for which NTR call has been made.
     * @return {@link Data} for NTR call for specified VRN.
     */
    public Data<TaxiPhvLicenseInformationResponse> ntrFor(String vrn) {
      AsyncOp<TaxiPhvLicenseInformationResponse> ntrForVrn = ntrLicences.get(vrn);
      return new Data(ntrForVrn.hasError(), ntrForVrn.getHttpStatus(),
          ntrForVrn.hasError() ? null : ntrForVrn.getResult(),
          ntrForVrn.hasError() ? ntrForVrn.getError() : null);
    }

    /**
     * Gets {@link Data} for DVLA call for specified VRN.
     *
     * @param vrn VRN for which DVLA call has been made.
     * @return {@link Data} for DVLA call for specified VRN.
     */
    public Data<Vehicle> dvlaFor(String vrn) {
      AsyncOp<Vehicle> dvlaForVrn = dvlaVehicles.get(vrn);
      return new Data(dvlaForVrn.hasError(), dvlaForVrn.getHttpStatus(),
          dvlaForVrn.hasError() ? null : dvlaForVrn.getResult(),
          dvlaForVrn.hasError() ? dvlaForVrn.getError() : null);
    }

    /**
     * Gets list of {@link AsyncOp} calls to NTR.
     *
     * @return list of {@link AsyncOp} calls to NTR.
     */
    List<AsyncOp<TaxiPhvLicenseInformationResponse>> ntrAsyncOps() {
      return ntrLicences.values().stream().collect(Collectors.toList());
    }

    /**
     * Gets list of {@link AsyncOp} calls to DVLA.
     *
     * @return list of {@link AsyncOp} calls to DVLA.
     */
    List<AsyncOp<Vehicle>> dvlaAsyncOps() {
      return dvlaVehicles.values().stream().collect(Collectors.toList());
    }
  }
}
