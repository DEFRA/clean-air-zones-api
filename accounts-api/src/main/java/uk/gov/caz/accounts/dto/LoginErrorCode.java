package uk.gov.caz.accounts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Class represents type of error associated with invalid user login.
 */
public enum  LoginErrorCode {
  @JsonProperty("invalidCredentials")
  INVALID_CREDENTIALS,
  @JsonProperty("pendingEmailChange")
  PENDING_EMAIL_CHANGE
}
