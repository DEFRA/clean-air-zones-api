package uk.gov.caz.taxiregister.controller;

import java.time.LocalDate;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.taxiregister.dto.reporting.ActiveLicencesAuditInfo;
import uk.gov.caz.taxiregister.dto.reporting.LicensingAuthoritiesAuditInfo;
import uk.gov.caz.taxiregister.service.ReportingService;

/**
 * A controller which exposes a reporting endpoint.
 */
@RestController
@AllArgsConstructor
public class ReportingController implements ReportingControllerApiSpec {

  static final String LICENSING_AUTHORITIES_AUDIT_PATH = "/v1/vehicles/{vrm}/"
      + "licence-info-audit";
  static final String ACTIVE_LICENCES_AUDIT_PATH = "/v1/licensing-authorities/"
      + "{licensingAuthorityId}/vrm-audit";

  private final ReportingService reportingService;

  @Override
  public LicensingAuthoritiesAuditInfo getLicensingAuthoritiesOfActiveLicencesForVrmOn(String vrm,
      LocalDate date) {
    Set<String> licensingAuthorities = reportingService
        .getLicensingAuthoritiesOfActiveLicencesForVrmOn(vrm, todayIfNull(date));
    return LicensingAuthoritiesAuditInfo.with(licensingAuthorities);
  }

  @Override
  public ActiveLicencesAuditInfo getActiveLicencesForLicensingAuthorityOn(int licensingAuthorityId,
      LocalDate date) {
    LocalDate queryDate = todayIfNull(date);
    Set<String> vrms = reportingService.getActiveLicencesForLicensingAuthorityOn(
        licensingAuthorityId, queryDate);
    return ActiveLicencesAuditInfo.builder()
        .auditDate(queryDate)
        .licensingAuthorityId(licensingAuthorityId)
        .vrmsWithActiveLicences(vrms)
        .build();
  }

  /**
   * Returns today if {@code date} is null, {@code date} otherwise.
   */
  private LocalDate todayIfNull(LocalDate date) {
    return date == null ? LocalDate.now() : date;
  }
}
