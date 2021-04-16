package uk.gov.caz.vcc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.caz.vcc.annotation.IntegrationTest;
import uk.gov.caz.vcc.service.RetrofitService.RetrofitQueryResponse;
import uk.gov.caz.vcc.util.BulkCheckerTestUtility;

@IntegrationTest
public class RetrofitServiceTestIT {
  @Autowired
  RetrofitService retrofitService;

  @Autowired
  BulkCheckerTestUtility testUtility;

  private void setupTestData(List<String> vrns) {
    vrns.forEach(vrn -> testUtility.createRetrofitVehicle(vrn));
  }

  private void teardownTestData(List<String> vrns) {
    vrns.forEach(vrn -> testUtility.deleteRetrofitVehicle(vrn));
  }
}