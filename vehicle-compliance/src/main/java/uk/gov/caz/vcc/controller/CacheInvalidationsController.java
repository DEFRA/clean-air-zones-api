package uk.gov.caz.vcc.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.vcc.dto.VrmsDto;
import uk.gov.caz.vcc.service.CazTariffService;
import uk.gov.caz.vcc.service.NationalTaxiRegisterService;
import uk.gov.caz.vcc.service.VehicleService;

/**
 * Rest Controller with endpoints related to cache invalidations.
 */
@RestController
@AllArgsConstructor
public class CacheInvalidationsController implements CacheInvalidationsControllerApiSpec {

  public static final String CACHE_INVALIDATION_PATH = "/v1/cache-invalidations";

  private final CazTariffService cazTariffService;

  private final NationalTaxiRegisterService nationalTaxiRegisterService;

  private final VehicleService vehicleService;
  
  @Override
  public ResponseEntity<Void> cacheEvictCleanAirZones() {
    cazTariffService.cacheEvictCleanAirZones();
    return ResponseEntity.accepted().build();
  }

  @Override
  public ResponseEntity<Void> cacheEvictLicences(@RequestBody VrmsDto vrms) {
    nationalTaxiRegisterService.cacheEvictLicenses(vrms.getVrms());
    return ResponseEntity.accepted().build();
  }

  @Override
  public ResponseEntity<Void> cacheEvictVehicles() {
    vehicleService.cacheEvictVehicles();
    return ResponseEntity.accepted().build();
  }
}