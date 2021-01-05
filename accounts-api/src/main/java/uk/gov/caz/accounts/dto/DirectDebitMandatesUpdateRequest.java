package uk.gov.caz.accounts.dto;

import static java.util.Objects.isNull;
import static org.apache.logging.log4j.util.Strings.isNotBlank;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.accounts.controller.exception.InvalidRequestPayloadException;
import uk.gov.caz.accounts.util.MapPreservingOrderBuilder;

/**
 * Value object representing the incoming request which updates Direct Debit Mandates statuses.
 */
@Value
@Builder
public class DirectDebitMandatesUpdateRequest {

  /**
   * List of mandateIds to update along with new statuses.
   */
  List<SingleDirectDebitMandateUpdate> directDebitMandates;

  /**
   * Public method that validates the incoming request.
   */
  public void validate() {
    validators.forEach((validator, message) -> {
      boolean isValid = validator.apply(this);
      if (!isValid) {
        throw new InvalidRequestPayloadException(message);
      }
    });

    directDebitMandates.forEach(SingleDirectDebitMandateUpdate::validate);
  }

  /**
   * Private method with validation rules.
   */
  private static final Map<Function<DirectDebitMandatesUpdateRequest, Boolean>, String> validators =
      MapPreservingOrderBuilder
          .<Function<DirectDebitMandatesUpdateRequest, Boolean>, String>builder()
          .put(debitMandatesIsNotNull(), "directDebitMandates cannot be null.")
          .build();

  /**
   * Returns a lambda that verifies if 'status' is not null.
   */
  private static Function<DirectDebitMandatesUpdateRequest, Boolean> debitMandatesIsNotNull() {
    return request -> !isNull(request.directDebitMandates);
  }

  /**
   * Object representing single DirectDebitMandate to update along with the new status.
   */
  @Value
  @Builder
  public static class SingleDirectDebitMandateUpdate {

    /**
     * ID of the mandate to update.
     */
    String mandateId;

    /**
     * New status which will be given to the mandate.
     */
    String status;

    /**
     * Public method that validates a given object and throws exception if validation doesn't pass.
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
    private static final Map<Function<SingleDirectDebitMandateUpdate, Boolean>, String> validators =
        MapPreservingOrderBuilder
            .<Function<SingleDirectDebitMandateUpdate, Boolean>, String>builder()
            .put(mandateIdIsNotBlank(), "mandateId cannot be empty.")
            .put(statusIsNotBlank(), "status cannot be empty.")
            .build();

    /**
     * Returns a lambda that verifies if 'mandateId' is not empty.
     */
    private static Function<SingleDirectDebitMandateUpdate, Boolean> mandateIdIsNotBlank() {
      return request -> isNotBlank(request.mandateId);
    }

    /**
     * Returns a lambda that verifies if 'status' is not empty.
     */
    private static Function<SingleDirectDebitMandateUpdate, Boolean> statusIsNotBlank() {
      return request -> isNotBlank(request.status);
    }
  }
}
