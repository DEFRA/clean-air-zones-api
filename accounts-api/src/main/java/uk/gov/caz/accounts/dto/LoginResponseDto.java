package uk.gov.caz.accounts.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Class that represents the JSON structure for response for login.
 */
@Value
@Builder
public class LoginResponseDto {

  /**
   * Primary key from Account_User table.
   */
  UUID accountUserId;

  /**
   * AccountId in Account_User table (must match PK in Account table).
   */
  UUID accountId;

  /**
   * Name of the created account. It is not unique. (accountName form Account table).
   */
  String accountName;

  /**
   * Unique login credential. It is stored on Cognito level.
   */
  String email;

  /**
   * Returns true for the fleet owner account.
   */
  boolean owner;

  /**
   * Returns true for the beta tester.
   */
  boolean betaTester;

  /**
   * Timestamp of when password was last updated or if not yet known timestamp of user creation.
   */
  @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  LocalDateTime passwordUpdateTimestamp;

  /**
   * Permissions for given user.
   */
  List<String> permissions;
}