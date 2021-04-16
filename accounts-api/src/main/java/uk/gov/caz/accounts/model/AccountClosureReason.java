package uk.gov.caz.accounts.model;

/**
 * Possible closure reasons of account.
 */
public enum AccountClosureReason {
  ACCOUNT_INACTIVITY,
  VEHICLES_UPDATED_TO_NON_CHARGEABLE,
  VEHICLES_NOT_CHARGED,
  NOT_A_TRAVEL_DESTINATION,
  NON_OPERATIONAL_BUSINESS,
  OTHER
}
