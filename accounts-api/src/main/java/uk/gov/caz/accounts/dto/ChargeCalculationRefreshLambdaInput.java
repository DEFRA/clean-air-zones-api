package uk.gov.caz.accounts.dto;

import com.google.common.base.Preconditions;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Class that keeps input object parameters for Lambda which refresh calculated charges cache.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargeCalculationRefreshLambdaInput {

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
   * Returns current invocation number, 1 if null.
   */
  public int getInvocationNumber() {
    return Objects.isNull(invocationNumber) ? 1 : invocationNumber;
  }

  /**
   * Validates this dto.
   */
  public void validate() {
    Preconditions.checkNotNull(correlationId, "correlationId has to be set");
  }
}
