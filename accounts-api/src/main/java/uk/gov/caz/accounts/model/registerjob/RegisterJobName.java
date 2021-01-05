package uk.gov.caz.accounts.model.registerjob;

import lombok.NonNull;
import lombok.Value;

/**
 * Holds the name of a job.
 */
@Value
public class RegisterJobName {

  @NonNull
  String value;
}
