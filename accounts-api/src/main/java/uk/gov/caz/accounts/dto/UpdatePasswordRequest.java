package uk.gov.caz.accounts.dto;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Class bearing information on the password update.
 */
@Value
@Builder
public class UpdatePasswordRequest {

  /**
   * Account user id of user whose password is being updated.
   */
  private UUID accountUserId;

  /**
   * Old password for the user.
   */
  private String oldPassword;

  /**
   * New password being set for the user.
   */
  private String newPassword;

  /**
   * Validates this object.
   */
  public void validate() {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(oldPassword),
        "Old password can't be empty");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(newPassword),
        "New password can't be empty");
  }
}
