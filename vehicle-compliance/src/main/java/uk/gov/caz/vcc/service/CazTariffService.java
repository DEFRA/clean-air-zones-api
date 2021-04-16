package uk.gov.caz.vcc.service;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.caz.definitions.dto.CleanAirZonesDto;
import uk.gov.caz.vcc.domain.TariffDetails;
import uk.gov.caz.vcc.repository.TariffDetailsRepository;

/**
 * Service layer implementation for reading clean air zone tariff details.
 */
@RequiredArgsConstructor
@Service
public class CazTariffService {

  private final TariffDetailsRepository tariffRepository;

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
   * @param cazId UUID of clean air zone
   *
   * @return list of tariffs for given CAZ
   */
  public Optional<TariffDetails> getTariffDetailsForGivenCazId(UUID cazId) {
    return tariffRepository.getTariffDetails(cazId);
  }

  /**
   * Service layer implementation for evicting cached clean air zones tariff details from
   * Redis.
   */
  public void cacheEvictCleanAirZones() {
    tariffRepository.cacheEvictCleanAirZones();
  }
}
