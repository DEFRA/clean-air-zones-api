package uk.gov.caz.accounts.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;


class UserTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID ACCOUNT_ID = UUID.randomUUID();
  private static final UUID IDENTITY_PROVIDER_USER_ID = UUID.randomUUID();
  private static final String EMAIL = "mail@gmail.com";
  private static final boolean EMAIL_VERIFIED = true;

  @Test
  public void combinesDbUserWithIdentityProviderUser() {
    // given
    User dbUser = User.builder().id(USER_ID).accountId(ACCOUNT_ID).build();
    User ipUser = User.builder().email(EMAIL).emailVerified(EMAIL_VERIFIED).build();

    // when
    User completeUser = User.combinedDbAndIdentityProvider(dbUser, ipUser);

    // then
    assertThat(completeUser).isNotNull();
    assertThat(completeUser.getId()).isEqualTo(USER_ID);
    assertThat(completeUser.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(completeUser.getEmail()).isEqualTo(EMAIL);
    assertThat(completeUser.isEmailVerified()).isTrue();
  }

  @Nested
  class IsRemoved {

    @Test
    public void shouldReturnFalseWhenUserHasAssignedIdentityProviderUserId() {
      // given
      User user = User.builder().identityProviderUserId(IDENTITY_PROVIDER_USER_ID).build();

      // when
      boolean isRemoved = user.isRemoved();

      // then
      assertThat(isRemoved).isFalse();
    }

    @Test
    public void shouldReturnTrueWhenUserHasNoIdentityProviderUserIdAssigned() {
      // given
      User user = User.builder().identityProviderUserId(null).build();

      // when
      boolean isRemoved = user.isRemoved();

      // then
      assertThat(isRemoved).isTrue();
    }
  }
}