package uk.gov.caz.accounts.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class UpdatePasswordRequestTest {

  private UUID accountUserId;
  private String oldPassword;
  private String newPassword;

  @BeforeEach
  public void setup() {
    accountUserId = UUID.randomUUID();
    oldPassword = RandomStringUtils.randomAlphabetic(10);
    newPassword = RandomStringUtils.randomAlphabetic(10);
  }

  @Test
  public void validatePassesForValidInput() {
    // given
    UpdatePasswordRequest updatePasswordRequest = new UpdatePasswordRequest(
        accountUserId, oldPassword, newPassword
    );

    // when
    updatePasswordRequest.validate();

    // then
    assertThat(updatePasswordRequest.getAccountUserId()).isEqualTo(accountUserId);
    assertThat(updatePasswordRequest.getOldPassword()).isEqualTo(oldPassword);
    assertThat(updatePasswordRequest.getNewPassword()).isEqualTo(newPassword);
  }

  @Nested
  class ValidationErrorIsThrownWhen {

    @Test
    public void oldPasswordIsEmpty() {
      // given
      UpdatePasswordRequest updatePasswordRequest = new UpdatePasswordRequest(
          accountUserId, "", newPassword
      );

      // when
      Throwable throwable = catchThrowable(() -> updatePasswordRequest.validate());

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void newPasswordIsEmpty() {
      // given
      UpdatePasswordRequest updatePasswordRequest = new UpdatePasswordRequest(
          accountUserId, oldPassword, ""
      );

      // when
      Throwable throwable = catchThrowable(() -> updatePasswordRequest.validate());

      // then
      assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
    }
  }
}
