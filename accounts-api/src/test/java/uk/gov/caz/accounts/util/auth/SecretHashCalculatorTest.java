package uk.gov.caz.accounts.util.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;

class SecretHashCalculatorTest {

  @Test
  public void shouldCalculateHash() {
    // given
    String userPoolClientId = "user-pool-client-id";
    String userPoolClientSecret = "user-pool-client-secret";
    String username = "a@b.com";

    // when
    String result = SecretHashCalculator
        .calculateSecretHash(userPoolClientId, userPoolClientSecret, username);

    // then
    assertThat(result).isEqualTo("hJAY3VKwKQyz2H1A6+Jgy7nUxmNepNrLTL8ltH+BzX8=");
  }

  @Test
  public void shouldThrowRuntimeExceptionWhenUserPoolClientSecretIsNull() {
    // given
    String userPoolClientId = "user-pool-client-id";
    String userPoolClientSecret = null;
    String username = "a@b.com";

    // when
    Throwable result = catchThrowable(() -> SecretHashCalculator
        .calculateSecretHash(userPoolClientId, userPoolClientSecret, username));

    // then
    assertThat(result).isInstanceOf(RuntimeException.class)
        .hasMessage("Error while calculating a SecretHash value");
  }
}