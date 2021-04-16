package uk.gov.caz.whitelist.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.whitelist.dto.WhitelistInfoHistoricalRequest;
import uk.gov.caz.whitelist.model.WhitelistVehicleHistorical;
import uk.gov.caz.whitelist.model.WhitelistVehicleHistory;
import uk.gov.caz.whitelist.repository.WhitelistVehicleHistoryPostgresRepository;

/**
 * A class that is responsible for managing vehicle's licences historical data using postgres
 * repository.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhitelistVehicleHistoryService {

  private final WhitelistVehicleHistoryPostgresRepository repository;

  /**
   * Finds all {@link WhitelistVehicleHistory} entities for a given vrm and date range.
   *
   * @param request {@link WhitelistInfoHistoricalRequest}
   * @return {@link WhitelistVehicleHistorical} .
   */
  public WhitelistVehicleHistorical findByVrnInRange(String vrn,
      WhitelistInfoHistoricalRequest request) {
    List<WhitelistVehicleHistory> changes = repository
        .findByVrnInRange(vrn, request.getLocalStartDate(), request.getLocalEndDate(),
            request.getPageSize(), request.getPageNumber());
    return WhitelistVehicleHistorical.builder()
        .changes(changes)
        .totalChangesCount(getTotalChangesCount(vrn, request, changes))
        .build();
  }

  /**
   * Helper method to provide total count.
   */
  private int getTotalChangesCount(String vrn, WhitelistInfoHistoricalRequest request,
      List<WhitelistVehicleHistory> changes) {
    return request.getPageNumber() == 0 && changes.size() < request.getPageSize() ? changes.size()
        : repository.count(vrn, request.getLocalStartDate(), request.getLocalEndDate())
            .intValue();
  }
}