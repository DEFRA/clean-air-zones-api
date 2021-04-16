package uk.gov.caz.taxiregister.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.taxiregister.dto.LicenceInfoHistoricalRequest;
import uk.gov.caz.taxiregister.model.LicenceInfoHistorical;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicenceHistory;
import uk.gov.caz.taxiregister.repository.TaxiPhvLicenceHistoryPostgresRepository;

/**
 * A class that is responsible for managing vehicle's licences historical data using postgres
 * repository.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaxiPhvLicenceHistoryService {

  private final TaxiPhvLicenceHistoryPostgresRepository repository;

  /**
   * Finds all {@link TaxiPhvVehicleLicenceHistory} entities for a given vrm and date range.
   *
   * @param request {@link LicenceInfoHistorical}
   * @return {@link LicenceInfoHistorical} .
   */
  public LicenceInfoHistorical findByVrmInRange(LicenceInfoHistoricalRequest request) {
    List<TaxiPhvVehicleLicenceHistory> changes = repository
        .findByVrmInRange(request.getVrm(), request.getModifyDateFrom(), request.getModifyDateTo(),
            request.getPageSize(), request.getPageNumber());
    return LicenceInfoHistorical.builder()
        .changes(changes)
        .totalChangesCount(getTotalChangesCount(request, changes))
        .build();
  }

  /**
   * Helper method to provide total count.
   */
  private long getTotalChangesCount(LicenceInfoHistoricalRequest request,
      List<TaxiPhvVehicleLicenceHistory> changes) {
    return request.getPageNumber() == 0 && changes.size() < request.getPageSize() ? changes.size()
        : repository
            .count(request.getVrm(), request.getModifyDateFrom(), request.getModifyDateTo());
  }
}