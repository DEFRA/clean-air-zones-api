package uk.gov.caz.accounts.assertion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import uk.gov.caz.accounts.StubbedIdentityProvider;
import uk.gov.caz.accounts.model.UserEntity;

@RequiredArgsConstructor
public class LockoutMechanismAssertion {

  private final StubbedIdentityProvider identityProvider;

  private UserEntity user;

  public LockoutMechanismAssertion attributesForUser(String email) {
    user = identityProvider.getUserAsUserEntity(email);
    return this;
  }

  public LockoutMechanismAssertion hasFailedLogins(int failedLogins) {
    assertThat(user.getFailedLogins()).isEqualTo(failedLogins);
    return this;
  }

  public LockoutMechanismAssertion hasLockoutTime(LocalDateTime lockoutTime) {
    assertThat(user.getLockoutTime()).contains(lockoutTime);
    return this;
  }

  public LockoutMechanismAssertion hasNoLockoutTime() {
    assertThat(user.getLockoutTime()).isEmpty();
    return this;
  }

  public LockoutMechanismAssertion lockoutTimeIsNotNull() {
    assertThat(user.getLockoutTime()).isPresent();
    return this;
  }

  public void mockFailedLoginsAndLockoutTime(String email, int failedLogins,
      LocalDateTime lockoutTime) {
    identityProvider.mockFailedLoginsAndLockoutTime(email, failedLogins, lockoutTime);
  }
}