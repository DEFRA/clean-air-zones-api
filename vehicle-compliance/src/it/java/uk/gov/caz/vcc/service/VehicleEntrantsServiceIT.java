package uk.gov.caz.vcc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;
import static org.mockserver.matchers.Times.exactly;
import static uk.gov.caz.vcc.util.CleanAirZoneEntrantAssert.assertThat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.caz.vcc.AnprAssertion;
import uk.gov.caz.vcc.annotation.IntegrationTest;
import uk.gov.caz.vcc.domain.ChargeValidity;
import uk.gov.caz.vcc.domain.CleanAirZoneEntrant;
import uk.gov.caz.vcc.domain.MilitaryVehicle;
import uk.gov.caz.vcc.domain.RetrofittedVehicle;
import uk.gov.caz.vcc.dto.VehicleEntrantDto;
import uk.gov.caz.vcc.dto.VehicleEntrantsSaveRequestDto;
import uk.gov.caz.vcc.repository.CleanAirZoneEntrantRepository;
import uk.gov.caz.vcc.repository.ModRepository;
import uk.gov.caz.vcc.repository.RetrofitRepository;
import uk.gov.caz.vcc.util.MockServerTestIT;

@IntegrationTest
class VehicleEntrantsServiceIT extends MockServerTestIT {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter
      .ofPattern("yyyy-MM-dd'T'HHmmssX");

  private static final String SAMPLE_ENTRANT_DATE = "2017-10-01T155301Z";

  private static final UUID SAMPLE_CLEAN_AIR_ZONE = UUID
      .fromString("420b5cfb-cc9b-492b-bc07-5b77d380f72d");

  private static final String  SAMPLE_CORRELATION_ID = UUID.randomUUID().toString();

  @Autowired
  private CazTariffService tariffService;

  @Autowired
  private NationalTaxiRegisterService nationalTaxiRegisterService;

  @Autowired
  private VehicleEntrantsService vehicleEntrantsService;

  @Autowired
  private CleanAirZoneEntrantRepository repository;

  @Autowired
  private ModRepository modRepository;

  @Autowired
  private RetrofitRepository retrofitRepository;

  @AfterEach
  @BeforeEach
  public void cleanup() {
    mockServer.reset();
    tariffService.cacheEvictCleanAirZones();
    repository.deleteAll();
    modRepository.deleteAll();
    retrofitRepository.deleteAll();
  }

  @Test
  public void shouldSaveVehicleEntrantsToDb() {
    //given
    UUID cazId = UUID.fromString("420b5cfb-cc9b-492b-bc07-5b77d380f72d");
    String correlationId = UUID.randomUUID().toString();
    mockPayments("payment-status-vrn1-request.json");
    mockPayments("payment-status-vrn2-request.json");
    CleanAirZoneEntrant entrantModel1 = getEntrantModel(cazId, correlationId, "vrn1");
    CleanAirZoneEntrant entrantModel2 = getEntrantModel(cazId, correlationId, "vrn2");

    VehicleEntrantDto entrantDto1 = getEntrantDto(entrantModel1);
    VehicleEntrantDto entrantDto2 = getEntrantDto(entrantModel2);

    VehicleEntrantsSaveRequestDto saveRequestDto = new VehicleEntrantsSaveRequestDto(
        cazId, correlationId, newArrayList(entrantDto1, entrantDto2)
    );

    //when
    vehicleEntrantsService.save(saveRequestDto);
    Iterable<CleanAirZoneEntrant> airZoneEntrants = repository.findAll();

    //then
    assertThat(airZoneEntrants).hasSize(2);
  }

  @Test
  public void shouldSaveVehicleEntrantToDbWithProperlyPopulatedFields() {
    //given
    UUID cazId = UUID.fromString("420b5cfb-cc9b-492b-bc07-5b77d380f72d");
    String correlationId = UUID.randomUUID().toString();
    mockPayments("payment-status-vrn1-request.json");
    CleanAirZoneEntrant entrantModel1 = getEntrantModel(cazId, correlationId, "vrn1");
    VehicleEntrantDto entrantDto1 = getEntrantDto(entrantModel1);

    VehicleEntrantsSaveRequestDto vehicleEntrantsSaveRequestDto = new VehicleEntrantsSaveRequestDto(
        cazId, correlationId, newArrayList(entrantDto1)
    );

    //when
    vehicleEntrantsService.save(vehicleEntrantsSaveRequestDto);
    Iterable<CleanAirZoneEntrant> airZoneEntrants = repository.findAll();

    //then
    assertThat(airZoneEntrants.iterator().next())
        .hasVrn(entrantDto1.getVrn())
        .hasEntrantTimestamp(entrantModel1.getEntrantTimestamp())
        .hasCorrelationId(correlationId)
        .hasCleanAirZoneId(cazId)
        .hasChargeValidityCode(entrantModel1.getChargeValidityCode().getChargeValidityCode());
  }

  @Test
  public void retrofittedVehicleReturnedOk() {
    // given
    UUID cazId = UUID.fromString("420b5cfb-cc9b-492b-bc07-5b77d380f72d");
    String correlationId = UUID.randomUUID().toString();
    String testVrn = "vrn1";
    mockPayments("payment-status-vrn1-request.json");
    CleanAirZoneEntrant entrantModel1 = getEntrantModel(cazId, correlationId, testVrn);
    VehicleEntrantDto entrantDto1 = getEntrantDto(entrantModel1);

    VehicleEntrantsSaveRequestDto vehicleEntrantsSaveRequestDto = new VehicleEntrantsSaveRequestDto(
        cazId, correlationId, newArrayList(entrantDto1)
    );

    createRetrofitVehicle(testVrn);

    //when
    vehicleEntrantsService.save(vehicleEntrantsSaveRequestDto);
    Iterable<CleanAirZoneEntrant> airZoneEntrants = repository.findAll();

    //then
    assertThat(airZoneEntrants.iterator().next())
        .hasVrn(entrantDto1.getVrn())
        .hasEntrantTimestamp(entrantModel1.getEntrantTimestamp())
        .hasCorrelationId(correlationId)
        .hasCleanAirZoneId(cazId)
        .hasChargeValidityCode(entrantModel1.getChargeValidityCode().getChargeValidityCode());
  }

  @Test
  public void militaryVehicleReturnedOk() {
    // given
    UUID cazId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();
    String testVrn = "vrn1";

    CleanAirZoneEntrant entrantModel1 = getEntrantModel(cazId, correlationId, testVrn);
    VehicleEntrantDto entrantDto1 = getEntrantDto(entrantModel1);

    VehicleEntrantsSaveRequestDto vehicleEntrantsSaveRequestDto = new VehicleEntrantsSaveRequestDto(
        cazId, correlationId, newArrayList(entrantDto1)
    );

    createMilitaryVehicle(testVrn);

    //when
    vehicleEntrantsService.save(vehicleEntrantsSaveRequestDto);
    Iterable<CleanAirZoneEntrant> airZoneEntrants = repository.findAll();

    //then
    assertThat(airZoneEntrants.iterator().next())
        .hasVrn(entrantDto1.getVrn())
        .hasEntrantTimestamp(entrantModel1.getEntrantTimestamp())
        .hasCorrelationId(correlationId)
        .hasCleanAirZoneId(cazId)
        .hasChargeValidityCode("CVC02");
  }

  @Test
  public void shouldReturnCompliantStatusToAnpr() {
    given()
        .prepareVehicleEntrantsWithVrn("CAS300")
        .mockCompliantStatus()

        .whenCreateResponseToAnpr()

        .then()
        .shouldCallNtrOnce()
        .shouldCallTariffOnce()
        .shouldNotCallPayments()

        .andResponse()
        .hasGivenVrn()
        .hasCompliantStatus()
        .tariffCodeIsNull()
        .exemptionCodeIsNull();
  }

  @Test
  public void shouldReturnExemptStatusToAnpr() {
    given()
        .prepareVehicleEntrantsWithVrn("CAS312")
        .mockExemptStatus()

        .whenCreateResponseToAnpr()

        .then()
        .shouldNotCallNtr()
        .shouldNotCallTariff()
        .shouldNotCallPayments()

        .andResponse()
        .hasGivenVrn()
        .hasExemptStatus()
        .hasModExemptionCode();
  }

  @Test
  public void shouldReturnNotCompliantNotPaidStatusToAnprWithoutCallToPayments() {
    given()
        .prepareVehicleEntrantsWithVrn("CAS334")
        .mockNotCompliantNotPaidStatusWithoutCallToPayments()

        .whenCreateResponseToAnpr()

        .then()
        .shouldCallNtrOnce()
        .shouldCallTariffOnce()
        .shouldNotCallPayments()

        .andResponse()
        .hasGivenVrn()
        .hasNotCompliantNotPaidStatus()
        .hasTariffCode("LCC01-CAR")
        .exemptionCodeIsNull();
  }

  @Test
  public void shouldReturnNotCompliantNotPaidStatusToAnprWithCallToPayments() {
    given()
        .prepareVehicleEntrantsWithVrn("CAS308")
        .mockNotCompliantNotPaidStatusWithCallToPayments()

        .whenCreateResponseToAnpr()

        .then()
        .shouldCallNtrOnce()
        .shouldCallTariffOnce()
        .shouldCallPaymentsForUkEntrant()

        .andResponse()
        .hasGivenVrn()
        .hasNotCompliantNotPaidStatus()
        .hasTariffCode("LCC01-MINIBUS")
        .exemptionCodeIsNull();
  }

  @Test
  public void shouldReturnNotCompliantPaidStatusToAnpr() {
    given()
        .prepareVehicleEntrantsWithVrn("CAS308")
        .mockNotCompliantPaidStatus()

        .whenCreateResponseToAnpr()

        .then()
        .shouldCallNtrOnce()
        .shouldCallTariffOnce()
        .shouldCallPaymentsForUkEntrant()

        .andResponse()
        .hasGivenVrn()
        .hasNotCompliantPaidStatus()
        .hasTariffCode("LCC01-MINIBUS")
        .exemptionCodeIsNull();
  }

  @Test
  public void shouldReturnUnrecognisedNotPaidStatusToAnpr() {
    given()
        .prepareVehicleEntrantsWithVrn("CAS307BE")
        .mockUnrecognisedNotPaidStatus()

        .whenCreateResponseToAnpr()

        .then()
        .shouldCallPaymentsForNonUkEntrant()
        .shouldNotCallNtr()
        .shouldNotCallTariff()

        .andResponse()
        .hasGivenVrn()
        .hasUnrecognisedNotPaidStatus()
        .tariffCodeIsNull()
        .exemptionCodeIsNull();
  }

  @Test
  public void shouldReturnUnrecognisedPaidStatusToAnpr() {
    given()
        .prepareVehicleEntrantsWithVrn("CAS307BE")
        .mockUnrecognisedPaidStatus()

        .whenCreateResponseToAnpr()

        .then()
        .shouldCallPaymentsForNonUkEntrant()
        .shouldNotCallNtr()
        .shouldNotCallTariff()

        .andResponse()
        .hasGivenVrn()
        .hasUnrecognisedPaidStatus()
        .tariffCodeIsNull()
        .exemptionCodeIsNull();
  }

  private void mockPayments(String request) {
    mockServer.when(requestPost("/v1/payments/vehicle-entrants", request), exactly(1))
        .respond(response("payment-status-not-paid-response.json"));
  }

  private CleanAirZoneEntrant getEntrantModel(UUID cazId, String correlationId, String vrn) {
    LocalDateTime entrantTimestamp = LocalDateTime.parse(SAMPLE_ENTRANT_DATE, FORMATTER);

    CleanAirZoneEntrant entrantModel = new CleanAirZoneEntrant(cazId, correlationId,
        entrantTimestamp);
    entrantModel.setVrn(vrn);
    entrantModel.setChargeValidityCode(new ChargeValidity("CVC04"));

    return entrantModel;
  }

  private VehicleEntrantDto getEntrantDto(CleanAirZoneEntrant model) {
    return new VehicleEntrantDto(model.getVrn(), "2017-10-01T155301Z");
  }

  @SneakyThrows
  private void createRetrofitVehicle(String testVrn) {
    RetrofittedVehicle r = new RetrofittedVehicle();
    r.setVrn(testVrn);
    r.setWhitelistDiscountCode("Sample retrofit code.");
    r.setDateOfRetrofit(new java.sql.Date(Calendar.getInstance().getTime().getTime()));

    retrofitRepository.save(r);
  }

  @SneakyThrows
  private void createMilitaryVehicle(String testVrn) {
    MilitaryVehicle m = new MilitaryVehicle();
    m.setVrn(testVrn);
    m.setWhitelistDiscountCode("Sample mod code.");
    m.setModWhitelistType("WHITE VEHICLE");

    modRepository.save(m);
  }

  AnprAssertion given() {
    return new AnprAssertion(vehicleEntrantsService, modRepository, nationalTaxiRegisterService,
        mockServer, repository);
  }
}