package uk.gov.caz.vcc.controller;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.vcc.dto.VrmsDto;
import uk.gov.caz.vcc.service.CazTariffService;
import uk.gov.caz.vcc.service.NationalTaxiRegisterService;
import uk.gov.caz.vcc.service.VehicleService;

@ExtendWith(MockitoExtension.class)
class CacheInvalidationsControllerTest {

  @Mock
  private CazTariffService cazTariffService;

  @Mock
  private NationalTaxiRegisterService nationalTaxiRegisterService;

  @Mock
  private VehicleService vehicleService;
  
  @InjectMocks
  private CacheInvalidationsController cacheInvalidationsController;

  @Test
  public void shouldCacheEvictForCleanAirZones() {
    // when
    cacheInvalidationsController.cacheEvictCleanAirZones();

    // then
    verify(cazTariffService, times(1)).cacheEvictCleanAirZones();
  }

  @Test
  public void shouldCacheEvictForLicenses() {
    // given
    VrmsDto vrmsDto = VrmsDto.builder()
        .vrms(Lists.newArrayList("SW61BYD", "MK16YZR"))
        .build();

    // when
    cacheInvalidationsController.cacheEvictLicences(vrmsDto);

    // then
    verify(nationalTaxiRegisterService, times(1)).cacheEvictLicenses(vrmsDto.getVrms());
  }
  
  @Test
  public void shouldCacheEvictForVehicles() {
    // when
    cacheInvalidationsController.cacheEvictVehicles();

    // then
    verify(vehicleService, times(1)).cacheEvictVehicles();
  }
}