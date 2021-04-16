package uk.gov.caz.vcc.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.verify.VerificationTimes.once;

import java.util.Collection;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import uk.gov.caz.definitions.dto.InformationUrlsDto;
import uk.gov.caz.vcc.annotation.IntegrationTest;
import uk.gov.caz.vcc.util.MockServerTestIT;

@IntegrationTest
public class TariffDetailsRepositoryTestIT extends MockServerTestIT {

  @Autowired
  protected CacheManager cacheManager;

  private static final String FIRST_RESPONSE = "caz-first-response.json";

  private static final String SECOND_RESPONSE = "caz-second-response.json";

  @Autowired
  private TariffDetailsRepository tariffRepository;

  @AfterEach
  public void clear() {
    cacheEvictCleanAirZones();
  }

  @Test
  public void shouldCacheAndEvictCleanAirZonesFromTariffService() {
    atTheStartCacheShouldBeEmpty();

    whenGetResultFromTariffService();

    thenTariffServiceShouldBeCalledOnce();
    thenCacheShouldBeNotEmpty();
    thenShouldGetResultFromCache();

    whenCacheEvict();

    thenCacheShouldBeEmpty();
    thenShouldGetNewResultFromTariffService();
    thenCacheShouldBeNotEmpty();
  }

  @Test
  public void shouldNotThrowExceptionWhenCacheIsEmpty() {
    atTheStartCacheShouldBeEmpty();

    thenShouldCacheEvict();
  }

  @Test
  public void shouldReturnTariffWithUrls() {
    given()
        .mockTariffResponseWithUrls()
        .whenCallTariffForDetails()

        .then()
        .hasBoundary(
            "https://www.birmingham.gov.uk/info/20076/pollution/1763/a_clean_air_zone_for_birmingham/3")
        .hasBecomeCompliant(
            "https://www.birmingham.gov.uk/info/20076/pollution/1763/a_clean_air_zone_for_birmingham/7")
        .hasAdditionalInfo("https://www.brumbreathes.co.uk/what-does-it-mean-for-me")
        .hasPublicTransportOptions("https://www.brumbreathes.co.uk/info/15/drive-work-clean-air-zone-1/9/drive-work-clean-air-zone/4")
        .hasExemptionOrDiscount(
            "https://www.birmingham.gov.uk/info/20076/pollution/1763/a_clean_air_zone_for_birmingham/6")
        .hasMainInfo(
            "https://www.birmingham.gov.uk/info/20076/pollution/1763/a_clean_air_zone_for_birmingham");
  }

  private void thenCacheShouldBeNotEmpty() {
    Collection<String> cacheNames = cacheManager.getCacheNames();
    assertThat(cacheNames.contains("cleanAirZones")).isTrue();
  }

  private void whenGetResultFromTariffService() {
    mockFirstResponse();
    assertThat(tariffRepository.getCleanAirZoneListing().getCleanAirZones()).hasSize(1);
  }

  private void thenShouldGetResultFromCache() {
    mockSecondResponse();
    assertThat(tariffRepository.getCleanAirZoneListing().getCleanAirZones()).hasSize(1);
  }

  private void thenShouldGetNewResultFromTariffService() {
    mockSecondResponse();
    assertThat(tariffRepository.getCleanAirZoneListing().getCleanAirZones()).hasSize(2);
  }

  private void mockFirstResponse() {
    prepareResponseFromTariff(mockServer, FIRST_RESPONSE);
  }

  private void mockSecondResponse() {
    clearTariffExpectations();
    prepareResponseFromTariff(mockServer, SECOND_RESPONSE);
  }

  private void clearTariffExpectations() {
    mockServer.clear(requestGet("/v1/clean-air-zones"));
  }

  private void prepareResponseFromTariff(ClientAndServer mockServer, String responseFile) {
    mockServer.when(requestGet("/v1/clean-air-zones"))
        .respond(response(responseFile));
  }

  private void atTheStartCacheShouldBeEmpty() {
    thenCacheShouldBeEmpty();
  }

  private void thenCacheShouldBeEmpty() {
    Cache cleanAirZones = cacheManager.getCache("cleanAirZones");
    assertThat(cleanAirZones.get("cleanAirZones")).isNull();
  }

  private void whenCacheEvict() {
    cacheEvictCleanAirZones();
  }

  private void thenShouldCacheEvict() {
    cacheEvictCleanAirZones();
  }

  private void cacheEvictCleanAirZones() {
    tariffRepository.cacheEvictCleanAirZones();
  }

  private void thenTariffServiceShouldBeCalledOnce() {
    mockServer.verify(requestGet("/v1/clean-air-zones"), once());
  }

  private TariffDetailsAssertion given() {
    return new TariffDetailsAssertion(tariffRepository);
  }

  @RequiredArgsConstructor
  static class TariffDetailsAssertion {

    public static final UUID CLEAN_AIR_ZONE_ID = UUID
        .fromString("5cd7441d-766f-48ff-b8ad-1809586fea37");
    private final TariffDetailsRepository tariffRepository;

    InformationUrlsDto informationUrls;

    public TariffDetailsAssertion hasMainInfo(String expectedMainInfo) {
      assertThat(informationUrls.getMainInfo()).isEqualTo(expectedMainInfo);
      return this;
    }

    public TariffDetailsAssertion hasExemptionOrDiscount(String expectedExemptionOrDiscount) {
      assertThat(informationUrls.getExemptionOrDiscount()).isEqualTo(expectedExemptionOrDiscount);
      return this;
    }

    public TariffDetailsAssertion hasBecomeCompliant(String expectedBecomeCompliant) {
      assertThat(informationUrls.getBecomeCompliant()).isEqualTo(expectedBecomeCompliant);
      return this;
    }

    public TariffDetailsAssertion hasBoundary(String expectedBoundary) {
      assertThat(informationUrls.getBoundary()).isEqualTo(expectedBoundary);
      return this;
    }

    public TariffDetailsAssertion hasAdditionalInfo(String expectedAdditionalInfo) {
      assertThat(informationUrls.getAdditionalInfo()).isEqualTo(expectedAdditionalInfo);
      return this;
    }

    public TariffDetailsAssertion hasPublicTransportOptions(String expectedPublicTransportOptions) {
      assertThat(informationUrls.getPublicTransportOptions()).isEqualTo(expectedPublicTransportOptions);
      return this;
    }

    private TariffDetailsAssertion mockTariffResponseWithUrls() {
      mockServer.when(requestGet("/v1/clean-air-zones/" + CLEAN_AIR_ZONE_ID + "/tariff"))
          .respond(response("tariff-rates-third-response.json"));
      return this;
    }

    private TariffDetailsAssertion whenCallTariffForDetails() {
      informationUrls = tariffRepository.getTariffDetails(CLEAN_AIR_ZONE_ID).get()
          .getInformationUrls();
      return this;
    }

    private TariffDetailsAssertion then() {
      return this;
    }
  }
}