package uk.gov.caz.whitelist.controller;

import com.google.common.base.Preconditions;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.whitelist.dto.WhitelistInfoHistoricalRequest;
import uk.gov.caz.whitelist.dto.WhitelistInfoHistoricalResponse;
import uk.gov.caz.whitelist.dto.WhitelistInfoHistoricalResponse.Change;
import uk.gov.caz.whitelist.model.WhitelistVehicleHistorical;
import uk.gov.caz.whitelist.service.WhitelistVehicleHistoryService;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HistoricalInfoController implements HistoricalInfoControllerApiSpec {

  public static final String BASE_PATH =
      "/v1/whitelisting/vehicles/{vrn}/whitelist-info-historical";

  private final WhitelistVehicleHistoryService service;

  @Override
  public ResponseEntity<WhitelistInfoHistoricalResponse> search(String vrn,
      WhitelistInfoHistoricalRequest request) {
    request.validate();

    WhitelistVehicleHistorical vehicleHistorical = service.findByVrnInRange(vrn, request);

    WhitelistInfoHistoricalResponse response = WhitelistInfoHistoricalResponse.builder()
        .page(request.getPageNumber())
        .pageCount(
            calculatePageCount(vehicleHistorical.getTotalChangesCount(), request.getPageSize()))
        .perPage(request.getPageSize())
        .totalChangesCount(vehicleHistorical.getTotalChangesCount())
        .changes(vehicleHistorical.getChanges().stream()
            .map(Change::from)
            .collect(Collectors.toList()))
        .build();
    return ResponseEntity.ok(response);
  }

  /**
   * Helper method to calculate pages count.
   */
  protected int calculatePageCount(int totalCount, int perPage) {
    Preconditions.checkArgument(perPage >= 1);
    Preconditions.checkArgument(totalCount >= 0);
    return (totalCount + perPage - 1) / perPage;
  }
}
