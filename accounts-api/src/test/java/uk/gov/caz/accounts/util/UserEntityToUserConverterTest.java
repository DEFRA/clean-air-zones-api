package uk.gov.caz.accounts.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.caz.accounts.model.AccountPermission;
import uk.gov.caz.accounts.model.Permission;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.model.UserEntity;

class UserEntityToUserConverterTest {

  @Test
  void shouldReturnConvertedUserForUserEntity() {
    // given
    Permission permission = Permission.MAKE_PAYMENTS;
    UserEntity userEntity = buildUserEntity(permission);

    // when
    User result = UserEntityToUserConverter.convert(userEntity);

    // then
    assertThat(result.getId()).isEqualTo(userEntity.getId());
    assertThat(result.getIdentityProviderUserId()).isEqualTo(userEntity.getIdentityProviderUserId());
    assertThat(result.getAccountPermissions()).hasSize(1);
    String accountPermission = result.getAccountPermissions().iterator().next();
    assertThat(accountPermission).isEqualTo(permission.name());
  }

  private UserEntity buildUserEntity(Permission permissionName) {
    return UserEntity.builder()
        .accountId(UUID.randomUUID())
        .identityProviderUserId(UUID.randomUUID())
        .isOwner(false)
        .accountPermissions(Arrays.asList(AccountPermission.builder()
            .name(permissionName)
            .description("Any Description")
            .build()))
        .build();
  }
}