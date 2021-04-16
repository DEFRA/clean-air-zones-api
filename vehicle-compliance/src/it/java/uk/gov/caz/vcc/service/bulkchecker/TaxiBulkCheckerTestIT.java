package uk.gov.caz.vcc.service.bulkchecker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.caz.definitions.dto.ComplianceOutcomeDto;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;
import uk.gov.caz.vcc.service.ChargeCalculationService.CsvOutput;

public class TaxiBulkCheckerTestIT extends BulkCheckerTestIT {

  private Map<String, TaxiTestCase> TEST_DATA = new HashMap<String, TaxiTestCase>();

  class TaxiTestCase {
    String vehicleType;
    Float bathExpectedCharge; // for Bath CAZ
    Float bhamExpectedCharge; // for birmingham CAZ
  }

  @BeforeAll
  public void setUp() {
    insertTestData();
    TEST_VRNS = new ArrayList<String>(TEST_DATA.keySet());
  }

  private void insertTestData() {
    TEST_DATA.put("CAS300", createTestCaseAndPersist("CAS300", "Taxi", 2.0f, 2.0f));
    TEST_DATA.put("IS05YBG", createTestCaseAndPersist("IS05YBG", "Taxi", 0.0f, 2.0f));
    whenVehiclesAreInTaxiDbBulkForBulkNTRTest();
  }
    

  @Test
  public void taxiBulkCheck() throws InterruptedException {
    // act
    List<ComplianceResultsDto> results = callBulkComplianceCheck(TEST_VRNS);

    // assert
    assertEquals(TEST_VRNS.size(), results.size());
    for (ComplianceResultsDto result : results) {
      assertTrue(TEST_DATA.containsKey(result.getRegistrationNumber()));
      TaxiTestCase testCase = TEST_DATA.get(result.getRegistrationNumber());
      assertEquals(testCase.vehicleType, result.getVehicleType());
      for (ComplianceOutcomeDto outcome : result.getComplianceOutcomes()) {
    	if (outcome.getName().equals("Bath")) {
          // check against Bath expected charge
    	  assertEquals(testCase.bathExpectedCharge, outcome.getCharge());
    	} else {
          // otherwise for Birmingham 
          assertEquals(testCase.bhamExpectedCharge, outcome.getCharge());
    	}
      }
    }
  }
  
  @Test
  public void csvTaxiBulkCheck() throws InterruptedException {
    // act
    List<CsvOutput> results = getCsvOfBulkCheck();
    
    // assert
    for (CsvOutput result : results) {
      assertTrue(TEST_DATA.containsKey(result.getVrn()));
      TaxiTestCase testCase = TEST_DATA.get(result.getVrn());
      assertEquals(testCase.vehicleType, result.getVehicleType());
      List<Float> charges = getRawChargesForCsvOutput(result);
      for (int i = 0; i < charges.size(); i++) {
        // this is where a note has been converted to null (as it's not a float)
        if (charges.get(i) == null) {
          continue;
        }
        
        if (i == 0) {
          // check against bath expected charge
          assertEquals(testCase.bathExpectedCharge, charges.get(i));
        } else {
          // otherwise for Birmingham
          assertEquals(testCase.bhamExpectedCharge, charges.get(i));
        }
      }
    }
    assertSorted(results);    
  }

  private TaxiTestCase createTestCaseAndPersist(String vrn, String vehicleType,
		  Float bathExpectedCharge, Float bhamExpectedCharge) {
    TaxiTestCase testCase = new TaxiTestCase();
    testCase.vehicleType = vehicleType;;
    testCase.bathExpectedCharge = bathExpectedCharge;
    testCase.bhamExpectedCharge = bhamExpectedCharge;
    return testCase;
  }

}
