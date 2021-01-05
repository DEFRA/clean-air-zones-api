package uk.gov.caz.accounts.dto;

import lombok.Builder;
import lombok.Value;

/**
 * Class that represents the JSON structure for the response after the successful email change
 * process.
 */
@Value
@Builder
public class ConfirmEmailChangeResponse {

  /**
   * The new user's email.
   */
  String newEmail;

  /**
   * Creates a response object.
   *
   * @param email updated email
   * @return {@link ConfirmEmailChangeResponse}
   */
  public static ConfirmEmailChangeResponse from(String email) {
    return ConfirmEmailChangeResponse.builder()
        .newEmail(email)
        .build();
  }
}
