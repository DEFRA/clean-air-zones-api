package uk.gov.caz.taxiregister.controller;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.taxiregister.dto.reporting.LicensingAuthoritiesAuditInfo;
import uk.gov.caz.taxiregister.service.ReportingService;
import uk.gov.caz.taxiregister.tasks.ActiveLicencesInReportingWindowStarter.ActiveLicencesInReportingWindowOutput;
import uk.gov.caz.util.function.MdcAwareSupplier;

/**
 * A controller which exposes a reporting endpoint.
 */
@RestController
@AllArgsConstructor
@Slf4j
public class ReportingController implements ReportingControllerApiSpec {

  static final String LICENSING_AUTHORITIES_AUDIT_PATH = "/v1/vehicles/{vrm}/"
      + "licence-info-audit";
  static final String ACTIVE_LICENCES_IN_REPORTING_WINDOW_PATH = "/v1/licences/"
      + "active-in-reporting-window";

  private final ReportingService reportingService;
  private ActiveLicencesInReportingWindowOutput output;

  @Override
  public LicensingAuthoritiesAuditInfo getLicensingAuthoritiesOfActiveLicencesForVrmOn(String vrm,
      LocalDate date) {
    Set<String> licensingAuthorities = reportingService
        .getLicensingAuthoritiesOfActiveLicencesForVrmOn(vrm, todayIfNull(date));
    return LicensingAuthoritiesAuditInfo.with(licensingAuthorities);
  }

  @Override
  public ResponseEntity<String> startActiveLicencesInReportingWindow(LocalDate startDate,
      LocalDate endDate, String csvFileName) {
    log.info("Executing task with report query: ActiveLicencesInReportingWindow");
    CompletableFuture.supplyAsync(MdcAwareSupplier.from(() -> {
      try {
        output.writeToCsv(reportingService.runReporting(startDate, endDate), csvFileName);
      } catch (Exception ex) {
        log.info("Got Error during report generation {}", ex.toString());
      } finally {
        log.info("Ending execution of task with report query: ActiveLicencesInReportingWindow");
      }
      return null;
    }));
    return ResponseEntity.ok("Started");
  }

  /**
   * Returns today if {@code date} is null, {@code date} otherwise.
   */
  private LocalDate todayIfNull(LocalDate date) {
    return date == null ? LocalDate.now() : date;
  }
}
