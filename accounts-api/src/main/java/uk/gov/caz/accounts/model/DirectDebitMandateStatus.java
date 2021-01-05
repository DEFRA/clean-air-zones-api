package uk.gov.caz.accounts.model;

/**
 * Possible statuses of DirectDebitMandates. These statuses come from
 * https://developer.gocardless.com/api-reference/#core-endpoints-mandates.
 */
public enum DirectDebitMandateStatus {
  PENDING_CUSTOMER_APPROVAL,
  PENDING_SUBMISSION,
  SUBMITTED,
  ACTIVE,
  FAILED,
  CANCELLED,
  EXPIRED
}
