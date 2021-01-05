package uk.gov.caz.accounts.dto;

import lombok.Builder;
import lombok.Value;
import org.springframework.http.HttpStatus;

/**
 * Class represents an error which may occur during update of direct debit mandates collection.
 */
@Value
@Builder
public class DirectDebitMandateUpdateError {

  int status = HttpStatus.BAD_REQUEST.value();
  String message;
  String mandateId;

  private static final String INVALID_MANDATE_STATUS_MESSAGE =
      "Invalid direct debit mandate status";
  private static final String DIRECT_DEBIT_DOES_NOT_EXIST_MESSAGE =
      "Direct debit mandate does not exist";
  private static final String DIRECT_DEBIT_MANDATE_AND_ACCOUNT_MISMATCH_MESSAGE =
      "Direct debit mandate does not belong to this account.";

  /**
   * Returns the {@link DirectDebitMandateUpdateError} with information about invalid mandate
   * status.
   */
  public static DirectDebitMandateUpdateError invalidMandateStatus(String mandateId) {
    return errorWithMandateIdAndMessage(mandateId, INVALID_MANDATE_STATUS_MESSAGE);
  }

  /**
   * Returns the {@link DirectDebitMandateUpdateError} with information about not existing mandate
   * with provided mandateId.
   */
  public static DirectDebitMandateUpdateError missingDirectDebitMandate(String mandateId) {
    return errorWithMandateIdAndMessage(mandateId, DIRECT_DEBIT_DOES_NOT_EXIST_MESSAGE);
  }

  /**
   * Returns the {@link DirectDebitMandateUpdateError} with information about not existing
   * association between the account and the mandate.
   */
  public static DirectDebitMandateUpdateError missingDirectDebitMandateForAccount(
      String mandateId) {
    return errorWithMandateIdAndMessage(mandateId,
        DIRECT_DEBIT_MANDATE_AND_ACCOUNT_MISMATCH_MESSAGE);
  }

  /**
   * Builds the {@link DirectDebitMandateUpdateError} with provided mandateId and message.
   */
  private static DirectDebitMandateUpdateError errorWithMandateIdAndMessage(String mandateId,
      String message) {
    return DirectDebitMandateUpdateError.builder()
        .mandateId(mandateId)
        .message(message)
        .build();
  }
}
