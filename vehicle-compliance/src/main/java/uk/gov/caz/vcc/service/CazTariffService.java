package uk.gov.caz.vcc.service;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.gov.caz.vcc.domain.TariffDetails;
import uk.gov.caz.vcc.dto.CleanAirZonesDto;
import uk.gov.caz.vcc.repository.TariffDetailsRepository;

@Service
public class CazTariffService {

  private final TariffDetailsRepository tariffRepository;

  /**
   * Default constructor.
   *
   * @param tariffRepository Repository layer implementation for retrieving
   *                         tariff details from a remote API.
   */
  public CazTariffService(TariffDetailsRepository tariffRepository) {
    this.tariffRepository = tariffRepository;
  }

  /**
   * Service layer implementation for retrieving clean air zone listings.
   *
   * @return a summary list of all clean air zones and their boundary URLs.
   */
  public CleanAirZonesDto getCleanAirZoneSelectionListings() {
    return tariffRepository.getCleanAirZoneListing();
  }

  /**
   * Service layer implementation for retrieving tariffs for give clean air zone.
   *
   * @param cazId UUID of Clear Air ZONE
   *
   * @return list of tariffs fir given CAZ
   */
  public Optional<TariffDetails> getTariffDetailsForGivenCazId(UUID cazId) {
    return tariffRepository.getTariffDetails(cazId);
  }

  /**
   * Service layer implementation for evicting a cached clean-air-zones from
   * redis.
   */
  public void cacheEvictCleanAirZones() {
    tariffRepository.cacheEvictCleanAirZones();
  }
}
