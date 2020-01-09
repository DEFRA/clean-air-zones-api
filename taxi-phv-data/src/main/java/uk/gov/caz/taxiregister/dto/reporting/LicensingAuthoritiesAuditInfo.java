package uk.gov.caz.taxiregister.dto.reporting;

import java.util.Collection;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

/**
 * A value class that serves as a response for the names of licensing authorities of active
 * licenses reporting API call.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LicensingAuthoritiesAuditInfo {

  /**
   * A collection of licensing authorities (names).
   */
  @NonNull
  Collection<String> licensingAuthoritiesNames;

  /**
   * Static factory method which creates an instance of {@link LicensingAuthoritiesAuditInfo} with
   * passed {@code licensingAuthoritiesNames}.
   */
  public static LicensingAuthoritiesAuditInfo with(Collection<String> licensingAuthoritiesNames) {
    return new LicensingAuthoritiesAuditInfo(licensingAuthoritiesNames);
  }
}
