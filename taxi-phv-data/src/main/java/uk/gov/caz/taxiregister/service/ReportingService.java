package uk.gov.caz.taxiregister.service;

import com.google.common.base.Preconditions;
import java.time.LocalDate;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.taxiregister.repository.ReportingRepository;

/**
 * A class responsible for handling reporting queries.
 */
@Service
@Slf4j
@AllArgsConstructor
public class ReportingService {

  private final ReportingRepository reportingRepository;

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
   * Returns licences (represented by VRMs) which were active for a given licensing authority
   * (represented by {@code licensingAuthorityId}) on a given date.
   *
   * @param licensingAuthorityId The identifier of the licensing authority.
   * @param date The date against which the check is performed.
   * @return A {@link Set} of VRMs which had at least one active licence on a given date for a given
   *     licensing authority.
   * @throws NullPointerException if {@code date} is null.
   * @throws IllegalArgumentException if {@code date} is a future date.
   */
  public Set<String> getActiveLicencesForLicensingAuthorityOn(int licensingAuthorityId,
      LocalDate date) {
    Preconditions.checkNotNull(date, "Date cannot be null");
    Preconditions.checkArgument(!date.isAfter(LocalDate.now()), "Cannot process a future date");

    return reportingRepository.getActiveLicencesForLicensingAuthorityOn(licensingAuthorityId, date);
  }
}
