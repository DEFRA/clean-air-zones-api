package uk.gov.caz.vcc;

import static org.hamcrest.core.Is.is;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import uk.gov.caz.vcc.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.vcc.repository.CleanAirZoneEntrantRepository;
import uk.gov.caz.vcc.repository.GeneralWhitelistRepository;
import uk.gov.caz.vcc.repository.RetrofitRepository;
import uk.gov.caz.vcc.repository.VehicleDetailsRepository;
import uk.gov.caz.vcc.service.CazTariffService;
import uk.gov.caz.vcc.service.NationalTaxiRegisterService;
import uk.gov.caz.vcc.service.VehicleEntrantsService;
import uk.gov.caz.vcc.util.MockServerTestIT;

@FullyRunningServerIntegrationTest
public class VehicleEntrantsIT extends MockServerTestIT {

  public static final String VRN_1 = "CAS334";
  public static final String VRN_2 = "CAS335";

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
  private VehicleDetailsRepository vehicleRepository;

  @Autowired
  private ObjectMapper objectMapper;

  @LocalServerPort
  int randomServerPort;

  @BeforeEach
  public void setup() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
  }

  @AfterEach
  @BeforeEach
  public void cleanup() {
    mockServer.reset();
    tariffService.cacheEvictCleanAirZones();
    repository.deleteAll();
    retrofitRepository.deleteAll();
  }

  @Test
  public void shouldThrow503AndNotPersistAnythingIfExceptionWasThrownInTheMiddleOfProcessing() {
    given()
        .prepareVehicleEntrantsWithVrn(VRN_1)
        .prepareVehicleEntrantsWithVrn(VRN_2)
        .mockNotCompliantPaidStatus(VRN_1)
        .mockErrorFromNtr(VRN_2)

        .andCallVehicleEntrantsEndpoint("vehicle-entrants-two.json")

        .then()
        .thereShouldBeNoEntriesInDb()

        .andHttpResponse()
        .assertThat()
        .statusCode(503)
        .body("message", is("Service unavailable"));
  }

  AnprAssertion given() {
    return new AnprAssertion(vehicleEntrantsService, nationalTaxiRegisterService,
        generalWhitelistRepository, retrofitRepository, vehicleRepository, mockServer, repository,
        null, null, objectMapper);
  }
}
