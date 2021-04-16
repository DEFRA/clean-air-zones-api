package uk.gov.caz.taxiregister.service;

import com.google.common.base.Preconditions;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.taxiregister.model.ActiveLicenceInReportingWindow;
import uk.gov.caz.taxiregister.repository.ReportingRepository;

/**
 * A class responsible for handling reporting queries.
 */
@Service
@Slf4j
@AllArgsConstructor
public class ReportingService {

  private final ReportingRepository reportingRepository;
  private final ActiveLicencesCollator activeLicencesCollator;

  /**
   * Returns licensing authorities (names) of active licences for a given VRM and date.
   *
   * @param vrm Vehicle registration mark.
   * @param date The date against which the check is performed.
   * @return A set of names of licensing authorities.
   * @throws NullPointerException if {@code vrm} or {@code date} is null.
   * @throws IllegalArgumentException if {@code date} is a future date.
   */
  public Set<String> getLicensingAuthoritiesOfActiveLicencesForVrmOn(String vrm, LocalDate date) {
    Preconditions.checkNotNull(vrm, "Vrm cannot be null");
    Preconditions.checkNotNull(date, "Date cannot be null");
    Preconditions.checkArgument(!date.isAfter(LocalDate.now()), "Cannot process a future date");

    return reportingRepository.getLicensingAuthoritiesOfActiveLicencesForVrmOn(vrm, date);
  }

  /**
   * Runs reporting query for active licences in a specified reporting window.
   *
   * @param reportingWindowStartDate Reporting window start date.
   * @param reportingWindowEndDate Reporting window end date.
   * @return List of {@link ActiveLicenceInReportingWindow} objects.
   */
  public List<ActiveLicenceInReportingWindow> runReporting(LocalDate reportingWindowStartDate,
      LocalDate reportingWindowEndDate) {
    Preconditions.checkArgument(reportingWindowEndDate.isAfter(reportingWindowStartDate),
        "Reporting window end date must be after start date.");
    return activeLicencesCollator
        .collate(reportingRepository.getLicenceEvents(reportingWindowEndDate),
            reportingWindowStartDate, reportingWindowEndDate);
  }
}
