package uk.gov.caz.vcc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.matchers.Times.exactly;
import static uk.gov.caz.vcc.controller.VehicleEntrantsController.CAZ_ID;
import static uk.gov.caz.vcc.util.MockServerTestIT.paymentV1Response;
import static uk.gov.caz.vcc.util.MockServerTestIT.requestGet;
import static uk.gov.caz.vcc.util.MockServerTestIT.requestPost;
import static uk.gov.caz.vcc.util.MockServerTestIT.response;
import static uk.gov.caz.vcc.util.VehicleResultDtoAssert.assertThat;

import com.amazonaws.services.sqs.model.Message;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;
import org.mockserver.verify.VerificationTimes;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.vcc.domain.GeneralWhitelistVehicle;
import uk.gov.caz.vcc.domain.RetrofittedVehicle;
import uk.gov.caz.vcc.dto.VehicleEntrantDto;
import uk.gov.caz.vcc.dto.VehicleEntrantSaveDto;
import uk.gov.caz.vcc.dto.VehicleResultDto;
import uk.gov.caz.vcc.repository.CleanAirZoneEntrantRepository;
import uk.gov.caz.vcc.repository.GeneralWhitelistRepository;
import uk.gov.caz.vcc.repository.RetrofitRepository;
import uk.gov.caz.vcc.repository.VehicleDetailsRepository;
import uk.gov.caz.vcc.service.NationalTaxiRegisterService;
import uk.gov.caz.vcc.service.VehicleEntrantsService;
import uk.gov.caz.vcc.util.Sha2Hasher;
import uk.gov.caz.vcc.util.SqsTestUtility;

@RequiredArgsConstructor
public class AnprAssertion {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter
      .ofPattern("yyyy-MM-dd'T'HHmmssX");

  public static final String SAMPLE_ENTRANT_DATE = "2017-10-01T155301Z";

  public static final LocalDateTime SAMPLE_ENTRANT_DATETIME = LocalDateTime
      .parse(SAMPLE_ENTRANT_DATE, FORMATTER);

  public static final UUID BATH_CLEAN_AIR_ZONE = UUID
      .fromString("131af03c-f7f4-4aef-81ee-aae4f56dbeb5");
  public static final UUID BIRMINGHAM_CLEAN_AIR_ZONE = UUID
      .fromString("420b5cfb-cc9b-492b-bc07-5b77d380f72d");

  public final VehicleEntrantsService vehicleEntrantsService;
  public final NationalTaxiRegisterService nationalTaxiRegisterService;
  public final GeneralWhitelistRepository generalWhitelistRepository;
  public final RetrofitRepository retrofitRepository;
  public final VehicleDetailsRepository vehicleRepository;
  public final ClientAndServer mockServer;
  private final CleanAirZoneEntrantRepository cleanAirZoneEntrantRepository;
  private final SqsTestUtility sqsTestUtility;
  private final String queueName;
  private final ObjectMapper objectMapper;


  List<VehicleResultDto> vehicleResultsResponse;
  String vrn;
  Optional<Vehicle> vehicle;
  List<VehicleEntrantDto> vehicleEntrants = new ArrayList<>();
  List<VehicleEntrantSaveDto> vehicleEntrantSaveDtos = new ArrayList<>();
  String exemptionReason;
  String chargeValidityCode;
  String taxiPhyDescription;
  List<String> licensingAuthorities;

  private ValidatableResponse httpResponse;

  public AnprAssertion prepareVehicleEntrantsWithVrn(String vrn) {
    nationalTaxiRegisterService.cacheEvictLicenses(newArrayList(vrn));
    this.vrn = vrn;
    this.vehicle = vehicleRepository.findByRegistrationNumber(vrn);
    VehicleEntrantDto vehicleEntrant = VehicleEntrantDto.builder().vrn(vrn)
        .timestamp(SAMPLE_ENTRANT_DATE).build();
    this.vehicleEntrants.add(vehicleEntrant);
    this.vehicleEntrantSaveDtos.add(VehicleEntrantSaveDto.from(vehicleEntrant, FORMATTER));
    return this;
  }

  public AnprAssertion setExemptionReason(String exemptionReason) {
    this.exemptionReason = exemptionReason;
    return this;
  }

  public AnprAssertion setChargeValidityCode(String chargeValidityCode) {
    this.chargeValidityCode = chargeValidityCode;
    return this;
  }

  public AnprAssertion whenCreateResponseToAnprForBath() {
    vehicleResultsResponse = vehicleEntrantsService
        .createVehicleResultsResponse(vehicleEntrantSaveDtos, BATH_CLEAN_AIR_ZONE);
    return this;
  }

  public AnprAssertion whenCreateResponseToAnprForBirmingham() {
    vehicleResultsResponse = vehicleEntrantsService
        .createVehicleResultsResponse(vehicleEntrantSaveDtos, BIRMINGHAM_CLEAN_AIR_ZONE);
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
        .header(CAZ_ID, BATH_CLEAN_AIR_ZONE)
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

  public AnprAssertion hasColour(String colour) {
    assertThat(vehicleResultsResponse).hasColour(colour);
    return this;
  }

  public AnprAssertion hasMake(String make) {
    assertThat(vehicleResultsResponse).hasMake(make);
    return this;
  }

  public AnprAssertion hasModel(String model) {
    assertThat(vehicleResultsResponse).hasModel(model);
    return this;
  }

  public AnprAssertion hasTypeApproval(String typeApproval) {
    assertThat(vehicleResultsResponse).hasTypeApproval(typeApproval);
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

  public AnprAssertion hasNoPaymentMethod() {
    assertThat(vehicleResultsResponse).hasPaymentMethod(null);
    return this;
  }

  public AnprAssertion hasPaymentMethodByCard() {
    assertThat(vehicleResultsResponse).hasPaymentMethod("card");
    return this;
  }

  public AnprAssertion hasPaymentMethodByDirectDebit() {
    assertThat(vehicleResultsResponse).hasPaymentMethod("direct_debit");
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

  public AnprAssertion hasNationalExemptionCode() {
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

  public AnprAssertion mockChargeZero() {
    mockTariffAndNtr(vrn);
    return this;
  }

  public AnprAssertion mockRetrofitCompliantStatus() {
    mockRetrofitVehicle(vrn);
    mockTariffAndNtr(vrn);
    return this;
  }

  public void mockTariffAndNtr(String vrn) {
    mockTariffServiceForBath();
    mockNtrBulkWithoutWac(vrn);
  }

  public AnprAssertion mockNotCompliantNotPaidStatusWhenPaymentStatusIsNotPaid() {
    mockTariffAndNtr(vrn);
    mockPaymentServiceWithStatusNotPaidWhenPaymentStatusIs(
        "payment-status-cas305-not-paid-response.json");
    return this;
  }

  public AnprAssertion mockNotCompliantNotPaidStatusWhenPaymentStatusIsRefunded() {
    mockTariffAndNtr(vrn);
    mockPaymentServiceWithStatusNotPaidWhenPaymentStatusIs("payment-status-refunded-response.json");
    return this;
  }

  public AnprAssertion mockNotCompliantNotPaidStatusWhenPaymentStatusIsChargeback() {
    mockTariffAndNtr(vrn);
    mockTariffServiceForBath();
    mockPaymentServiceWithStatusNotPaidWhenPaymentStatusIs(
        "payment-status-chargeback-response.json");
    return this;
  }

  public AnprAssertion mockExemptStatus() {
    mockMilitaryVehicle();
    return this;
  }

  public AnprAssertion mockWhitelistExemptStatus() {
    mockWhitelistVehicle(true, false);
    return this;
  }

  public AnprAssertion mockWhitelistCompliantStatus() {
    mockWhitelistVehicle(false, true);
    return this;
  }

  public AnprAssertion mockNotCompliantPaidByDirectDebitStatus() {
    mockTariffAndNtr(vrn);
    mockPaymentServiceWithStatusPaidByDirectDebit();
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

  public AnprAssertion mockUnrecognisedPaidStatusForCAS307BE() {
    mockServer
        .when(requestPost("/v1/payments/vehicle-entrants", "payment-status-cas307be-request.json"),
            exactly(1))
        .respond(paymentV1Response("payment-status-cas307be-paid-response.json"));
    return this;
  }

  public AnprAssertion mockPaymentServiceWithStatusPaidForTST010() {
    mockServer
        .when(requestPost("/v1/payments/vehicle-entrants", "payment-status-tst010-request.json"),
            exactly(1))
        .respond(paymentV1Response("payment-status-tst010-paid-response.json"));
    return this;
  }

  public AnprAssertion mockPaymentServiceWithStatusNotPaidForJA07PCB() {
    mockServer
        .when(requestPost("/v1/payments/vehicle-entrants", "payment-status-ja07pcb-request.json"),
            exactly(1))
        .respond(paymentV1Response("payment-status-ja07pcb-not-paid-response.json"));
    return this;
  }

  public AnprAssertion mockPaymentServiceWithStatusNotPaidForNonUkFor0AS307BE() {
    mockServer
        .when(requestPost("/v1/payments/vehicle-entrants", "payment-status-0as307be-request.json"),
            exactly(1))
        .respond(paymentV1Response("payment-status-0as307be-not-paid-response.json"));
    return this;
  }

  public void mockPaymentServiceWithStatusPaid() {
    mockServer
        .when(requestPost("/v1/payments/vehicle-entrants", "payment-status-second-request.json"),
            exactly(1))
        .respond(paymentV1Response("payment-status-paid-response.json"));
  }

  public void mockPaymentServiceWithStatusPaidByDirectDebit() {
    mockServer
        .when(requestPost("/v1/payments/vehicle-entrants", "payment-status-second-request.json"),
            exactly(1))
        .respond(paymentV1Response("payment-status-paid-by-direct-debit-response.json"));
  }

  public void mockPaymentServiceWithStatusNotPaidWhenPaymentStatusIs(String paymentResponse) {
    mockServer
        .when(requestPost("/v1/payments/vehicle-entrants", "payment-status-second-request.json"),
            exactly(1))
        .respond(paymentV1Response(paymentResponse));
  }

  public AnprAssertion mockPaymentServiceWithStatusNotPaidForCAS307BE() {
    mockServer
        .when(requestPost("/v1/payments/vehicle-entrants", "payment-status-cas307be-request.json"),
            exactly(1))
        .respond(paymentV1Response("payment-status-cas307be-not-paid-response.json"));
    return this;
  }

  public AnprAssertion mockTariffServiceForBath() {
    mockServer.when(requestGet("/v1/clean-air-zones/" + BATH_CLEAN_AIR_ZONE + "/tariff"),
        exactly(1))
        .respond(response("tariff-rates-bath-response.json"));
    return this;
  }

  public AnprAssertion mockTariffServiceForBirmingham() {
    mockServer.when(requestGet("/v1/clean-air-zones/" + BIRMINGHAM_CLEAN_AIR_ZONE + "/tariff"),
        exactly(1))
        .respond(response("tariff-rates-birmingham-response.json"));
    return this;
  }

  public AnprAssertion mockTariffWithZeroRates() {
    mockServer.when(requestGet("/v1/clean-air-zones/" + BATH_CLEAN_AIR_ZONE + "/tariff"),
        exactly(1))
        .respond(response("tariff-zero-rates-response.json"));
    return this;
  }

  public AnprAssertion mockNtr(String vrn) {
    mockServer.when(requestGet("/v1/vehicles/" + vrn + "/licence-info"), exactly(1))
        .respond(response("ntr-first-response.json"));
    this.taxiPhyDescription = "taxi";
    this.licensingAuthorities = newArrayList("la-1", "la-2");
    return this;
  }

  public AnprAssertion mockNtrBulk(String vrn) {
    mockServer
        .when(
            requestPost("/v1/vehicles/licences-info/search", "ntr-bulk-details-request.json", vrn),
            exactly(1))
        .respond(response("ntr-bulk-response.json", vrn));
    this.taxiPhyDescription = "taxi";
    this.licensingAuthorities = newArrayList("la-1", "la-2");
    return this;
  }

  public AnprAssertion mockNtrBulkWithoutLicensingAuthorities(String vrn) {
    mockServer
        .when(
            requestPost("/v1/vehicles/licences-info/search", "ntr-bulk-details-request.json", vrn),
            exactly(1))
        .respond(response("ntr-bulk-response.json", vrn));
    return this;
  }

  public AnprAssertion mockNtrBulkForNotPresent(String vrn) {
    mockServer
        .when(
            requestPost("/v1/vehicles/licences-info/search", "ntr-bulk-details-request.json", vrn),
            exactly(1))
        .respond(response("ntr-empty-bulk-response.json"));
    return this;
  }

  public AnprAssertion mockNtrWithoutWac(String vrn) {
    mockServer.when(requestGet("/v1/vehicles/" + vrn + "/licence-info"), exactly(1))
        .respond(response("ntr-bath-without-wac-response.json"));
    return this;
  }

  public AnprAssertion mockNtrBulkWithoutWac(String vrn) {
    mockServer
        .when(
            requestPost("/v1/vehicles/licences-info/search", "ntr-bulk-details-request.json", vrn),
            exactly(1))
        .respond(response("ntr-bulk-without-wac-response.json", vrn));
    return this;
  }

  public AnprAssertion shouldCallNtrOnce() {
    mockServer.verify(requestGet("/v1/vehicles/" + vrn + "/licence-info"),
        VerificationTimes.exactly(1));
    return this;
  }

  public AnprAssertion shouldCallNtrBulkOnce() {
    mockServer.verify(
        requestPost("/v1/vehicles/licences-info/search", "ntr-bulk-details-request.json", vrn),
        VerificationTimes.exactly(1));
    return this;
  }

  public AnprAssertion shouldCallTariffOnce() {
    mockServer
        .verify(requestGet("/v1/clean-air-zones/" + BATH_CLEAN_AIR_ZONE + "/tariff"),
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
        requestPost("/v1/payments/vehicle-entrants", "payment-status-cas307be-request.json"),
        VerificationTimes.exactly(1));
    return this;
  }

  public AnprAssertion shouldNotCallNtr() {
    mockServer.verify(requestGet("/v1/vehicles/" + vrn + "/licence-info"),
        VerificationTimes.exactly(0));
    return this;
  }

  public AnprAssertion shouldNotCallNtrBulk() {
    mockServer.verify(
        requestPost("/v1/vehicles/licences-info/search", "ntr-bulk-details-request.json", vrn),
        VerificationTimes.exactly(0));
    return this;
  }

  public AnprAssertion shouldNotCallTariff() {
    mockServer.verify(requestGet("/v1/clean-air-zones/" + BATH_CLEAN_AIR_ZONE + "/tariff"),
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
            requestPost("/v1/payments/vehicle-entrants", "payment-status-cas307be-request.json"),
            VerificationTimes.exactly(0));
    return this;
  }

  public AnprAssertion shouldSendReportingMessageWithoutDvlaData() {
	List<List<Map<String, Object>>> messages = toMaps(sqsTestUtility.receiveSqsMessages(queueName));
    assertTrue(messages.size() > 0);
    for (VehicleEntrantDto entrant : vehicleEntrants) {
      for (List<Map<String, Object>> requests: messages) {
        checkEntrantAttributes(requests, entrant);
        checkAttributeInMessagesIfNotNull(requests, "exemptionReason", exemptionReason);
        checkAttributeInMessagesIfNotNull(requests, "chargeValidityCode", chargeValidityCode);
      }
    }
    return this;
  }

  public AnprAssertion shouldSendReportingMessage() {
	List<List<Map<String, Object>>>  messages = toMaps(sqsTestUtility.receiveSqsMessages(queueName));
    assertTrue(messages.size() > 0);
    for (VehicleEntrantDto entrant : vehicleEntrants) {
      for (List<Map<String, Object>> requests: messages) {
        checkEntrantAttributes(requests, entrant);
        checkVehicleAttributes(requests);
        checkAttributeInMessagesIfNotNull(requests, "exemptionReason", exemptionReason);
        checkAttributeInMessagesIfNotNull(requests, "chargeValidityCode", chargeValidityCode);
        checkLicensingAuthorityAttribute(requests);
      }
    }
    return this;
  }

  private void checkLicensingAuthorityAttribute(List<Map<String, Object>> messages) {
    if (this.licensingAuthorities != null && !this.licensingAuthorities.isEmpty()) {
      assertTrue(findAttributeInMessages(messages, "licensingAuthorities", licensingAuthorities));
    }
  }

  private void checkEntrantAttributes(List<Map<String, Object>> messages,
      VehicleEntrantDto entrant) {
    String hashedVrn = Sha2Hasher.sha256Hash(entrant.getVrn());
    checkAttributeInMessagesIfNotNull(messages, "vrnHash", hashedVrn);
  }

  private void checkVehicleAttributes(List<Map<String, Object>> messagesAsMaps) {
    if (vehicle.isPresent()) {
      checkAttributeInMessagesIfNotNull(messagesAsMaps, "colour", vehicle.get().getColour());
      checkAttributeInMessagesIfNotNull(messagesAsMaps, "fuelType", vehicle.get().getFuelType());
      checkAttributeInMessagesIfNotNull(messagesAsMaps, "make", vehicle.get().getMake());
      checkAttributeInMessagesIfNotNull(messagesAsMaps, "model", vehicle.get().getModel());
      checkAttributeInMessagesIfNotNull(messagesAsMaps, "typeApproval",
          vehicle.get().getTypeApproval());
    }
  }

  private List<List<Map<String, Object>>> toMaps(List<Message> messages) {
    return messages.stream()
      .map(Message::getBody)
      .map(this::readJson)
      .collect(Collectors.toList());  
  }

  @SneakyThrows
  private List<Map<String, Object>> readJson(String body) {
    return objectMapper.readValue(body, new TypeReference<List<Map<String, Object>>>() {
    });
  }

  private void checkAttributeInMessagesIfNotNull(List<Map<String, Object>> messages, String name,
      String attribute) {
    if (attribute != null && !attribute.isEmpty()) {
      assertTrue(findAttributeInMessages(messages, name, attribute));
    }
  }

  private boolean findAttributeInMessages(List<Map<String, Object>> messages, String name,
      Object attribute) {
    return messages.stream()
        .anyMatch(message -> {
          Object value = message.get(name);
          return Objects.equals(value, attribute);
        });
  }

  public void mockMilitaryVehicle() {
    mockMilitaryVehicle("CAS312");
    this.exemptionReason = "Other";
    this.vehicle = Optional.empty();
  }

  public void mockMilitaryVehicle(String vrn) {
    mockServer.when(requestGet("/v1/mod/" + vrn))
        .respond(response("mod-vehicle-response.json"));
  }

  public AnprAssertion mockMilitaryVehicleForGivenVrn() {
    mockServer.when(requestGet("/v1/mod/" + vrn))
        .respond(response("mod-pms115-response.json"));
    return this;
  }

  public void mockWhitelistVehicle(boolean isExempt, boolean isCompliant) {
    GeneralWhitelistVehicle w = new GeneralWhitelistVehicle();
    w.setVrn("CAS402");
    w.setExempt(isExempt);
    w.setCompliant(isCompliant);
    w.setReasonUpdated("test");
    w.setUpdateTimestamp(java.time.LocalDateTime.now());
    w.setUploaderId(UUID.fromString("23a84d23-a45a-4ce1-aa74-df2058c93289"));
    w.setCategory("test");

    generalWhitelistRepository.save(w);
    this.exemptionReason = isExempt ? "Exemption" : null;
  }

  public AnprAssertion mockProblematicVRNCategoryInWhitelist() {
    GeneralWhitelistVehicle whitelistVehicle = GeneralWhitelistVehicle.builder()
        .vrn(vrn)
        .category("Problematic VRN")
        .exempt(true)
        .compliant(false)
        .reasonUpdated("test")
        .updateTimestamp(java.time.LocalDateTime.now())
        .uploaderId(UUID.fromString("23a84d23-a45a-4ce1-aa74-df2058c93289"))
        .build();
    generalWhitelistRepository.save(whitelistVehicle);
    this.exemptionReason = "Problematic VRN";
    return this;
  }

  public AnprAssertion mockEarlyAdopterCategoryInWhitelist() {
    GeneralWhitelistVehicle whitelistVehicle = GeneralWhitelistVehicle.builder()
        .vrn(vrn)
        .category("Early Adopter")
        .exempt(false)
        .compliant(true)
        .reasonUpdated("test")
        .updateTimestamp(java.time.LocalDateTime.now())
        .uploaderId(UUID.fromString("23a84d23-a45a-4ce1-aa74-df2058c93289"))
        .build();
    generalWhitelistRepository.save(whitelistVehicle);
    return this;
  }

  public AnprAssertion mockNonUkVehicleCategoryInWhitelist() {
    GeneralWhitelistVehicle whitelistVehicle = GeneralWhitelistVehicle.builder()
        .vrn(vrn)
        .category("Non-UK Vehicle")
        .exempt(false)
        .compliant(true)
        .reasonUpdated("test")
        .updateTimestamp(java.time.LocalDateTime.now())
        .uploaderId(UUID.fromString("23a84d23-a45a-4ce1-aa74-df2058c93289"))
        .build();
    generalWhitelistRepository.save(whitelistVehicle);
    return this;
  }

  public AnprAssertion mockExemptionCategoryInWhitelist() {
    GeneralWhitelistVehicle whitelistVehicle = GeneralWhitelistVehicle.builder()
        .vrn(vrn)
        .category("Exemption")
        .exempt(true)
        .compliant(false)
        .reasonUpdated("test")
        .updateTimestamp(java.time.LocalDateTime.now())
        .uploaderId(UUID.fromString("23a84d23-a45a-4ce1-aa74-df2058c93289"))
        .build();
    generalWhitelistRepository.save(whitelistVehicle);
    this.exemptionReason = "Exemption";
    return this;
  }

  public AnprAssertion mockOtherCategoryInWhitelist() {
    GeneralWhitelistVehicle whitelistVehicle = GeneralWhitelistVehicle.builder()
        .vrn(vrn)
        .category("Other")
        .exempt(true)
        .compliant(false)
        .reasonUpdated("test")
        .updateTimestamp(java.time.LocalDateTime.now())
        .uploaderId(UUID.fromString("23a84d23-a45a-4ce1-aa74-df2058c93289"))
        .build();
    generalWhitelistRepository.save(whitelistVehicle);
    this.exemptionReason = "Other";
    return this;
  }

  public AnprAssertion mockRetrofitVehicle(String vrn) {
    RetrofittedVehicle w = new RetrofittedVehicle();
    w.setVrn(vrn);
    w.setDateOfRetrofit(new java.sql.Date(2019, 1, 1));

    retrofitRepository.save(w);
    return this;
  }

  public AnprAssertion thereShouldBeNoEntriesInDb() {
    assertThat(cleanAirZoneEntrantRepository.count()).isEqualTo(0);
    return this;
  }

  public AnprAssertion mockNotFoundInMod() {
    mockServer.when(requestGet("/v1/mod/" + vrn))
        .respond(HttpResponse.response().withStatusCode(404));
    return this;
  }
}
