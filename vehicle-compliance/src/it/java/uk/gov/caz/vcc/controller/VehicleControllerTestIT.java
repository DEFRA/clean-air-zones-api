package uk.gov.caz.vcc.controller;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import uk.gov.caz.vcc.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.vcc.domain.GeneralWhitelistVehicle;
import uk.gov.caz.vcc.domain.RetrofittedVehicle;
import uk.gov.caz.vcc.repository.GeneralWhitelistRepository;
import uk.gov.caz.vcc.repository.LocalVehicleDetailsRepository;
import uk.gov.caz.vcc.repository.NationalTaxiRegisterRepository;
import uk.gov.caz.vcc.repository.RetrofitRepository;
import uk.gov.caz.vcc.util.MockServerTestIT;
import uk.gov.caz.vcc.util.TestFixturesLoader;

/**
 * Integration tests for the VehicleController layer: https://spring.io/guides/gs/testing-web/
 */
@TestInstance(Lifecycle.PER_CLASS)
@FullyRunningServerIntegrationTest
@Sql(scripts = "classpath:data/sql/clear-registered-details.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class VehicleControllerTestIT extends MockServerTestIT {

  private static final String IMAGINARY_CAZ = "0d7ab5c4-5fff-4935-8c4e-56267c0c9493";
  
  private static final String IMAGINARY_CAZ_TWO = "131af03c-f7f4-4aef-81ee-aae4f56dbeb5";
  
  private static final String BOTH_IMAGINARY_CAZ = String.format("%s,%s", IMAGINARY_CAZ, IMAGINARY_CAZ_TWO);

  @Autowired
  private LocalVehicleDetailsRepository localVehicleDetailsRepository;

  @Autowired
  private TestFixturesLoader testFixturesLoader;

  @Autowired
  private CacheInvalidationsController cacheInvalidationsController;

  @Autowired
  private NationalTaxiRegisterRepository nationalTaxiRegisterRepository;

  @Autowired
  private GeneralWhitelistRepository generalWhitelistRepository;

  @Autowired
  private RetrofitRepository retrofitRepository;

  private static ObjectMapper objectMapper = new ObjectMapper();
  
  private static final List<String> VALID_TYPE_APPROVALS = Arrays.asList("M1", "M2", "M3", "N1", "N2", "N3",
      "L1", "L2", "L3", "L4", "L5", "L6", "L7", "T1", "T2", "T3", "T4", "T5");

  @LocalServerPort
  int randomServerPort;

  @BeforeAll
  public void before() throws IOException {
    testFixturesLoader.loadTestVehiclesIntoDb();
  }

  @BeforeEach
  public void setup() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
  }

  @AfterEach
  public void cleanup() {
    VehicleControllerTestIT.mockServer.reset();
    nationalTaxiRegisterRepository.cacheEvictLicenseInfo();
    cacheInvalidationsController.cacheEvictVehicles();
    cacheInvalidationsController.cacheEvictCleanAirZones();
  }

  @AfterAll
  public void after() {
    localVehicleDetailsRepository.deleteAll();
  }


  @ParameterizedTest
  @MethodSource("streamVehicleArguments")
  void testVehicleDetails(VehicleWithExpectedValues v) {
    whenVehicleIsNotInTaxiDb(v.registrationNumber);

    try {
      RestAssured.
          given()
          .header("X-Correlation-Id", "05fe5e4c-a798-4994-a3dd-6f0fb7fbfbda").
          when()
          .get("/v1/compliance-checker/vehicles/{vrn}/details", v.registrationNumber).
          then()
          .statusCode(200)
          .body("registrationNumber", equalTo(v.registrationNumber))
          .body("typeApproval", VALID_TYPE_APPROVALS.contains(v.typeApprovalCategory)? 
              equalTo(v.typeApprovalCategory):equalTo(""))
          .body("type", equalTo(v.expectedOutcomes.type))
          .body("make", equalTo(v.make))
          .body("model", equalTo(v.model))
          .body("colour", equalTo(v.colour))
          .body("fuelType", equalTo(v.fuelType))
          .body("exempt", equalTo(v.expectedOutcomes.exempt));
    } catch (AssertionError e) {
      throw new AssertionError(
          "AssertionError encountered whilst testing vehicle details with vrn: "
              + v.registrationNumber, e);
    } catch (Exception e) {
      throw new RuntimeException(
          "Exception encountered whilst testing vehicle details with vrn: " + v.registrationNumber,
          e);
    }
  }

  @ParameterizedTest
  @MethodSource("streamVehicleArguments")
  void testVehicleCompliance(VehicleWithExpectedValues v) {
    whenVehicleIsNotInTaxiDb(v.registrationNumber);
    mockTariffCall(IMAGINARY_CAZ, "tariff-rates-imaginary-caz.json");
    mockCazListCall("caz-first-response.json");

    Response complianceResponse =
        RestAssured.
            given()
            .header("X-Correlation-Id", "05fe5e4c-a798-4994-a3dd-6f0fb7fbfbda").
            when()
            .get("/v1/compliance-checker/vehicles/{vrn}/compliance?zones={zone}",
                v.registrationNumber, IMAGINARY_CAZ);

    try {
      if (v.expectedOutcomes.exempt) {
        complianceResponse.
            then()
            .statusCode(200)
            .body("isExempt", equalTo(true))
            .body("complianceOutomes", equalTo(null));
      } else if (v.expectedOutcomes.compliant == null || v.expectedOutcomes.type.equals("null")) {
        complianceResponse.
            then()
            .statusCode(422);
      } else {
        complianceResponse.
            then()
            .statusCode(200)
            .body("registrationNumber", equalTo(v.registrationNumber))
            .body("complianceOutcomes[0].charge", is(v.expectedOutcomes.compliant ? 0.0f : 10.0f));
      }
    } catch (AssertionError e) {
      throw new AssertionError(
          "AssertionError encountered whilst testing vehicle details with vrn: "
              + v.registrationNumber, e);
    }
  }

  @Test
  void testVehicleComplianceWhenNoZonesParameterPassed() {
    String vrn = "CAS300";
    whenVehicleIsNotInTaxiDb(vrn);
    mockCazListCall("caz-first-response.json");
    mockTariffCall(IMAGINARY_CAZ, "tariff-rates-imaginary-caz.json");

    RestAssured.
        given()
        .header("X-Correlation-Id", "05fe5e4c-a798-4994-a3dd-6f0fb7fbfbda").
        when()
        .get("/v1/compliance-checker/vehicles/{vrn}/compliance", vrn)
        .then()
        .statusCode(200)
        .body("registrationNumber", equalTo(vrn))
        .body("complianceOutcomes[0].charge", is(10.0f));
  }
  
  @Test
  public void testVehicleComplianceTaxiNullEuroStatus() {
    mockCazListCall("caz-first-response.json");
    mockTariffCall(IMAGINARY_CAZ, "tariff-rates-imaginary-caz.json");
    mockTariffCall(IMAGINARY_CAZ_TWO, "tariff-rates-imaginary-caz.json");
    String vrn = "JA07PCB";
    whenVehicleIsInTaxiDb(vrn, "ntr-bath-response.json");
    Response complianceResponse =
        RestAssured.
            given()
            .header("X-Correlation-Id", "05fe5e4c-a798-4994-a3dd-6f0fb7fbfbda").
            when()
            .get("/v1/compliance-checker/vehicles/{vrn}/compliance?zones={zone}",
                vrn, BOTH_IMAGINARY_CAZ);
    
    complianceResponse.
    then()
    .statusCode(200)
    .body("registrationNumber", equalTo(vrn))
    .body("complianceOutcomes[0].charge", is(0.0f));
    
  }

  @Test
  public void testBulkVehicleComplianceWithStatedZoneId() throws IOException {
    whenCazInfoIsInTariffService("/v1/clean-air-zones","caz-imaginary-caz-response.json");
    whenEachCazHasTariffInfo(IMAGINARY_CAZ, "tariff-rates-imaginary-caz.json");
    whenEachCazHasTariffInfo(IMAGINARY_CAZ_TWO, "tariff-rates-imaginary-caz.json");
    String vrn = "CAS310";
    List<String> vrns = Arrays.asList(vrn);
    whenVehicleIsNotInTaxiDb(vrn);

    Response complianceResponse =
        RestAssured.
            given()
            .header("X-Correlation-Id", "05fe5e4c-a798-4994-a3dd-6f0fb7fbfbda")
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .when()
            .body(objectMapper.writeValueAsString(vrns))
            .post("/v1/compliance-checker/vehicles/bulk-compliance?zones={zone}", IMAGINARY_CAZ);
    complianceResponse.then()
        .statusCode(200)
        .body("[0].isRetrofitted", equalTo(false))
        .body("[0].isExempt", equalTo(false))
        .body("[0].registrationNumber", equalTo(vrn))
        .body("[0].complianceOutcomes[0].cleanAirZoneId", equalTo(IMAGINARY_CAZ))
        .body("[0].complianceOutcomes[0].charge", is(10.0f))
        .body("[0].complianceOutcomes[0].size()", is(6)); //The value 6 accounts for the VRN, vehicle type, 2 CAZ charges and notes
  }

  @Test
  public void testBulkVehicleComplianceWithoutZones() throws IOException {
    whenCazInfoIsInTariffService("/v1/clean-air-zones","caz-imaginary-caz-response.json");
    whenEachCazHasTariffInfo(IMAGINARY_CAZ, "tariff-rates-imaginary-caz.json");
    whenEachCazHasTariffInfo(IMAGINARY_CAZ_TWO, "tariff-rates-imaginary-caz.json");
    String vrn = "CAS310";
    List<String> vrns = Arrays.asList(vrn);
    whenVehicleIsNotInTaxiDb(vrn);

    Response complianceResponse =
        RestAssured.
            given()
            .header("X-Correlation-Id", "05fe5e4c-a798-4994-a3dd-6f0fb7fbfbda")
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .when()
            .body(objectMapper.writeValueAsString(vrns))
            .post("/v1/compliance-checker/vehicles/bulk-compliance");
    complianceResponse.then()
        .statusCode(200)
        .body("[0].isRetrofitted", equalTo(false))
        .body("[0].isExempt", equalTo(false))
        .body("[0].registrationNumber", equalTo(vrn))
        .body("[0].complianceOutcomes[0].cleanAirZoneId", equalTo(IMAGINARY_CAZ))
        .body("[0].complianceOutcomes[0].charge", is(10.0f))
        .body("[0].complianceOutcomes[0].size()", is(6))
        .body("[0].complianceOutcomes[1].size()", is(6));
  }
  
  
  @Test
  public void shouldReturn404IfDetailsNotFound() throws Exception {
    whenVehicleIsNotInTaxiDb("VEH404");

    RestAssured.
        given()
        .header("X-Correlation-Id", "05fe5e4c-a798-4994-a3dd-6f0fb7fbfbda").
        when()
        .get("/v1/compliance-checker/vehicles/VEH404/details").
        then()
        .statusCode(404);
  }

  @Test
  public void shouldReturnVehicleDetailsWithoutLicensingAuthoritiesNames() throws Exception {
    String vrn = "CAS300";
    whenVehicleIsNotInTaxiDb(vrn);

    RestAssured.
        given()
        .header("X-Correlation-Id", "05fe5e4c-a798-4994-a3dd-6f0fb7fbfbda").
        when()
        .get("/v1/compliance-checker/vehicles/{vrn}/details", vrn).
        then()
        .statusCode(200)
        .body("registrationNumber", equalTo(vrn))
        .body("taxiOrPhv", equalTo(false))
        .body("licensingAuthoritiesNames", equalTo(emptyList()));
  }

  @Test
  public void taxiQueryParamTrueTreatsVehicleAsTaxi() throws Exception {
    // If isTaxiOrPhv is true, that should be used instead of referring to the NTR and
    // the vehicle treated as being a taxi for compliance calculation purposes.
    String vrn = "CAS300";
    boolean isTaxiOrPhv = true;

    whenVehicleIsNotInTaxiDb(vrn);
    mockTariffCall(IMAGINARY_CAZ, "tariff-rates-imaginary-caz.json");
    mockCazListCall("caz-first-response.json");

    RestAssured.
        given()
        .header("X-Correlation-Id", "05fe5e4c-a798-4994-a3dd-6f0fb7fbfbda").
        when()
        .get(
            "/v1/compliance-checker/vehicles/{vrn}/compliance?zones={bathCaz}&isTaxiOrPhv={isTaxiOrPhv}",
            vrn, IMAGINARY_CAZ, isTaxiOrPhv).
        then()
        .statusCode(200)
        .body("registrationNumber", equalTo(vrn))
        .body("isExempt", equalTo(false))
        .body("complianceOutcomes[0].cleanAirZoneId", equalTo(IMAGINARY_CAZ))
        .body("complianceOutcomes[0].charge", is(10.0f));
  }

  @Test
  public void dvlaDataShouldBeFetchedIfFoundInDVLA() {
    String vrn = "CAS300";

    RestAssured.
        given()
        .header("X-Correlation-Id", "05fe5e4c-a798-4994-a3dd-6f0fb7fbfbda").
        when()
        .get(
            "/v1/compliance-checker/vehicles/{vrn}/external-details",
            vrn).
        then()
        .statusCode(200)
        .body("colour", equalTo("Grey"))
        .body("typeApproval", equalTo("M1"))
        .body("make", equalTo("Hyundai"))
        .body("model", equalTo("i20"))
        .body("fuelType", equalTo("Petrol"))
        .body("euroStatus", equalTo("EURO 3"))
    ;
  }

  @Nested
  class RegisterDetails {

    @Nested
    class ShouldBeExempt{

      @Test
      void shouldBeInGpw() {
        String vrn = "PMS310";
        mockWhitelistVehicle(vrn, false, true);

        RestAssured
            .given()
            .header("X-Correlation-Id", "05fe5e4c-a798-4994-a3dd-6f0fb7fbfbda")
            .when()
            .get("/v1/compliance-checker/vehicles/{vrn}/register-details", vrn)
            .then()
            .statusCode(200)
            .body("registerCompliant", is(false))
            .body("registerExempt", is(true))
            .body("registeredMOD", is(false))
            .body("registeredGPW", is(true))
            .body("registeredNTR", is(false))
            .body("registeredRetrofit", is(false));
      }

      @Test
      void shouldBeInMod() {
        String vrn = "PMS311";
        mockMilitaryVehicle(vrn);

        RestAssured
            .given()
            .header("X-Correlation-Id", "05fe5e4c-a798-4994-a3dd-6f0fb7fbfbda")
            .when()
            .get("/v1/compliance-checker/vehicles/{vrn}/register-details", vrn)
            .then()
            .statusCode(200)
            .body("registerCompliant", is(false))
            .body("registerExempt", is(true))
            .body("registeredMOD", is(true))
            .body("registeredGPW", is(false))
            .body("registeredNTR", is(false))
            .body("registeredRetrofit", is(false));
      }
    }

    @Nested
    class ShouldBeCompliant{

      @Test
      void shouldBeInGpw() {
        String vrn = "PMS312";
        mockWhitelistVehicle(vrn, true, false);

        RestAssured
            .given()
            .header("X-Correlation-Id", "05fe5e4c-a798-4994-a3dd-6f0fb7fbfbda")
            .when()
            .get("/v1/compliance-checker/vehicles/{vrn}/register-details", vrn)
            .then()
            .statusCode(200)
            .body("registerCompliant", is(true))
            .body("registerExempt", is(false))
            .body("registeredMOD", is(false))
            .body("registeredGPW", is(true))
            .body("registeredNTR", is(false))
            .body("registeredRetrofit", is(false));
      }

      @Test
      void shouldBeInRetrofit() {
        String vrn = "PMS313";
        mockRetrofitVehicle(vrn);

        RestAssured
            .given()
            .header("X-Correlation-Id", "05fe5e4c-a798-4994-a3dd-6f0fb7fbfbda")
            .when()
            .get("/v1/compliance-checker/vehicles/{vrn}/register-details", vrn)
            .then()
            .statusCode(200)
            .body("registerCompliant", is(true))
            .body("registerExempt", is(false))
            .body("registeredMOD", is(false))
            .body("registeredGPW", is(false))
            .body("registeredNTR", is(false))
            .body("registeredRetrofit", is(true));
      }
    }

    @Test
    void shouldBeInNtr() {
      String vrn = "PMS314";
      whenVehicleIsInTaxiDb(vrn);

      RestAssured
          .given()
          .header("X-Correlation-Id", "05fe5e4c-a798-4994-a3dd-6f0fb7fbfbda")
          .when()
          .get("/v1/compliance-checker/vehicles/{vrn}/register-details", vrn)
          .then()
          .statusCode(200)
          .body("registerCompliant", is(false))
          .body("registerExempt", is(false))
          .body("registeredMOD", is(false))
          .body("registeredGPW", is(false))
          .body("registeredNTR", is(true))
          .body("registeredRetrofit", is(false));
    }

    @Test
    void shouldBeCompliantAndExemptInGpw() {
      String vrn = "PMS315";
      mockWhitelistVehicle(vrn, true, true);

      RestAssured
          .given()
          .header("X-Correlation-Id", "05fe5e4c-a798-4994-a3dd-6f0fb7fbfbda")
          .when()
          .get("/v1/compliance-checker/vehicles/{vrn}/register-details", vrn)
          .then()
          .statusCode(200)
          .body("registerCompliant", is(true))
          .body("registerExempt", is(true))
          .body("registeredMOD", is(false))
          .body("registeredGPW", is(true))
          .body("registeredNTR", is(false))
          .body("registeredRetrofit", is(false));
    }
  }

  @Nested
  class PhgvDiscountAvailability {

    @Nested
    class ForCompliantVehicles {

      @Test
      void shouldReturnPhgvDiscountAvailabilityFlag() {
        String vrn = "IS20ABH";
        mockTariffsCalls();

        RestAssured
            .given()
            .header("X-Correlation-Id", "05fe5e4c-a798-4994-a3dd-6f0fb7fbfbda")
            .when()
            .get("/v1/compliance-checker/vehicles/{vrn}/compliance", vrn)
            .then()
            .statusCode(200)
            .body("phgvDiscountAvailable", equalTo(true));
      }
    }

    @Nested
    class ForNonCompliantVehicles {

      @ParameterizedTest
      @ValueSource(strings = {"IS20ABA", "IS20ABB", "IS20ABC", "IS20ABD"})
      void shouldReturnPhgvDiscountAvailabilityFlagSetToTrue(String vrn) {
        mockTariffsCalls();

        RestAssured
            .given()
            .header("X-Correlation-Id", "05fe5e4c-a798-4994-a3dd-6f0fb7fbfbda")
            .when()
            .get("/v1/compliance-checker/vehicles/{vrn}/compliance", vrn)
            .then()
            .statusCode(200)
            .body("phgvDiscountAvailable", equalTo(true));
      }

      @ParameterizedTest
      @ValueSource(strings = {"IS20ABE", "IS20ABF"})
      void shouldReturnPhgvDiscountAvailabilityFlagSetToFalse(String vrn) {
        mockTariffsCalls();

        RestAssured
            .given()
            .header("X-Correlation-Id", "05fe5e4c-a798-4994-a3dd-6f0fb7fbfbda")
            .when()
            .get("/v1/compliance-checker/vehicles/{vrn}/compliance", vrn)
            .then()
            .statusCode(200)
            .body("phgvDiscountAvailable", equalTo(false));
      }
      
      @Test
      void shouldReturnPhgvDiscountAvailabilityFlagAsFalseWhenNotApplicableToVehicleType() {
        String vrn = "IS20ABG";
        mockTariffsCalls();

        RestAssured
            .given()
            .header("X-Correlation-Id", "05fe5e4c-a798-4994-a3dd-6f0fb7fbfbda")
            .when()
            .get("/v1/compliance-checker/vehicles/{vrn}/compliance", vrn)
            .then()
            .statusCode(200)
            .body("phgvDiscountAvailable", equalTo(false));
      }
    }

    private void mockTariffsCalls() {
      invalidateTariffsCache();
      mockTariffCall(IMAGINARY_CAZ, "tariff-rates-imaginary-caz.json");
      mockCazListCall("caz-first-response.json");
    }

    private void invalidateTariffsCache() {
      cacheInvalidationsController.cacheEvictCleanAirZones();
    }
  }

  public static Stream<Arguments> streamVehicleArguments() throws IOException {
    VehicleWithExpectedValues[] testVehicles = loadTestVehicles();
    return Arrays.stream(testVehicles).map(v -> Arguments.of(v));
  }

  private static VehicleWithExpectedValues[] loadTestVehicles() throws IOException {
    File vehicleDetailsJson = new ClassPathResource("/db/fixtures/vehicle-details.json").getFile();

    VehicleWithExpectedValues[] testVehicles =
        objectMapper.readValue(vehicleDetailsJson, VehicleWithExpectedValues[].class);

    return testVehicles;
  }

  public void mockWhitelistVehicle(String vrn, boolean compliant, boolean exempt) {
    GeneralWhitelistVehicle whitelistVehicle = GeneralWhitelistVehicle.builder()
        .vrn(vrn)
        .category("Early Adopter")
        .exempt(exempt)
        .compliant(compliant)
        .reasonUpdated("test")
        .updateTimestamp(LocalDateTime.now())
        .uploaderId(UUID.fromString("23a84d23-a45a-4ce1-aa74-df2058c93289"))
        .build();
    generalWhitelistRepository.save(whitelistVehicle);
  }

  public void mockMilitaryVehicle(String vrn) {
    mockServer.when(requestGet("/v1/mod/" + vrn))
        .respond(response("mod-vehicle-response.json"));
  }

  private void mockRetrofitVehicle(String vrn) {
    RetrofittedVehicle r = new RetrofittedVehicle();
    r.setVrn(vrn);
    r.setWhitelistDiscountCode("Sample retrofit code.");
    r.setDateOfRetrofit(new java.sql.Date(Calendar.getInstance().getTime().getTime()));

    retrofitRepository.save(r);
  }

  private static class VehicleWithExpectedValues {

    public String description;
    public String registrationNumber;
    public String colour;
    public Date dateOfFirstRegistration;
    public String euroStatus;
    public String typeApprovalCategory;
    public Integer massInService;
    public String bodyType;
    public String make;
    public String model;
    public Integer revenueWeight;
    public Integer seatingCapacity;
    public Integer standingCapacity;
    public String taxClass;
    public String fuelType;
    public ExpectedOutcomes expectedOutcomes;
  }

  private static class ExpectedOutcomes {

    public String type;
    public Boolean exempt;
    public Boolean compliant;
  }

}
