package uk.gov.caz.taxiregister.dto.reporting;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.Collection;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * A value class that serves as a response for the active licenses for a given licensing authority
 * reporting API call.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ActiveLicencesAuditInfo {

  /**
   * The database identifier of the licensing authority.
   */
  int licensingAuthorityId;

  /**
   * The date against which the reporting call was made.
   */
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @NonNull
  LocalDate auditDate;

  /**
   * A collection of licensing authorities (names).
   */
  @NonNull
  Collection<String> vrmsWithActiveLicences;
}
