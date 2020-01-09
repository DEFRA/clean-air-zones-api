package uk.gov.caz.vcc.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.verify.VerificationTimes.atLeast;
import static org.mockserver.verify.VerificationTimes.once;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import uk.gov.caz.vcc.annotation.IntegrationTest;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;
import uk.gov.caz.vcc.util.MockServerTestIT;

@IntegrationTest
public class NationalTaxiRegisterRepositoryTestIT extends MockServerTestIT {

  private static final String FIRST_RESPONSE = "ntr-first-response.json";

  private static final String SECOND_RESPONSE = "ntr-second-response.json";

  private static final String FIRST_VRN = "SW61BYD";

  public static final String LICENCE_INFO_FOR_FIRST_VRN_URL =
      "/v1/vehicles/" + FIRST_VRN + "/licence-info";

  private static final String SECOND_VRN = "MK16YZR";

  public static final String LICENCE_INFO_FOR_SECOND_VRN_URL =
      "/v1/vehicles/" + SECOND_VRN + "/licence-info";

  @Autowired
  protected CacheManager cacheManager;

  @Autowired
  private NationalTaxiRegisterRepository ntrRepository;

  @AfterEach
  public void clear() {
    cacheEvictLicenses();
    mockServer.reset();
  }

  @Test
  public void shouldCacheLicencesFromNationalTaxiRegister() {
    atTheStartCacheShouldBeEmpty();

    whenGetLicenceFromNationalTaxiRegisterWithFirstVrn();

    thenNationalTaxiRegisterShouldBeCalledOnceWithFirstVrn();
    thenCacheShouldBeNotEmptyForFirstVrn();
    thenGetFirstLicenceFromCache();
    thenNumberOfCallShouldBeTheSameWithFirstVrn();

    whenGetLicenceFromNationalTaxiRegisterWithSecondVrn();

    thenNationalTaxiRegisterShouldBeCalledOnceWithSecondVrn();
    thenCacheShouldBeNotEmptyForSecondVrn();
    thenGetSecondLicenceFromCache();
    thenNumberOfCallShouldBeTheSameWithSecondVrn();
  }

  @Test
  public void shouldCacheAndEvictLicencesFromNationalTaxiRegister() {
    atTheStartCacheShouldBeEmpty();

    whenGetLicenceFromNationalTaxiRegisterWithFirstVrn();

    thenNationalTaxiRegisterShouldBeCalledOnceWithFirstVrn();
    thenCacheShouldBeNotEmptyForFirstVrn();
    thenEvictCache();
    thenCacheShouldBeEmpty();
    thenGetFirstLicenceFromNationalTaxiRegisterWithFirstVrn();
    thenNationalTaxiRegisterShouldBeCalledTwiceWithFirstVrn();

    whenGetLicenceFromNationalTaxiRegisterWithSecondVrn();

    thenNationalTaxiRegisterShouldBeCalledOnceWithSecondVrn();
    thenCacheShouldBeNotEmptyForSecondVrn();
    thenEvictCache();
    thenCacheShouldBeEmpty();
    thenGetFirstLicenceFromNationalTaxiRegisterWithSecondVrn();
    thenNationalTaxiRegisterShouldBeCalledTwiceWithSecondVrn();
  }

  @Test
  public void shouldNotThrowExceptionWhenCacheIsEmpty() {
    atTheStartCacheShouldBeEmpty();

    thenEvictCache();
  }

  private TaxiPhvLicenseInformationResponse getLicenceWith(String firstVrn) {
    return ntrRepository.getLicenseInfo(firstVrn).get();
  }

  private void thenGetFirstLicenceFromNationalTaxiRegisterWithFirstVrn() {
    verifyBodyForFirstVrn(getLicenceWith(FIRST_VRN));
  }

  private void thenGetFirstLicenceFromNationalTaxiRegisterWithSecondVrn() {
    verifyBodyForSecondVrn(getLicenceWith(SECOND_VRN));
  }

  private void verifyBodyForFirstVrn(TaxiPhvLicenseInformationResponse licenseInfo) {
    assertThat(licenseInfo.isActive()).isTrue();
    assertThat(licenseInfo.getWheelchairAccessible()).isTrue();
  }

  private void verifyBodyForSecondVrn(TaxiPhvLicenseInformationResponse licenseInfo) {
    assertThat(licenseInfo.isActive()).isFalse();
    assertThat(licenseInfo.getWheelchairAccessible()).isTrue();
  }

  private void whenGetLicenceFromNationalTaxiRegisterWithFirstVrn() {
    mockFirstResponse();
    verifyBodyForFirstVrn(getLicenceWith(FIRST_VRN));
  }

  private void whenGetLicenceFromNationalTaxiRegisterWithSecondVrn() {
    mockSecondResponse();
    verifyBodyForSecondVrn(getLicenceWith(SECOND_VRN));
  }

  private void thenGetFirstLicenceFromCache() {
    verifyBodyForFirstVrn(getLicenceWith(FIRST_VRN));
  }

  private void thenGetSecondLicenceFromCache() {
    verifyBodyForSecondVrn(getLicenceWith(SECOND_VRN));
  }

  private void mockFirstResponse() {
    prepareResponseFromNtr(mockServer, FIRST_RESPONSE, FIRST_VRN);
  }

  private void mockSecondResponse() {
    prepareResponseFromNtr(mockServer, SECOND_RESPONSE, SECOND_VRN);
  }

  private void prepareResponseFromNtr(ClientAndServer mockServer, String responseFile,
      String vrn) {
    mockServer.when(requestGet("/v1/vehicles/" + vrn + "/licence-info"))
        .respond(response(responseFile));
  }

  private void thenNumberOfCallShouldBeTheSameWithFirstVrn() {
    thenNationalTaxiRegisterShouldBeCalledOnceWithFirstVrn();
  }

  private void thenNumberOfCallShouldBeTheSameWithSecondVrn() {
    thenNationalTaxiRegisterShouldBeCalledOnceWithSecondVrn();
  }

  private void thenNationalTaxiRegisterShouldBeCalledOnceWithFirstVrn() {
    verifyRequest(LICENCE_INFO_FOR_FIRST_VRN_URL, once());
  }

  private void thenNationalTaxiRegisterShouldBeCalledOnceWithSecondVrn() {
    verifyRequest(LICENCE_INFO_FOR_SECOND_VRN_URL, once());
  }

  private void thenNationalTaxiRegisterShouldBeCalledTwiceWithFirstVrn() {
    verifyRequest(LICENCE_INFO_FOR_FIRST_VRN_URL, atLeast(2));
  }

  private void thenNationalTaxiRegisterShouldBeCalledTwiceWithSecondVrn() {
    verifyRequest(LICENCE_INFO_FOR_SECOND_VRN_URL, atLeast(2));
  }

  private void verifyRequest(String licenceInfoForFirstVrnUrl, VerificationTimes verificationTimes) {
    mockServer.verify(requestGet(licenceInfoForFirstVrnUrl), verificationTimes);
  }

  private void cacheEvictLicenses() {
    ntrRepository.cacheEvictLicenseInfo();
  }

  private void thenCacheShouldBeNotEmptyForFirstVrn() {
    Cache licenseInfo = getLicenseInfoCache();
    assertThat(licenseInfo.get(FIRST_VRN)).isNotNull();

    TaxiPhvLicenseInformationResponse actual = licenseInfo.get(FIRST_VRN, TaxiPhvLicenseInformationResponse.class);
    assertThat(actual.isActive()).isTrue();
    assertThat(actual.getWheelchairAccessible()).isTrue();
    assertThat(actual.getLicensingAuthoritiesNames()).contains("la-1", "la-2");
  }

  private void thenCacheShouldBeNotEmptyForSecondVrn() {
    Cache licenseInfo = getLicenseInfoCache();
    assertThat(licenseInfo.get(SECOND_VRN)).isNotNull();

    TaxiPhvLicenseInformationResponse actual = licenseInfo.get(SECOND_VRN, TaxiPhvLicenseInformationResponse.class);
    assertThat(actual.isActive()).isFalse();
    assertThat(actual.getWheelchairAccessible()).isTrue();
    assertThat(actual.getLicensingAuthoritiesNames()).contains("la-2", "la-3");
  }

  private void atTheStartCacheShouldBeEmpty() {
    thenCacheShouldBeEmpty();
  }

  private void thenCacheShouldBeEmpty() {
    Cache licenseInfo = getLicenseInfoCache();
    assertThat(licenseInfo.get(FIRST_VRN)).isNull();
    assertThat(licenseInfo.get(SECOND_VRN)).isNull();
  }

  private Cache getLicenseInfoCache() {
    return cacheManager.getCache("licenseInfo");
  }

  private void thenEvictCache() {
    cacheEvictLicenses();
  }
}