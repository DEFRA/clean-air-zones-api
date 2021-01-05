package uk.gov.caz.accounts.dto;

import com.google.common.base.Preconditions;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Class that keeps input object parameters for Lambda which calculate charges and populates cache.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargeCalculationLambdaInput {

  /**
   * Account of id being calculated.
   */
  private UUID accountId;

  /**
   * Job of id to be marked after calculation process.
   */
  private Integer jobId;

  /**
   * Correlation ID connected with this request.
   */
  private UUID correlationId;

  /**
   * A number that says how many times this lambda function has been recursively called. This is to
   * prevent us from a non-terminating recursive calls. Nullable. If absent, then equal to one.
   */
  private Integer invocationNumber;

  /**
   * Flag indicating whether to send email(s) upon successful job completion. {@code true} is the
   * default value unless provided. Using a boxed value for backward compatibility.
   */
  private Boolean shouldSendEmailsUponSuccessfulJobCompletion;

  /**
   * Returns current invocation number, 1 if null.
   */
  public int getInvocationNumber() {
    return Objects.isNull(invocationNumber) ? 1 : invocationNumber;
  }

  /**
   * Getter for {@code shouldSendEmailsUponSuccessfulJobCompletion} that returns either the provided
   * value (if not null) or the default one.
   */
  public boolean shouldSendEmailsUponSuccessfulJobCompletion() {
    return Objects.isNull(shouldSendEmailsUponSuccessfulJobCompletion)
        || shouldSendEmailsUponSuccessfulJobCompletion;
  }

  /**
   * Validates this dto.
   */
  public void validate() {
    Preconditions.checkNotNull(accountId, "accountId has to be set");
    Preconditions.checkNotNull(jobId, "jobId has to be set");
    Preconditions.checkNotNull(correlationId, "correlationId has to be set");
  }
}
