package uk.gov.caz.accounts.util.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.caz.accounts.model.Permission.MAKE_PAYMENTS;

import com.google.common.collect.ImmutableList;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.caz.accounts.dto.LoginResponseDto;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.LoginData;
import uk.gov.caz.accounts.model.UserEntity;

public class LoginDataToLoginResponseConverterTest {

  @Test
  void shouldReturnUserObjectWhenEmailIsFoundInAttributesList() {
    // given
    LoginData loginData = buildLoginData();

    // when
    LoginResponseDto response = LoginDataToLoginResponseConverter.from(loginData);

    // then
    assertThat(response.getAccountId()).isEqualTo(loginData.getAccount().getId());
    assertThat(response.getAccountName()).isEqualTo(loginData.getAccount().getName());
    assertThat(response.getEmail()).isEqualTo(loginData.getUser().getEmail());
    assertThat(response.getAccountUserId()).isEqualTo(loginData.getUser().getId());
    assertThat(response.isOwner()).isEqualTo(loginData.getUser().isOwner());
    assertThat(response.isBetaTester()).isEqualTo(loginData.isBetaTester());
    assertThat(response.getPermissions()).containsExactly("MAKE_PAYMENTS");
  }

  private LoginData buildLoginData() {
    Account account = Account.builder()
        .id(UUID.randomUUID())
        .name("Account Name")
        .build();
    UserEntity user = UserEntity.builder()
        .isOwner(true)
        .email("jan@kowalski.com")
        .id(UUID.randomUUID())
        .accountId(account.getId())
        .build();

    return LoginData.of(user, account, LocalDateTime.now(), ImmutableList.of(MAKE_PAYMENTS), false);
  }
}
