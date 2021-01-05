package uk.gov.caz.accounts.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountUserCodeTest {

  @Nested
  class IsActive {

    @Test
    void shouldReturnTrueIfStatusIsActiveAndIsNotExpired() {
      //when
      AccountUserCode accountUserCode = accountUserCodeWithStatusAndExpirationDay(
          CodeStatus.ACTIVE,
          LocalDateTime.now().plusDays(1));

      //then
      assertThat(accountUserCode.isActive()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"DISCARDED", "DISCARDED", "EXPIRED"})
    void shouldReturnFalseIfStatusIsNotActiveAndIsNotExpired(String statusName) {
      //when
      AccountUserCode accountUserCode = accountUserCodeWithStatusAndExpirationDay(
          CodeStatus.valueOf(statusName),
          LocalDateTime.now().plusDays(1));

      //then
      assertThat(accountUserCode.isActive()).isFalse();
    }

    @Test
    void shouldReturnFalseIfStatusIsActiveButIsExpired() {
      //when
      AccountUserCode accountUserCode = accountUserCodeWithStatusAndExpirationDay(
          CodeStatus.ACTIVE,
          LocalDateTime.now().minusDays(1));

      //then
      assertThat(accountUserCode.isActive()).isFalse();
    }

    private AccountUserCode accountUserCodeWithStatusAndExpirationDay(CodeStatus status,
        LocalDateTime expiration) {
      return AccountUserCode.builder()
          .id(1)
          .accountUserId(UUID.randomUUID())
          .status(status)
          .expiration(expiration)
          .code("exampleCode")
          .codeType(CodeType.PASSWORD_RESET)
          .build();
    }
  }
}