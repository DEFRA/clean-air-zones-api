package uk.gov.caz.accounts.dto;

import static java.util.Objects.isNull;

import com.google.common.base.Strings;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.accounts.controller.exception.InvalidRequestPayloadException;
import uk.gov.caz.accounts.util.MapPreservingOrderBuilder;

/**
 * Class that represents the JSON structure for request when creating DirectDebitMandate.
 */
@Value
@Builder
public class DirectDebitMandateRequest {

  /**
   * An identifier of mandate.
   */
  String mandateId;

  /**
   * An identifier of the Clean Air Zone.
   */
  UUID cleanAirZoneId;

  /**
   * An identifier of the account which creates the direct debit mandate.
   */
  UUID accountUserId;

  /**
   * Public method that validates given object and throws exceptions if validation doesn't pass.
   */
  public void validate() {
    validators.forEach((validator, message) -> {
      boolean isValid = validator.apply(this);
      if (!isValid) {
        throw new InvalidRequestPayloadException(message);
      }
    });
  }

  /**
   * Private method with validation rules.
   */
  private static final Map<Function<DirectDebitMandateRequest, Boolean>, String> validators =
      MapPreservingOrderBuilder.<Function<DirectDebitMandateRequest, Boolean>, String>builder()
          .put(debitMandateIdIsNotNullOrEmpty(), "directDebitMandateId cannot be null.")
          .put(cleanAirZoneIdIsNotNull(), "cleanAirZoneId cannot be null or empty.")
          .put(accountUserIdIsNotNull(), "accountUserId cannot be null.")
          .build();

  /**
   * Returns a lambda that verifies if 'directDebitMandate' is not null.
   */
  private static Function<DirectDebitMandateRequest, Boolean> debitMandateIdIsNotNullOrEmpty() {
    return request -> !Strings.isNullOrEmpty(request.getMandateId());
  }

  /**
   * Returns a lambda that verifies if 'cleanAirZoneId' is not null.
   */
  private static Function<DirectDebitMandateRequest, Boolean> cleanAirZoneIdIsNotNull() {
    return request -> !isNull(request.getCleanAirZoneId());
  }

  /**
   * Returns a lambda that verifies if 'accountUserId' is not null.
   */
  private static Function<DirectDebitMandateRequest, Boolean> accountUserIdIsNotNull() {
    return request -> !isNull(request.getAccountUserId());
  }
}
