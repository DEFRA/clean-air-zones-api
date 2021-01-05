package uk.gov.caz.accounts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Class represents type of error associated with user creation process.
 */
public enum AccountErrorCode {
  @JsonProperty("emailNotUnique")
  EMAIL_NOT_UNIQUE,
  @JsonProperty("passwordNotValid")
  PASSWORD_NOT_VALID,
  @JsonProperty("profanity")
  PROFANITY,
  @JsonProperty("abuse")
  ABUSE,
  @JsonProperty("duplicate")
  ACCOUNT_NAME_NOT_UNIQUE,
  @JsonProperty("emailAlreadyVerified")
  EMAIL_ALREADY_VERIFIED,
  @JsonProperty("invalid")
  INVALID_USER_VERIFICATION_TOKEN,
  @JsonProperty("expired")
  EXPIRED_USER_VERIFICATION_TOKEN,
  @JsonProperty("invalid account or user")
  INVITING_USER_NOT_FOUND,
  @JsonProperty("newPasswordReuse")
  PASSWORD_RECENTLY_USED,
  @JsonProperty("oldPasswordInvalid")
  OLD_PASSWORD_INVALID
}
