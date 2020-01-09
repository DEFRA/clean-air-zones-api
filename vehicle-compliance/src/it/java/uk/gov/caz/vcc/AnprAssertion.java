package uk.gov.caz.vcc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;
import static org.mockserver.matchers.Times.exactly;
import static uk.gov.caz.vcc.controller.VehicleEntrantsController.CAZ_ID;
import static uk.gov.caz.vcc.util.MockServerTestIT.response;
import static uk.gov.caz.vcc.util.MockServerTestIT.requestGet;
import static uk.gov.caz.vcc.util.MockServerTestIT.requestPost;
import static uk.gov.caz.vcc.util.VehicleResultDtoAssert.assertThat;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;
import org.mockserver.verify.VerificationTimes;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.vcc.domain.MilitaryVehicle;
import uk.gov.caz.vcc.dto.VehicleEntrantDto;
import uk.gov.caz.vcc.dto.VehicleResultDto;
import uk.gov.caz.vcc.repository.CleanAirZoneEntrantRepository;
import uk.gov.caz.vcc.repository.ModRepository;
import uk.gov.caz.vcc.service.NationalTaxiRegisterService;
import uk.gov.caz.vcc.service.VehicleEntrantsService;

@RequiredArgsConstructor
public class AnprAssertion {

  public static final DateTimeFormatter FORMATTER = DateTimeFormatter
      .ofPattern("yyyy-MM-dd'T'HHmmssX");

  public static final String SAMPLE_ENTRANT_DATE = "2017-10-01T155301Z";

  public static final UUID SAMPLE_CLEAN_AIR_ZONE = UUID
      .fromString("420b5cfb-cc9b-492b-bc07-5b77d380f72d");

  public final VehicleEntrantsService vehicleEntrantsService;

  public final ModRepository modRepository;

  public final NationalTaxiRegisterService nationalTaxiRegisterService;

  public final ClientAndServer mockServer;

  private final CleanAirZoneEntrantRepository cleanAirZoneEntrantRepository;

  List<VehicleResultDto> vehicleResultsResponse;

  String vrn;

  List<VehicleEntrantDto> vehicleEntrants = new ArrayList<>();

  private ValidatableResponse httpResponse;

  public AnprAssertion prepareVehicleEntrantsWithVrn(String vrn) {
    nationalTaxiRegisterService.cacheEvictLicenses(newArrayList(vrn));
    this.vrn = vrn;
    this.vehicleEntrants.add(new VehicleEntrantDto(vrn, SAMPLE_ENTRANT_DATE));
    return this;
  }

  public AnprAssertion whenCreateResponseToAnpr() {
    vehicleResultsResponse = vehicleEntrantsService
        .createVehicleResultsResponse(vehicleEntrants, SAMPLE_CLEAN_AIR_ZONE,
            UUID.randomUUID().toString());
    return this;
  }

  @SneakyThrows
  public AnprAssertion andCallVehicleEntrantsEndpoint(String file) {
    String payload = Resources.toString(Resources.getResource("data/json/" + file), Charsets.UTF_8);
    String correlationId = UUID.randomUUID().toString();

    httpResponse = RestAssured
        .given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId)
        .header(CAZ_ID, SAMPLE_CLEAN_AIR_ZONE)
        .body(payload)
        .when()
        .post("/v1/vehicle-entrants")
        .then()
        .header(Constants.X_CORRELATION_ID_HEADER, correlationId);
    return this;
  }

  public AnprAssertion then() {
    return this;
  }

  public AnprAssertion andResponse() {
    return this;
  }

  public ValidatableResponse andHttpResponse() {
    return httpResponse;
  }

  public AnprAssertion hasGivenVrn() {
    assertThat(vehicleResultsResponse).hasVrn(vrn);
    return this;
  }

  public AnprAssertion hasUnrecognisedNotPaidStatus() {
    assertThat(vehicleResultsResponse).hasStatus("unrecognisedNotPaid");
    return this;
  }

  public AnprAssertion hasUnrecognisedPaidStatus() {
    assertThat(vehicleResultsResponse).hasStatus("unrecognisedPaid");
    return this;
  }

  public AnprAssertion hasExemptStatus() {
    assertThat(vehicleResultsResponse).hasStatus("exempt");
    return this;
  }

  public AnprAssertion hasCompliantStatus() {
    assertThat(vehicleResultsResponse).hasStatus("compliant");
    return this;
  }

  public AnprAssertion hasNotCompliantNotPaidStatus() {
    assertThat(vehicleResultsResponse).hasStatus("notCompliantNotPaid");
    return this;
  }

  public AnprAssertion hasNotCompliantPaidStatus() {
    assertThat(vehicleResultsResponse).hasStatus("notCompliantPaid");
    return this;
  }

  public AnprAssertion tariffCodeIsNull() {
    assertThat(vehicleResultsResponse).tariffCodeIsNull();
    return this;
  }

  public AnprAssertion exemptionCodeIsNull() {
    assertThat(vehicleResultsResponse).exemptionCodeIsNull();
    return this;
  }

  public AnprAssertion hasModExemptionCode() {
    assertThat(vehicleResultsResponse).hasExemptionCode("WDC001");
    return this;
  }

  public AnprAssertion hasTariffCode(String tariffCode) {
    assertThat(vehicleResultsResponse).hasTariffCode(tariffCode);
    return this;
  }

  public AnprAssertion mockCompliantStatus() {
    mockTariffAndNtr(vrn);
    return this;
  }

  public AnprAssertion mockNotCompliantNotPaidStatusWithoutCallToPayments() {
    mockTariffAndNtr(vrn);
    return this;
  }

  public void mockTariffAndNtr(String notCompliantNotPaid) {
    mockTariffService();
    mockNtr(notCompliantNotPaid);
  }

  public AnprAssertion mockNotCompliantNotPaidStatusWithCallToPayments() {
    mockTariffAndNtr(vrn);
    mockPaymentServiceWithStatusNotPaid();
    return this;
  }

  public AnprAssertion mockExemptStatus() {
    mockMilitaryVehicle();
    return this;
  }

  public AnprAssertion mockNotCompliantPaidStatus() {
    mockTariffAndNtr(vrn);
    mockPaymentServiceWithStatusPaid();
    return this;
  }

  public AnprAssertion mockNotCompliantPaidStatus(String vrn) {
    mockTariffAndNtr(vrn);
    mockPaymentServiceWithStatusPaid();
    return this;
  }

  public AnprAssertion mockErrorFromNtr(String vrn) {
    mockServer.when(requestGet("/v1/vehicles/" + vrn + "/licence-info"), exactly(1))
        .respond(HttpResponse.response().withStatusCode(500));
    return this;
  }

  public AnprAssertion mockUnrecognisedNotPaidStatus() {
    mockPaymentServiceWithStatusNotPaidForNonUk();
    return this;
  }

  public AnprAssertion mockUnrecognisedPaidStatus() {
    mockPaymentServiceWithStatusPaidForNonUk();
    return this;
  }

  public void mockPaymentServiceWithStatusPaid() {
    mockServer
        .when(requestPost("/v1/payments/vehicle-entrants", "payment-status-second-request.json"),
            exactly(1))
        .respond(response("payment-status-paid-response.json"));
  }

  public void mockPaymentServiceWithStatusNotPaid() {
    mockServer
        .when(requestPost("/v1/payments/vehicle-entrants", "payment-status-second-request.json"),
            exactly(1))
        .respond(response("payment-status-not-paid-response.json"));
  }

  public void mockPaymentServiceWithStatusPaidForNonUk() {
    mockServer
        .when(requestPost("/v1/payments/vehicle-entrants", "payment-status-third-request.json"),
            exactly(1))
        .respond(response("payment-status-paid-response.json"));
  }

  public void mockPaymentServiceWithStatusNotPaidForNonUk() {
    mockServer
        .when(requestPost("/v1/payments/vehicle-entrants", "payment-status-third-request.json"),
            exactly(1))
        .respond(response("payment-status-not-paid-response.json"));
  }

  public void mockTariffService() {
    mockServer.when(requestGet("/v1/clean-air-zones/" + SAMPLE_CLEAN_AIR_ZONE + "/tariff"),
        exactly(1))
        .respond(response("tariff-rates-second-response.json"));
  }

  public void mockNtr(String vrn) {
    mockServer.when(requestGet("/v1/vehicles/" + vrn + "/licence-info"), exactly(1))
        .respond(response("ntr-second-response.json"));
  }

  public AnprAssertion shouldCallNtrOnce() {
    mockServer.verify(requestGet("/v1/vehicles/" + vrn + "/licence-info"),
        VerificationTimes.exactly(1));
    return this;
  }

  public AnprAssertion shouldCallNtrTwice() {
    mockServer.verify(requestGet("/v1/vehicles/" + vrn + "/licence-info"),
        VerificationTimes.exactly(2));
    return this;
  }


  public AnprAssertion shouldCallTariffOnce() {
    mockServer
        .verify(requestGet("/v1/clean-air-zones/" + SAMPLE_CLEAN_AIR_ZONE + "/tariff"),
            VerificationTimes.exactly(1));
    return this;
  }

  public AnprAssertion shouldCallPaymentsForUkEntrant() {
    mockServer
        .verify(
            requestPost("/v1/payments/vehicle-entrants", "payment-status-second-request.json"),
            VerificationTimes.exactly(1));
    return this;
  }

  public AnprAssertion shouldCallPaymentsForNonUkEntrant() {
    mockServer.verify(
        requestPost("/v1/payments/vehicle-entrants", "payment-status-third-request.json"),
        VerificationTimes.exactly(1));
    return this;
  }

  public AnprAssertion shouldNotCallNtr() {
    mockServer.verify(requestGet("/v1/vehicles/" + vrn + "/licence-info"),
        VerificationTimes.exactly(0));
    return this;
  }

  public AnprAssertion shouldNotCallTariff() {
    mockServer.verify(requestGet("/v1/clean-air-zones/" + SAMPLE_CLEAN_AIR_ZONE + "/tariff"),
        VerificationTimes.exactly(0));
    return this;
  }

  public AnprAssertion shouldNotCallPayments() {
    mockServer
        .verify(
            requestPost("/v1/payments/vehicle-entrants", "payment-status-second-request.json"),
            VerificationTimes.exactly(0));
    mockServer
        .verify(
            requestPost("/v1/payments/vehicle-entrants", "payment-status-third-request.json"),
            VerificationTimes.exactly(0));
    return this;
  }

  public void mockMilitaryVehicle() {
    MilitaryVehicle militaryVehicle = new MilitaryVehicle();
    militaryVehicle.setVrn("CAS312");
    militaryVehicle.setWhitelistDiscountCode("WDC001");
    militaryVehicle.setModWhitelistType("TEST-TYPE");
    modRepository.save(militaryVehicle);
  }

  public AnprAssertion thereShouldBeNoEntriesInDb() {
    assertThat(cleanAirZoneEntrantRepository.count()).isEqualTo(0);
    return this;
  }
}
