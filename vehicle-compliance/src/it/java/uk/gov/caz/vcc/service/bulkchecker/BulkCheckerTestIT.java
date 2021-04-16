package uk.gov.caz.vcc.service.bulkchecker;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.caz.definitions.dto.CleanAirZoneDto;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.vcc.annotation.IntegrationTest;
import uk.gov.caz.vcc.controller.CacheInvalidationsController;
import uk.gov.caz.vcc.repository.LocalVehicleDetailsRepository;
import uk.gov.caz.vcc.service.ChargeCalculationService;
import uk.gov.caz.vcc.service.ChargeCalculationService.CsvOutput;
import uk.gov.caz.vcc.util.BulkCheckerTestUtility;
import uk.gov.caz.vcc.util.MockServerTestIT;
import uk.gov.caz.vcc.util.TestFixturesLoader;

@IntegrationTest
@TestInstance(Lifecycle.PER_CLASS)
public class BulkCheckerTestIT extends MockServerTestIT {

  @Autowired
  CacheInvalidationsController cacheInvalidationsController;

  @Autowired
  ChargeCalculationService chargeCalculationService;

  @Autowired
  LocalVehicleDetailsRepository localVehicleDetailsRepository;

  @Autowired
  TestFixturesLoader testFixturesLoader;

  @Autowired
  BulkCheckerTestUtility testUtility;

  protected final Map<String, String> TEST_DATA = new HashMap<String, String>();
  protected List<String> TEST_VRNS;

  @BeforeAll
  public void beforeAll() throws IOException {
    testFixturesLoader.loadTestVehiclesIntoDb();
    mockTariffService();
  }

  @AfterAll
  public void teardown() {
    cacheInvalidationsController.cacheEvictVehicles();
    localVehicleDetailsRepository.deleteAll();
  }

  // protected helper methods

  protected List<ComplianceResultsDto> callBulkComplianceCheck(List<String> vrns)
      throws InterruptedException {
    return chargeCalculationService
        .bulkComplianceCheck(vrns, Arrays.asList(UUID.fromString(BIRMINGHAM_CAZ)));
  }

  protected List<CleanAirZoneDto> buildCleanAirZoneList() {
    List<CleanAirZoneDto> cleanAirZones = new ArrayList<CleanAirZoneDto>();
    cleanAirZones.add(testUtility.createCleanAirZoneDto(UUID.fromString(BATH_CAZ), "Bath"));
    cleanAirZones
        .add(testUtility.createCleanAirZoneDto(UUID.fromString(BIRMINGHAM_CAZ), "Birmingham"));
    return cleanAirZones;
  }

  protected List<Float> getRawChargesForCsvOutput(CsvOutput result) {
    return result.getCharges().values().stream()
        .filter(c -> c != "")
        .map(c -> {
          try {
            return Float.parseFloat(c);
          } catch (Exception ex) {
            return null;
          }
        })
        .collect(Collectors.toList());
  }

  protected void mockTaxiResponse(String vrn, boolean success, boolean wav) {
    if (success) {
      if (wav) {    
        whenVehicleIsInTaxiDb(vrn,"ntr-bath-response.json");
      } else {
        whenVehicleIsInTaxiDb(vrn,"ntr-bath-without-wac-response.json");        
      }
    } else {
      whenVehicleIsNotInTaxiDb(vrn);      
    }
  }

  protected List<CsvOutput> getCsvOfBulkCheck() throws InterruptedException {
    List<CleanAirZoneDto> cleanAirZones = buildCleanAirZoneList();
    List<CsvOutput> results = chargeCalculationService
        .getComplianceCheckAsCsv(TEST_VRNS, cleanAirZones);
    return results.subList(1, results.size());
  }

  protected void assertSorted(List<CsvOutput> results) {
    List<CsvOutput> sortedResults = results.stream()
        .sorted(Comparator.comparing(CsvOutput::getVrn)).collect(Collectors.toList());
    assertEquals(sortedResults, results);
  }

  protected void assertNoteIsExempt(CsvOutput result) {
    assertTrue(result.getCharges().containsValue("Exempt from charges."));
  }

  // private helper methods

  private void mockTariffService() {
    whenCazInfoIsInTariffService("/v1/clean-air-zones", "caz-second-response.json");
    whenEachCazHasTariffInfo(BATH_CAZ, "classC-bath-tariff-rates.json");
    whenEachCazHasTariffInfo(BIRMINGHAM_CAZ, "classD-birmingham-tariff-rates.json");
  }
}
