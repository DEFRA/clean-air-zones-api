package uk.gov.caz.vcc.service;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.caz.vcc.dto.DvlaVehiclesInformation;
import uk.gov.caz.vcc.dto.LicencesInformation;
import uk.gov.caz.vcc.dto.NtrAndDvlaVehicleData;
import uk.gov.caz.vcc.dto.SingleDvlaVehicleData;
import uk.gov.caz.vcc.dto.SingleLicenceData;
import uk.gov.caz.vcc.repository.LicenseAndVehicleRepository;

/**
 * Stateful class that populates NTR Licences and DVLA Vehicles upon prefetch
 * request using the bulk APIs and caches the results. Note that the prototype
 * scope is used as this method can be accessed both from web requests (VEI), or
 * in handling a CSV bulk check (which has no web application context).
 */
@RequiredArgsConstructor
@Scope("prototype")
@Service
@Slf4j
public class LicenseAndVehicleProvider {

  private final LicenseAndVehicleRepository licenseAndVehicleRepository;
  private final NationalTaxiRegisterService nationalTaxiRegisterService;

  private LicencesInformation licencesInformation;
  private DvlaVehiclesInformation dvlaVehiclesInformation;

  private boolean prefetched;

  /**
   * Initiates prefetch operation for Licences and DVLA details for specified collection of VRNs.
   * This method must be called before {@code findLicenseAndVehicle} and implementation should
   * validate that.
   *
   * @param vrns Collection of VRNs to prefetch licences and DVLA data.
   */
  public void prefetch(Collection<String> vrns) {
    log.info("Trying to call NTR and DVLA in parallel for VRN list size of {}.", vrns.size());
    final CompletableFuture<LicencesInformation> licencesFuture = CompletableFuture.supplyAsync(
        () -> nationalTaxiRegisterService.getLicensesInformation(vrns)
    );
    final CompletableFuture<DvlaVehiclesInformation> dvlaFuture = CompletableFuture.supplyAsync(
        () -> licenseAndVehicleRepository.findDvlaVehiclesInBulk(vrns)
    );
    final CompletableFuture<Void> twoCalls = CompletableFuture
        .allOf(licencesFuture, dvlaFuture);
    twoCalls.thenRun(() -> {
      try {
        this.licencesInformation = licencesFuture.get();
        this.dvlaVehiclesInformation = dvlaFuture.get();
        this.prefetched = true;
      } catch (InterruptedException | ExecutionException e) {
        log.error("Parallel calls to DVLA and NTR failed");
        log.error(e.getMessage());
      }
    });
    twoCalls.join();
    log.info("Calling DVLA and NTR done");
  }

  /**
   * Returns NTR and DVLA data for the passed vrn.
   */
  public NtrAndDvlaVehicleData findLicenseAndVehicle(String vrn) {
    if (!prefetched) {
      throw new IllegalStateException(
          "NTR and DVLA have not been prefetched. Please call 'prefetch(Collection of vrns)' "
              + "method before this one.");
    }
    return new NtrAndDvlaVehicleData(getCachedDvlaVehicleData(vrn), getCachedLicenceInfo(vrn));
  }

  /**
   * Gets prefetched DVLA Vehicle.
   */
  private SingleDvlaVehicleData getCachedDvlaVehicleData(String vrn) {
    if (dvlaVehiclesInformation.hasFailed()) {
      return SingleDvlaVehicleData
          .failure(HttpStatus.SERVICE_UNAVAILABLE, "DVLA Service is unavailable");
    }
    return dvlaVehiclesInformation.getDvlaVehicleInfoFor(vrn);
  }

  /**
   * Gets prefetched NTR Licence.
   */
  private SingleLicenceData getCachedLicenceInfo(String vrn) {
    if (licencesInformation.hasFailed()) {
      return SingleLicenceData.failure(licencesInformation.getHttpStatus(),
          licencesInformation.getErrorMessage());
    }
    return SingleLicenceData.success(licencesInformation.getLicenceInfoFor(vrn));
  }
}