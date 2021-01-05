package uk.gov.caz.accounts.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.accounts.model.UserEntity;

/**
 * Class that represents the JSON structure for response after email resend.
 */
@Value
@Builder
public class UserVerificationEmailResendResponse {

  /**
   * Primary key from Account_User table.
   */
  UUID accountUserId;

  /**
   * AccountId in Account_User table (must match PK in Account table).
   */
  UUID accountId;

  /**
   * Recipient of the verification email.
   */
  String email;

  /**
   * Method to create {@link UserVerificationEmailResendResponse} from {@link UserEntity}.
   *
   * @param user {@link UserEntity}
   * @return {@link UserVerificationEmailResendResponse}
   */
  public static UserVerificationEmailResendResponse from(UserEntity user) {
    return UserVerificationEmailResendResponse.builder()
        .accountId(user.getId())
        .accountUserId(user.getAccountId())
        .email(user.getEmail())
        .build();
  }
}
