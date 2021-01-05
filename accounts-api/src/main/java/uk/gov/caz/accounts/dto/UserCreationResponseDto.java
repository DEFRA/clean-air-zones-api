package uk.gov.caz.accounts.dto;

import lombok.Builder;
import lombok.Value;
import uk.gov.caz.accounts.model.UserEntity;

/**
 * Class that represents the JSON structure for response for Account creation.
 */
@Value
@Builder(toBuilder = true)
public class UserCreationResponseDto {

  /**
   * Primary key from Account_User table.
   */
  String accountUserId;

  /**
   * AccountId in Account_User table (must match PK in Account table).
   */
  String accountId;

  /**
   * Name of the created account. It is not unique. (accountName form Account table).
   */
  String accountName;

  /**
   * Unique login credential. It is stored on Cognito level.
   */
  String email;

  /**
   * Returns true for the fleet admin account.
   */
  boolean owner;

  /**
   * Method to map entity to response dto.
   *
   * @param user {@link UserEntity}
   * @param accountName account name from request
   * @return {@link UserCreationResponseDto}
   */
  public static UserCreationResponseDto toDto(UserEntity user, String accountName) {
    return UserCreationResponseDto.builder()
        .accountId(user.getAccountId().toString())
        .accountUserId(user.getId().toString())
        .accountName(accountName)
        .email(user.getEmail())
        .owner(user.isOwner())
        .build();
  }
}