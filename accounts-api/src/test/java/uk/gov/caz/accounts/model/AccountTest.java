package uk.gov.caz.accounts.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountTest {

  @Nested
  class IsActive {

    @Test
    public void shouldReturnFalseWhenInactivationTimestampIsSet() {
      // when
      Account account = Account.builder()
          .inactivationTimestamp(LocalDateTime.now())
          .build();

      // then
      assertThat(account.isActive()).isFalse();
    }

    @Test
    public void shouldReturnTrueWhenInactivationTimestampIsNull() {
      // when
      Account account = Account.builder().build();

      // then
      assertThat(account.isActive()).isTrue();
    }
  }

  @Nested
  class GetInactivationTimestamp {

    @Test
    public void shouldReturnNonEmptyOptionalWhenInactivationTimestampIsSet() {
      // when
      Account account = Account.builder()
          .inactivationTimestamp(LocalDateTime.now())
          .build();

      // then
      assertThat(account.getInactivationTimestamp()).isNotEmpty();
    }

    @Test
    public void shouldReturnEmptyOptionalWhenInactivationTimestampIsNull() {
      // when
      Account account = Account.builder().build();

      // then
      assertThat(account.getInactivationTimestamp()).isEmpty();
    }
  }
}