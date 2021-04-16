package uk.gov.caz.taxiregister.controller;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.taxiregister.controller.exception.RequestParamsValidationException;
import uk.gov.caz.taxiregister.dto.LicenceInfoHistoricalRequest;
import uk.gov.caz.taxiregister.dto.LicenceInfoHistoricalResponse;
import uk.gov.caz.taxiregister.dto.LicenceInfoHistoricalResult;
import uk.gov.caz.taxiregister.model.LicenceInfoHistorical;
import uk.gov.caz.taxiregister.service.TaxiPhvLicenceHistoryService;

@RestController
@RequiredArgsConstructor
public class LookupHistoricalController implements LookupHistoricalControllerApiSpec {

  public static final String PATH = "/v1/vehicles/{vrm}/licence-info-historical";
  protected static final List<String> REQUIRED_PROPERTIES = Arrays.asList(
      "startDate", "endDate", "pageNumber", "pageSize"
  );

  private final TaxiPhvLicenceHistoryService service;

  @Override
  public ResponseEntity<LicenceInfoHistoricalResponse> getLicenceInfoFor(String vrm,
      Map<String, String> queryStrings) {
    checkPreconditions(vrm, queryStrings);
    LicenceInfoHistoricalRequest request = LicenceInfoHistoricalRequest.from(vrm, queryStrings);
    LicenceInfoHistorical infoHistorical = service.findByVrmInRange(request);
    List<LicenceInfoHistoricalResult> histories = infoHistorical.getChanges()
        .stream()
        .map(LicenceInfoHistoricalResult::from)
        .collect(Collectors.toList());
    return ResponseEntity.ok(
        LicenceInfoHistoricalResponse.builder()
            .page(request.getPageNumber())
            .pageCount(
                calculatePageCount(infoHistorical.getTotalChangesCount(), request.getPageSize()))
            .perPage(request.getPageSize())
            .totalChangesCount(infoHistorical.getTotalChangesCount())
            .changes(histories)
            .build());
  }

  /**
   * Helper method to calculate pages count.
   */
  protected long calculatePageCount(long totalCount, long perPage) {
    Preconditions.checkArgument(perPage >= 1);
    Preconditions.checkArgument(totalCount >= 0);
    return (totalCount + perPage - 1) / perPage;
  }

  /**
   * Helper method to verify params quality.
   */
  private void checkPreconditions(String vrm, Map<String, String> queryStrings) {
    if (vrm == null) {
      throw new RequestParamsValidationException("vrm cannot be null");
    }
    List<String> nullProperties = new ArrayList<>();
    REQUIRED_PROPERTIES.forEach(requiredProperty -> {
      String propertyValue = queryStrings.get(requiredProperty);
      if (propertyValue == null) {
        nullProperties.add(requiredProperty);
      }
    });
    if (!nullProperties.isEmpty()) {
      throw new RequestParamsValidationException(
          String.format("Properties: %s cannot be null",
              String.join(",", nullProperties)));
    }

  }
}