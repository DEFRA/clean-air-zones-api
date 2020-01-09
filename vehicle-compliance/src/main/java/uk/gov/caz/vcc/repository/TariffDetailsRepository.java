package uk.gov.caz.vcc.repository;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.web.client.RestTemplate;
import uk.gov.caz.vcc.domain.exceptions.ExternalServiceCallException;
import uk.gov.caz.vcc.domain.CazClass;
import uk.gov.caz.vcc.domain.TariffDetails;
import uk.gov.caz.vcc.domain.VehicleType;
import uk.gov.caz.vcc.domain.VehicleTypeCharge;
import uk.gov.caz.vcc.dto.CleanAirZonesDto;
import uk.gov.caz.vcc.dto.RatesDto;
import uk.gov.caz.vcc.dto.TariffDto;

@Slf4j
@Repository
public class TariffDetailsRepository {

  private final RestTemplate tariffServiceRestTemplate;

  private String tariffServiceRootUri;

  /**
   * Default constructor for tariff repository.
   *
   * @param restTemplateBuilder Injected rest template builder
   * @param tariffServiceRootUri the remote URL to query drawn from the application's
   *     configuration file.
   */
  public TariffDetailsRepository(RestTemplateBuilder restTemplateBuilder,
      @Value("${services.tariff-service.root-url}") String tariffServiceRootUri) {
    this.tariffServiceRootUri = tariffServiceRootUri;
    this.tariffServiceRestTemplate = restTemplateBuilder.rootUri(tariffServiceRootUri).build();
  }

  /**
   * Method for retrieving a clean air zone "listing" from the Tariff API.
   *
   * @return a summary listing of a clean air zone including their identifiers and boundary urls.
   */
  @Cacheable(value = "cleanAirZones")
  public CleanAirZonesDto getCleanAirZoneListing() {
    log.info("Calling Tariff Service to get clean air zone listing "
        + "information from base API endpoint: {}", tariffServiceRootUri);

    try {
      ResponseEntity<CleanAirZonesDto> responseEntity = tariffServiceRestTemplate
          .getForEntity("/v1/clean-air-zones", CleanAirZonesDto.class);

      return responseEntity.getBody();
    } catch (Exception e) {
      log.error("Cannot fetch clean air zone listing information.");
      throw new ExternalServiceCallException(e);
    }
  }

  /**
   * Method for retrieving tariff details from the Tariff API.
   *
   * @param cleanAirZoneId the unique identifier of the zone
   * @return details of the clean air zone tariff.
   */
  @Cacheable(value = "tariffs")
  public Optional<TariffDetails> getTariffDetails(UUID cleanAirZoneId) {
    log.info("Calling Tariff Service to get full rate information for zone identifier '{}'",
        cleanAirZoneId);

    try {
      ResponseEntity<TariffDto> responseEntity = tariffServiceRestTemplate.getForEntity(
          "/v1/clean-air-zones/{cazIdentifer}/tariff", TariffDto.class, cleanAirZoneId);
      return Optional.of(mapFromTariffDto(responseEntity.getBody()));
    } catch (NotFound e) {
      log.warn("Cannot fetch full tariff information for zone identifier {}", cleanAirZoneId);
      return Optional.empty();
    } catch (Exception e) {
      log.error("Cannot call external service for caz id {}", cleanAirZoneId);
      throw new ExternalServiceCallException(e);
    }
  }

  /**
   * Method for evicting a cached clean-air-zones from redis.
   */
  @CacheEvict(value = { "cleanAirZones", "tariffs" }, allEntries = true)
  public void cacheEvictCleanAirZones() {
    log.debug("Evicting cached clean-air-zones.");
  }

  /**
   * Helper method to map inner domain object representation of a Tariff from a data transfer
   * object.
   *
   * @param tariff the external DTO object representation of a tariff.
   * @return A domain object representation of tariff details.
   */
  public static TariffDetails mapFromTariffDto(TariffDto tariff) {
    TariffDetails response = new TariffDetails();
    response.setCazId(tariff.getCleanAirZoneId());
    response.setName(tariff.getName());
    response.setChargesMotorcycles(tariff.isMotorcyclesChargeable());
    response.setTariff(CazClass.fromChar(tariff.getTariffClass()));
    response.setInformationUrls(tariff.getInformationUrls());
    response.setRates(mapRatesFromTariffDto(tariff));
    response.setChargeIdentifier(tariff.getChargeIdentifier());
    return response;
  }

  /**
   * Helper method for mapping between external tariff rate representations to a domain object.
   *
   * @param tariff the external Tariff definition.
   * @return an internal domain object representation of tariff rates.
   */
  private static ArrayList<VehicleTypeCharge> mapRatesFromTariffDto(TariffDto tariff) {
    RatesDto externalRates = tariff.getRates();
    ArrayList<VehicleTypeCharge> internalRates = new ArrayList<>();

    VehicleTypeCharge privateCar = new VehicleTypeCharge();
    privateCar.setVehicleType(VehicleType.PRIVATE_CAR);
    privateCar.setCharge(externalRates.getCar().floatValue());
    internalRates.add(privateCar);

    VehicleTypeCharge largeVan = new VehicleTypeCharge();
    largeVan.setVehicleType(VehicleType.LARGE_VAN);
    largeVan.setCharge(externalRates.getLargeVan().floatValue());
    internalRates.add(largeVan);

    VehicleTypeCharge smallVan = new VehicleTypeCharge();
    smallVan.setVehicleType(VehicleType.SMALL_VAN);
    smallVan.setCharge(externalRates.getSmallVan().floatValue());
    internalRates.add(smallVan);

    VehicleTypeCharge miniBus = new VehicleTypeCharge();
    miniBus.setVehicleType(VehicleType.MINIBUS);
    miniBus.setCharge(externalRates.getMiniBus().floatValue());
    internalRates.add(miniBus);

    VehicleTypeCharge taxi = new VehicleTypeCharge();
    taxi.setVehicleType(VehicleType.TAXI_OR_PHV);
    taxi.setCharge(externalRates.getTaxi().floatValue());
    internalRates.add(taxi);

    VehicleTypeCharge bus = new VehicleTypeCharge();
    bus.setVehicleType(VehicleType.BUS);
    bus.setCharge(externalRates.getBus().floatValue());
    internalRates.add(bus);

    VehicleTypeCharge coach = new VehicleTypeCharge();
    coach.setVehicleType(VehicleType.COACH);
    coach.setCharge(externalRates.getCoach().floatValue());
    internalRates.add(coach);

    VehicleTypeCharge motorCycle = new VehicleTypeCharge();
    motorCycle.setVehicleType(VehicleType.MOTORCYCLE);
    motorCycle.setCharge(externalRates.getMotorcycle().floatValue());
    internalRates.add(motorCycle);

    VehicleTypeCharge hgv = new VehicleTypeCharge();
    hgv.setVehicleType(VehicleType.HGV);
    hgv.setCharge(externalRates.getHgv().floatValue());
    internalRates.add(hgv);

    return internalRates;
  }
}
