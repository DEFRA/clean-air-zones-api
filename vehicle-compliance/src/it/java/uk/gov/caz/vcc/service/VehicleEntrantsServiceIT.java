package uk.gov.caz.vcc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.matchers.Times.exactly;
import static uk.gov.caz.vcc.util.CleanAirZoneEntrantAssert.assertThat;

import com.amazonaws.services.sqs.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.caz.vcc.AnprAssertion;
import uk.gov.caz.vcc.annotation.IntegrationTest;
import uk.gov.caz.vcc.domain.ChargeValidity;
import uk.gov.caz.vcc.domain.CleanAirZoneEntrant;
import uk.gov.caz.vcc.domain.GeneralWhitelistVehicle;
import uk.gov.caz.vcc.domain.RetrofittedVehicle;
import uk.gov.caz.vcc.dto.VehicleEntrantSaveDto;
import uk.gov.caz.vcc.dto.VehicleEntrantsSaveRequestDto;
import uk.gov.caz.vcc.repository.CleanAirZoneEntrantRepository;
import uk.gov.caz.vcc.repository.GeneralWhitelistRepository;
import uk.gov.caz.vcc.repository.RetrofitRepository;
import uk.gov.caz.vcc.repository.VehicleDetailsRepository;
import uk.gov.caz.vcc.util.MockServerTestIT;
import uk.gov.caz.vcc.util.SqsTestUtility;
import uk.gov.caz.vcc.util.TestFixturesLoader;

@IntegrationTest
class VehicleEntrantsServiceIT extends MockServerTestIT {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter
      .ofPattern("yyyy-MM-dd'T'HHmmssX");

  private static final String SAMPLE_ENTRANT_DATE = "2017-10-01T155301Z";

  static final ImmutableMap<String, String> VRNS_TO_HASHED_VRNS = ImmutableMap
      .<String, String>builder()
      .put("vrn1", "d84749408564e1f15e92d2b7c4389f634e7905368b88504d7570af3a77198aa0")
      .put("vrn2", "2b07fcdbae118f11ca782a9ae03355cf7f205129670f6de3d27f483f2c9dc4ba")
      .put("CAS312", "90893d1e0836049608a382c44e232b984c8296a07e2192ea425a677c370b93e1")
      .put("longerthanseve", "88a060dcf5775e513213d62d6e90281bf3156144df5c30872a44c886d9205a89")
      .build();

  private static final String NOT_COMPLIANT_CODE = "CVC01";
  private static final String EXEMPT_CODE = "CVC02";
  private static final String COMPLIANT_CODE = "CVC03";
  private static final String UNRECOGNISED_CODE = "CVC04";
  private static final String PAID_PAYMENT_TARIFF_CODE = "PAYMENT_TARIFF_CODE";

  @Value("${services.sqs.reporting-data-queue-name}")
  private String queueName;

  @Autowired
  private CazTariffService tariffService;

  @Autowired
  private NationalTaxiRegisterService nationalTaxiRegisterService;

  @Autowired
  private VehicleEntrantsService vehicleEntrantsService;

  @Autowired
  private CleanAirZoneEntrantRepository repository;

  @Autowired
  private RetrofitRepository retrofitRepository;

  @Autowired
  private GeneralWhitelistRepository generalWhitelistRepository;

  @Autowired
  private TestFixturesLoader testFixturesLoader;

  @Autowired
  private SqsTestUtility sqsTestUtility;

  @Autowired
  private VehicleDetailsRepository vehicleRepository;

  @Autowired
  private ObjectMapper objectMapper;

  @AfterEach
  @BeforeEach
  public void cleanup() throws IOException {
    mockServer.reset();
    tariffService.cacheEvictCleanAirZones();
    repository.deleteAll();
    retrofitRepository.deleteAll();
    generalWhitelistRepository.deleteAll();
    testFixturesLoader.loadTestVehiclesIntoDb();
  }

  @BeforeEach
  public void createEmailQueue() {
    sqsTestUtility.createQueue(queueName);
  }

  @AfterEach
  public void deleteQueue() {
    sqsTestUtility.deleteQueue(queueName);
  }

  @Nested
  class SaveVehicleToDb {

    @Test
    public void shouldSaveVehicleEntrantsToDb() {
      //given
      UUID cazId = UUID.fromString("131af03c-f7f4-4aef-81ee-aae4f56dbeb5");
      String correlationId = UUID.randomUUID().toString();
      mockPayments("payment-status-vrn1-request.json",
          "payment-status-vrn1-not-paid-response.json");
      mockPayments("payment-status-vrn2-request.json",
          "payment-status-vrn2-not-paid-response.json");
      CleanAirZoneEntrant entrantModel1 = getEntrantModel(cazId, correlationId, "vrn1");
      CleanAirZoneEntrant entrantModel2 = getEntrantModel(cazId, correlationId, "vrn2");

      VehicleEntrantSaveDto entrantDto1 = getEntrantSaveDto(entrantModel1);
      VehicleEntrantSaveDto entrantDto2 = getEntrantSaveDto(entrantModel2);

      VehicleEntrantsSaveRequestDto saveRequestDto = new VehicleEntrantsSaveRequestDto(
          cazId, correlationId, newArrayList(entrantDto1, entrantDto2)
      );

      //when
      vehicleEntrantsService.save(saveRequestDto);
      Iterable<CleanAirZoneEntrant> airZoneEntrants = repository.findAll();

      //then
      assertThat(airZoneEntrants).hasSize(2);
      verifyReportingDataMessageIsSent(newArrayList(entrantDto1, entrantDto2), "false");
    }

    @Test
    public void shouldSaveVehicleEntrantToDbWithProperlyPopulatedFields() {
      //given
      UUID cazId = UUID.fromString("131af03c-f7f4-4aef-81ee-aae4f56dbeb5");
      String correlationId = UUID.randomUUID().toString();
      mockPayments("payment-status-vrn1-request.json",
          "payment-status-vrn1-not-paid-response.json");
      CleanAirZoneEntrant entrantModel1 = getEntrantModel(cazId, correlationId, "vrn1");
      VehicleEntrantSaveDto entrantDto1 = getEntrantSaveDto(entrantModel1);

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
          .hasAnyEntrantPaymentId()
          .hasChargeValidityCode(entrantModel1.getChargeValidityCode().getChargeValidityCode());

      verifyReportingDataMessageIsSent(newArrayList(entrantDto1), "false");
    }
  }

  @Nested
  class WhenRetrofittedVehicle {

    @Test
    public void retrofittedVehicleReturnedOk() {
      // given
      UUID cazId = UUID.fromString("131af03c-f7f4-4aef-81ee-aae4f56dbeb5");
      String correlationId = UUID.randomUUID().toString();
      String testVrn = "vrn1";
      mockPayments("payment-status-vrn1-request.json",
          "payment-status-vrn1-not-paid-response.json");
      CleanAirZoneEntrant entrantModel1 = getEntrantModel(cazId, correlationId, testVrn);
      VehicleEntrantSaveDto entrantDto1 = getEntrantSaveDto(entrantModel1);

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

      verifyReportingDataMessageIsSent(newArrayList(entrantDto1), "false");
    }
  }

  @Nested
  class WhenNonStandardUkPlateFormattedVehicle {

    @Test
    public void nonStandardUkPlateFormattedVehicleReturnedOk() {
      // given
      UUID cazId = UUID.fromString("131af03c-f7f4-4aef-81ee-aae4f56dbeb5");
      String correlationId = UUID.randomUUID().toString();
      String testVrn = "longerthanseve";
      mockPayments("payment-status-non-standard-uk-request.json",
          "payment-status-non-standard-uk-not-paid-response.json");
      CleanAirZoneEntrant entrantModel1 = getEntrantModel(cazId, correlationId, testVrn);
      VehicleEntrantSaveDto entrantDto1 = getEntrantSaveDto(entrantModel1);

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

      verifyReportingDataMessageIsSent(newArrayList(entrantDto1), "true");
    }

    @Test
    public void nonUkVehicleWhitelistedVehicleReturnedOk() {
      // given
      UUID cazId = UUID.fromString("131af03c-f7f4-4aef-81ee-aae4f56dbeb5");
      String correlationId = UUID.randomUUID().toString();
      String testVrn = "longerthanseve";
      mockPayments("payment-status-non-standard-uk-request.json",
          "payment-status-non-standard-uk-not-paid-response.json");
      CleanAirZoneEntrant entrantModel1 = getEntrantModel(cazId, correlationId, testVrn);
      VehicleEntrantSaveDto entrantDto1 = getEntrantSaveDto(entrantModel1);

      createNonUkWhitelistVehicle(testVrn);

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
          .hasChargeValidityCode("CVC03");

      verifyReportingDataMessageIsSent(newArrayList(entrantDto1), "true");
    }
  }

  @Nested
  class WhenMilitaryVehicle {

    @Test
    public void militaryVehicleReturnedOk() {
      // given
      UUID cazId = UUID.randomUUID();
      String correlationId = UUID.randomUUID().toString();
      String testVrn = "CAS312";

      CleanAirZoneEntrant entrantModel1 = getEntrantModel(cazId, correlationId, testVrn);
      VehicleEntrantSaveDto entrantDto1 = getEntrantSaveDto(entrantModel1);

      VehicleEntrantsSaveRequestDto vehicleEntrantsSaveRequestDto = new VehicleEntrantsSaveRequestDto(
          cazId, correlationId, newArrayList(entrantDto1)
      );

      createMilitaryVehicle(testVrn);
      given().mockNtrBulkForNotPresent(testVrn);

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

      verifyReportingDataMessageIsSent(newArrayList(entrantDto1), "false");
    }
  }

  @Test
  public void whenMultipleVehiclesFoundInGpwModRetrofitCazEntrantsWorksWell() {
    // given
    UUID cazId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();
    String testVrn = "CAS312";

    CleanAirZoneEntrant entrantModel1 = getEntrantModel(cazId, correlationId, testVrn);
    VehicleEntrantSaveDto entrantDto1 = getEntrantSaveDto(entrantModel1);

    VehicleEntrantsSaveRequestDto vehicleEntrantsSaveRequestDto = new VehicleEntrantsSaveRequestDto(
        cazId, correlationId, newArrayList(entrantDto1)
    );

    createMilitaryVehicle(testVrn);
    createRetrofitVehicle(testVrn);
    createNonUkWhitelistVehicle(testVrn);
    given().mockNtrBulkForNotPresent(testVrn);

    //when
    vehicleEntrantsService.save(vehicleEntrantsSaveRequestDto);

    Iterable<CleanAirZoneEntrant> airZoneEntrants = repository.findAll();
    System.out.println(airZoneEntrants);

  }

  @Nested
  class CompliantStatus {

    @Test
    public void shouldReturnCompliantStatusToAnprWhenVehicleIsRetrofitted() {
      given()
          .prepareVehicleEntrantsWithVrn("CAS301")
          .mockRetrofitVehicle("CAS301")
          .mockTariffWithZeroRates()
          .mockNotFoundInMod()
          .setChargeValidityCode(COMPLIANT_CODE)
          .mockNtrBulkForNotPresent("CAS301")

          .whenCreateResponseToAnprForBath()

          .then()
          .shouldCallNtrBulkOnce()
          .shouldCallTariffOnce()
          .shouldNotCallPayments()
          .shouldSendReportingMessage()

          .andResponse()
          .hasGivenVrn()
          .hasCompliantStatus()
          .hasNoPaymentMethod()
          .tariffCodeIsNull()
          .exemptionCodeIsNull();
    }

    @Test
    public void shouldReturnCompliantWhenChargeIsZero() {
      given()
          .prepareVehicleEntrantsWithVrn("CAS300")
          .mockTariffWithZeroRates()
          .mockNtrBulk("CAS300")
          .setChargeValidityCode(COMPLIANT_CODE)

          .whenCreateResponseToAnprForBath()

          .then()
          .shouldCallNtrBulkOnce()
          .shouldCallTariffOnce()
          .shouldNotCallPayments()
          .shouldSendReportingMessage()

          .andResponse()
          .hasGivenVrn()
          .hasCompliantStatus()
          .hasNoPaymentMethod()
          .tariffCodeIsNull()
          .exemptionCodeIsNull();
    }

    @Test
    public void shouldReturnCompliantStatusToAnprOnWhitelist() {
      given()
          .prepareVehicleEntrantsWithVrn("CAS402")
          .mockWhitelistCompliantStatus()
          .setChargeValidityCode(COMPLIANT_CODE)
          .mockNtrBulk("CAS402")

          .whenCreateResponseToAnprForBath()

          .then()
          .shouldCallNtrBulkOnce()
          .shouldCallTariffOnce()
          .shouldNotCallPayments()
          .shouldSendReportingMessage()

          .andResponse()
          .hasGivenVrn()
          .hasCompliantStatus()
          .hasNoPaymentMethod()
          .exemptionCodeIsNull();
    }

    @Test
    public void shouldReturnCompliantStatusForNotUkVrnAndWhenVrnIsOnWhitelistWithNonUkCategory() {
      given()
          .prepareVehicleEntrantsWithVrn("14PET1")
          .mockNonUkVehicleCategoryInWhitelist()

          .whenCreateResponseToAnprForBath()

          .then()

          .andResponse()
          .hasGivenVrn()
          .hasCompliantStatus()
          .hasNoPaymentMethod()
          .exemptionCodeIsNull();
    }

    @Test
    public void shouldReturnCompliantStatusWhenVehicleShouldNotPay() {
      given()
          .prepareVehicleEntrantsWithVrn("JA07PCB")
          .mockNtrBulkWithoutWac("JA07PCB")
          .mockTariffWithZeroRates()

          .whenCreateResponseToAnprForBath()

          .then()

          .andResponse()
          .hasGivenVrn()
          .hasCompliantStatus()
          .exemptionCodeIsNull();
    }

    @Test
    public void shouldReturnCompliantStatusWhenVehicleShouldNotPayForBirmingham() {
      given()
          .prepareVehicleEntrantsWithVrn("JA07PCB")
          .mockTariffServiceForBirmingham()
          .mockNtrBulk("JA07PCB")

          .whenCreateResponseToAnprForBirmingham()

          .then()

          .andResponse()
          .hasGivenVrn()
          .hasCompliantStatus()
          .exemptionCodeIsNull();
    }

    @Test
    public void shouldReturnCompliantStatusForVrnWithLeadingZerosWhenIsInGpwWithNonUkCategory() {
      given()
          .prepareVehicleEntrantsWithVrn("0A07PCB")
          .mockNonUkVehicleCategoryInWhitelist()
          .mockNtrBulk("0A07PCB")

          .whenCreateResponseToAnprForBath()

          .then()

          .andResponse()
          .hasGivenVrn()
          .hasCompliantStatus()
          .exemptionCodeIsNull();
    }
  }

  @Nested
  class ExemptStatus {

    @ParameterizedTest
    @ValueSource(strings = {"CAS320", "CAS321", "CAS322", "CAS323", "CAS324", "CAS325", "CAS326",
        "CAS327", "CAS328", "CAS329", "CAS330", "CAS331", "CAS332"})
    public void shouldReturnExemptStatusAndExemptionCode(String vrn) {
      given()
          .prepareVehicleEntrantsWithVrn(vrn)
          .mockExemptStatus()
          .mockNtrBulkForNotPresent(vrn)

          .whenCreateResponseToAnprForBath()

          .then()
          .shouldCallNtrBulkOnce()
          .shouldNotCallTariff()
          .shouldNotCallPayments()

          .andResponse()
          .hasGivenVrn()
          .hasExemptStatus()
          .hasNoPaymentMethod()
          .hasNationalExemptionCode();
    }

    @Test
    public void shouldReturnExemptStatusToAnpr() {
      given()
          .prepareVehicleEntrantsWithVrn("CAS312")
          .mockExemptStatus()
          .setChargeValidityCode(EXEMPT_CODE)
          .mockNtrBulkWithoutLicensingAuthorities("CAS312")

          .whenCreateResponseToAnprForBath()

          .then()
          .shouldCallNtrBulkOnce()
          .shouldNotCallTariff()
          .shouldNotCallPayments()
          .shouldSendReportingMessage()

          .andResponse()
          .hasGivenVrn()
          .hasExemptStatus()
          .hasNoPaymentMethod()
          .hasNationalExemptionCode();
    }

    @ParameterizedTest
    @CsvSource({"CAS325,STEAM", "CAS329,electric motorcycle"})
    public void shouldReturnExemptStatusToAnprForFuelAndTaxExemptions(String vrn, String reason) {
      given()
          .prepareVehicleEntrantsWithVrn(vrn)
          .setExemptionReason(reason)
          .setChargeValidityCode(EXEMPT_CODE)
          .mockNotFoundInMod()
          .mockNtrBulk(vrn)

          .whenCreateResponseToAnprForBath()

          .then()
          .shouldNotCallTariff()
          .shouldNotCallPayments()
          .shouldSendReportingMessage()

          .andResponse()
          .hasGivenVrn()
          .hasExemptStatus()
          .hasNoPaymentMethod();
    }

    @Test
    public void shouldReturnExemptStatusToAnprOnWhitelist() {
      given()
          .prepareVehicleEntrantsWithVrn("CAS402")
          .mockWhitelistExemptStatus()
          .setExemptionReason("test")
          .setChargeValidityCode(EXEMPT_CODE)
          .mockNtrBulk("CAS402")
          .mockNotFoundInMod()

          .whenCreateResponseToAnprForBath()
          .shouldSendReportingMessage()

          .then()
          .shouldCallNtrBulkOnce()
          .shouldNotCallTariff()
          .shouldNotCallPayments()

          .andResponse()
          .hasGivenVrn()
          .hasExemptStatus()
          .hasNoPaymentMethod()
          .hasNationalExemptionCode();
    }


    @ParameterizedTest
    @ValueSource(strings = {"PMS331", "PMS325", "PMS326"})
    public void shouldReturnExemptStatusWithDvlaData(String vrn) {
      given()
          .prepareVehicleEntrantsWithVrn(vrn)
          .mockExemptStatus()
          .mockNtrBulk(vrn)

          .whenCreateResponseToAnprForBath()

          .then()
          .shouldCallNtrBulkOnce()
          .shouldNotCallTariff()
          .shouldNotCallPayments()

          .andResponse()
          .hasGivenVrn()
          .hasMake("Mercedes-Benz")
          .hasModel("Ciato")
          .hasColour("Black")
          .hasTypeApproval("N3")
          .hasExemptStatus()
          .hasNoPaymentMethod()
          .hasNationalExemptionCode();
    }

    @Test
    public void shouldReturnExemptStatusWithDvlaDataWhenVehicleIsInModAndDvla() {
      given()
          .prepareVehicleEntrantsWithVrn("PMS115")
          .setExemptionReason("Other")
          .setChargeValidityCode(EXEMPT_CODE)
          .mockMilitaryVehicleForGivenVrn()
          .mockNtrBulkWithoutLicensingAuthorities("PMS115")

          .whenCreateResponseToAnprForBath()
          .shouldSendReportingMessage()

          .then()
          .shouldCallNtrBulkOnce()
          .shouldNotCallTariff()
          .shouldNotCallPayments()

          .andResponse()
          .hasGivenVrn()
          .hasMake("Mercedes-Benz")
          .hasModel("Ciato")
          .hasColour("Black")
          .hasTypeApproval("N3")
          .hasExemptStatus()
          .hasNoPaymentMethod()
          .hasNationalExemptionCode();
    }

    @Test
    public void shouldReturnExemptStatusWithoutDvlaDataWhenVehicleIsInModButIsNotInDvla() {
      given()
          .prepareVehicleEntrantsWithVrn("KP16VDC")
          .setExemptionReason("Other")
          .setChargeValidityCode(EXEMPT_CODE)
          .mockMilitaryVehicleForGivenVrn()
          .mockNtrBulkWithoutLicensingAuthorities("KP16VDC")

          .whenCreateResponseToAnprForBath()
          .shouldSendReportingMessage()

          .then()
          .shouldCallNtrBulkOnce()
          .shouldNotCallTariff()
          .shouldNotCallPayments()

          .andResponse()
          .hasGivenVrn()
          .hasExemptStatus()
          .hasNoPaymentMethod()
          .hasNationalExemptionCode();
    }

    @Test
    public void shouldReturnExemptStatusForVrnWithLeadingZerosWhenIsInMod() {
      given()
          .prepareVehicleEntrantsWithVrn("0P16VDC")
          .setExemptionReason("Other")
          .setChargeValidityCode(EXEMPT_CODE)
          .mockMilitaryVehicleForGivenVrn()
          .mockNtrBulkWithoutLicensingAuthorities("0P16VDC")

          .whenCreateResponseToAnprForBath()

          .then()
          .shouldSendReportingMessage()

          .andResponse()
          .hasGivenVrn()
          .hasExemptStatus()
          .hasNoPaymentMethod()
          .hasNationalExemptionCode();
    }
  }

  @Nested
  class NotCompliantStatus {

    @Test
    public void shouldReturnNotCompliantNotPaidStatusWhenPaymentStatusIsNotPaid() {
      given()
          .prepareVehicleEntrantsWithVrn("CAS305")
          .mockNotCompliantNotPaidStatusWhenPaymentStatusIsNotPaid()
          .setChargeValidityCode(NOT_COMPLIANT_CODE)

          .whenCreateResponseToAnprForBath()

          .then()
          .shouldCallNtrBulkOnce()
          .shouldCallTariffOnce()
          .shouldCallPaymentsForUkEntrant()
          .shouldSendReportingMessage()

          .andResponse()
          .hasGivenVrn()
          .hasNotCompliantNotPaidStatus()
          .hasNoPaymentMethod()
          .hasTariffCode("BTH01-TAXI")
          .exemptionCodeIsNull();
    }

    @Test
    public void shouldReturnNotCompliantNotPaidStatusWhenPaymentStatusIsRefunded() {
      given()
          .prepareVehicleEntrantsWithVrn("CAS305")
          .mockNotCompliantNotPaidStatusWhenPaymentStatusIsRefunded()
          .setChargeValidityCode(NOT_COMPLIANT_CODE)

          .whenCreateResponseToAnprForBath()

          .then()
          .shouldCallNtrBulkOnce()
          .shouldCallTariffOnce()
          .shouldCallPaymentsForUkEntrant()
          .shouldSendReportingMessage()

          .andResponse()
          .hasGivenVrn()
          .hasNotCompliantNotPaidStatus()
          .hasNoPaymentMethod()
          .hasTariffCode("BTH01-TAXI")
          .exemptionCodeIsNull();
    }

    @Test
    public void shouldReturnNotCompliantNotPaidStatusWhenPaymentStatusIsChargeback() {
      given()
          .prepareVehicleEntrantsWithVrn("CAS305")
          .mockNotCompliantNotPaidStatusWhenPaymentStatusIsChargeback()
          .setChargeValidityCode(NOT_COMPLIANT_CODE)

          .whenCreateResponseToAnprForBath()

          .then()
          .shouldCallNtrBulkOnce()
          .shouldCallTariffOnce()
          .shouldCallPaymentsForUkEntrant()
          .shouldSendReportingMessage()

          .andResponse()
          .hasGivenVrn()
          .hasNotCompliantNotPaidStatus()
          .hasNoPaymentMethod()
          .hasTariffCode("BTH01-TAXI")
          .exemptionCodeIsNull();
    }

    @Test
    public void shouldReturnNotCompliantPaidStatusToAnpr() {
      given()
          .prepareVehicleEntrantsWithVrn("CAS305")
          .mockNotCompliantPaidByDirectDebitStatus()
          .setChargeValidityCode(NOT_COMPLIANT_CODE)

          .whenCreateResponseToAnprForBath()

          .then()
          .shouldCallNtrBulkOnce()
          .shouldCallTariffOnce()
          .shouldCallPaymentsForUkEntrant()
          .shouldSendReportingMessage()

          .andResponse()
          .hasGivenVrn()
          .hasNotCompliantPaidStatus()
          .hasPaymentMethodByDirectDebit()
          .hasTariffCode("BTH01-TAXI")
          .exemptionCodeIsNull();
    }
  }

  @Nested
  class UnrecognisedStatus {

    @Test
    public void shouldReturnUnrecognisedNotPaidStatusToAnpr() {
      given()
          .prepareVehicleEntrantsWithVrn("CAS307BE")
          .mockPaymentServiceWithStatusNotPaidForCAS307BE()
          .setChargeValidityCode(UNRECOGNISED_CODE)

          .whenCreateResponseToAnprForBath()

          .then()
          .shouldCallPaymentsForNonUkEntrant()
          .shouldNotCallNtrBulk()
          .shouldNotCallTariff()
          .shouldSendReportingMessage()

          .andResponse()
          .hasGivenVrn()
          .hasUnrecognisedNotPaidStatus()
          .hasNoPaymentMethod()
          .tariffCodeIsNull()
          .exemptionCodeIsNull();
    }

    @Test
    public void shouldReturnUnrecognisedPaidStatusToAnpr() {
      given()
          .prepareVehicleEntrantsWithVrn("CAS307BE")
          .mockUnrecognisedPaidStatusForCAS307BE()
          .setChargeValidityCode(UNRECOGNISED_CODE)

          .whenCreateResponseToAnprForBath()

          .then()
          .shouldCallPaymentsForNonUkEntrant()
          .shouldNotCallNtr()
          .shouldNotCallTariff()
          .shouldSendReportingMessage()

          .andResponse()
          .hasGivenVrn()
          .hasUnrecognisedPaidStatus()
          .hasPaymentMethodByCard()
          .hasTariffCode(PAID_PAYMENT_TARIFF_CODE)
          .exemptionCodeIsNull();
    }

    @Test
    public void shouldReturnUnrecognisedPaidStatusWhenThereIsNoFuelType() {
      given()
          .prepareVehicleEntrantsWithVrn("TST010")
          .mockPaymentServiceWithStatusPaidForTST010()
          .mockNtrBulk("TST010")

          .whenCreateResponseToAnprForBath()

          .then()

          .andResponse()
          .hasGivenVrn()
          .hasMake("Mercedes-Benz")
          .hasModel("Ciato")
          .hasColour("Black")
          .hasTypeApproval("N3")
          .hasUnrecognisedPaidStatus()
          .hasPaymentMethodByCard()
          .exemptionCodeIsNull();
    }

    @Test
    public void shouldReturnUnrecognisedNotPaidStatusForVrnWithLeadingZeros() {
      given()
          .prepareVehicleEntrantsWithVrn("0AS307BE")
          .mockPaymentServiceWithStatusNotPaidForNonUkFor0AS307BE()
          .setChargeValidityCode(UNRECOGNISED_CODE)
          .mockNtrBulkWithoutLicensingAuthorities("0AS307BE")

          .whenCreateResponseToAnprForBath()

          .then()
          .shouldSendReportingMessage()

          .andResponse()
          .hasGivenVrn()
          .hasUnrecognisedNotPaidStatus()
          .tariffCodeIsNull()
          .exemptionCodeIsNull();
    }
  }

  @Nested
  class WhitelistCategory {

    /*
     in this scenario status will be "exempt"
     we have status "exempt" in vehicle-details.json for CAS321
     we have status "compliant" in WL for Early_Adopter category
    */
    @Test
    public void shouldReturnExemptStatusWhenCategoryIsEarlyAdopterInWhitelist() {
      given()
          .prepareVehicleEntrantsWithVrn("CAS321")
          .mockEarlyAdopterCategoryInWhitelist()
          .mockTariffServiceForBath()
          .mockNtrBulk("CAS321")
          .setChargeValidityCode(EXEMPT_CODE)

          .whenCreateResponseToAnprForBath()

          .then()
          .shouldCallNtrBulkOnce()
          .shouldSendReportingMessage()

          .andResponse()
          .hasGivenVrn()
          .hasExemptStatus()
          .hasNoPaymentMethod()
          .tariffCodeIsNull()
          .hasNationalExemptionCode();
    }

    /*
       in this scenario status will be "exempt"
       we have status "exempt" in vehicle-details.json for CAS321
       we have status "compliant" in WL for Non Uk category
      */
    @Test
    public void shouldReturnExemptStatusWhenCategoryIsNonUKInWhitelist() {
      given()
          .prepareVehicleEntrantsWithVrn("CAS321")
          .setChargeValidityCode(COMPLIANT_CODE)
          .mockNonUkVehicleCategoryInWhitelist()
          .mockNotCompliantNotPaidStatusWhenPaymentStatusIsChargeback()
          .whenCreateResponseToAnprForBath()
          .then()
          .shouldNotCallNtr()
          .shouldSendReportingMessageWithoutDvlaData()
          .andResponse()
          .hasGivenVrn()
          .hasCompliantStatus()
          .hasNoPaymentMethod()
          .tariffCodeIsNull()
          .exemptionCodeIsNull();
    }

    /*
       in this scenario status will be "exempt"
       we have status "compliant" in vehicle-details.json for CAS303
       we have status "exempt" in WL for Problematic VRN category
      */
    @Test
    public void shouldReturnExemptStatusWhenCategoryIsProblematicVrnInWhitelist() {
      given()
          .prepareVehicleEntrantsWithVrn("CAS303")
          .setChargeValidityCode(EXEMPT_CODE)
          .mockProblematicVRNCategoryInWhitelist()
          .mockCompliantStatus()

          .whenCreateResponseToAnprForBath()

          .then()
          .shouldNotCallNtr()
          .shouldSendReportingMessageWithoutDvlaData()
          .andResponse()
          .hasGivenVrn()
          .hasExemptStatus()
          .hasNoPaymentMethod()
          .tariffCodeIsNull()
          .hasNationalExemptionCode();
    }

    /*
       in this scenario status will be "exempt"
       we have status "compliant" in vehicle-details.json for CAS304
       we have status "exempt" in WL for Exemption category
      */
    @Test
    public void shouldReturnExemptStatusWhenCategoryIsExemptionInWhitelist() {
      given()
          .prepareVehicleEntrantsWithVrn("CAS304")
          .setChargeValidityCode(EXEMPT_CODE)
          .mockExemptionCategoryInWhitelist()
          .mockTariffServiceForBath()
          .mockNtrBulk("CAS304")

          .whenCreateResponseToAnprForBath()

          .then()
          .shouldCallNtrBulkOnce()
          .shouldSendReportingMessage()

          .andResponse()
          .hasGivenVrn()
          .hasExemptStatus()
          .hasNoPaymentMethod()
          .tariffCodeIsNull()
          .hasNationalExemptionCode();
    }

    /*
       in this scenario status will be "exempt"
       we have status "compliant" in vehicle-details.json for CAS314
       we have status "exempt" in WL for Other category
      */
    @Test
    public void shouldReturnExemptStatusWhenCategoryIsOtherInWhitelist() {
      given()
          .prepareVehicleEntrantsWithVrn("CAS314")
          .setChargeValidityCode(EXEMPT_CODE)
          .mockOtherCategoryInWhitelist()
          .mockTariffServiceForBath()
          .mockNtrBulk("CAS314")

          .whenCreateResponseToAnprForBath()

          .then()
          .shouldCallNtrBulkOnce()
          .shouldSendReportingMessage()

          .andResponse()
          .hasGivenVrn()
          .hasExemptStatus()
          .hasNoPaymentMethod()
          .tariffCodeIsNull()
          .hasNationalExemptionCode();
    }

  }

  private void mockPayments(String request, String responseFile) {
    mockServer.when(requestPost("/v1/payments/vehicle-entrants", request), exactly(1))
        .respond(paymentV1Response(responseFile));
  }

  private CleanAirZoneEntrant getEntrantModel(UUID cazId, String correlationId, String vrn) {
    LocalDateTime entrantTimestamp = LocalDateTime.parse(SAMPLE_ENTRANT_DATE, FORMATTER);

    CleanAirZoneEntrant entrantModel = new CleanAirZoneEntrant(cazId, correlationId,
        entrantTimestamp);
    entrantModel.setVrn(vrn);
    entrantModel.setChargeValidityCode(new ChargeValidity("CVC04"));

    return entrantModel;
  }

  private VehicleEntrantSaveDto getEntrantSaveDto(CleanAirZoneEntrant model) {
    LocalDateTime timestamp = LocalDateTime.parse("2017-10-01T155301Z", FORMATTER);

    return new VehicleEntrantSaveDto(model.getVrn(), timestamp);
  }

  private void verifyReportingDataMessageIsSent(ArrayList<VehicleEntrantSaveDto> vehicleEntrants,
      String nonStandardUkFormattedPlateVehicle) {
    List<Message> messages = sqsTestUtility.receiveSqsMessages(queueName);
    for (VehicleEntrantSaveDto entrant : vehicleEntrants) {
      String hashedVrn = VRNS_TO_HASHED_VRNS.get(entrant.getVrn());
      assertTrue(findAttributeInMessages(messages, hashedVrn, nonStandardUkFormattedPlateVehicle)
          .isPresent());
    }
  }


  private Optional<Message> findAttributeInMessages(List<Message> messages, String attribute,
      String nonStandardUkFormattedPlateVehicle) {
    return messages.stream()
        .filter(message -> message.getBody().contains(attribute) && message.getBody()
            .contains(nonStandardUkFormattedPlateVehicle))
        .findAny();
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
    given().mockMilitaryVehicle(testVrn);
  }

  @SneakyThrows
  private void createNonUkWhitelistVehicle(String vrn) {
    GeneralWhitelistVehicle whitelistVehicle = GeneralWhitelistVehicle.builder()
        .vrn(vrn)
        .category("Non-UK Vehicle")
        .exempt(true)
        .compliant(false)
        .reasonUpdated("test")
        .updateTimestamp(java.time.LocalDateTime.now())
        .uploaderId(UUID.fromString("23a84d23-a45a-4ce1-aa74-df2058c93289"))
        .build();
    generalWhitelistRepository.save(whitelistVehicle);
  }

  AnprAssertion given() {
    return new AnprAssertion(vehicleEntrantsService, nationalTaxiRegisterService,
        generalWhitelistRepository, retrofitRepository, vehicleRepository, mockServer, repository,
        sqsTestUtility, queueName, objectMapper);
  }
}
