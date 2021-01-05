package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.model.Permission;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.UserRepository;
import uk.gov.caz.accounts.service.exception.AccountUserNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserPermissionsServiceTest {

  private static final UUID ANY_ACCOUNT_ID = UUID.randomUUID();
  private static final UUID ANY_ACCOUNT_USER_ID = UUID.randomUUID();

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserPermissionsService userPermissionsService;

  @Test
  public void shouldThrowNullPointerExceptionWhenPermissionsIsNull() {
    // given
    Set<Permission> permissions = null;

    // when
    Throwable throwable = catchThrowable(() ->
        userPermissionsService.updatePermissions(ANY_ACCOUNT_ID, ANY_ACCOUNT_USER_ID, permissions));

    // then
    assertThat(throwable).isInstanceOf(NullPointerException.class)
        .hasMessage("newPermissions cannot be null");
  }

  @Test
  public void shouldThrowAccountUserNotFoundExceptionWhenUserIsDeleted() {
    // given
    Set<Permission> permissions = Collections.emptySet();
    given(userRepository.findByIdAndAccountId(ANY_ACCOUNT_USER_ID, ANY_ACCOUNT_ID))
        .willReturn(Optional.of(UserEntity.builder()
            .identityProviderUserId(null)
            .build()));

    // when
    Throwable throwable = catchThrowable(() ->
        userPermissionsService.updatePermissions(ANY_ACCOUNT_ID, ANY_ACCOUNT_USER_ID, permissions));

    // then
    assertThat(throwable).isInstanceOf(AccountUserNotFoundException.class)
        .hasMessage("User not found");
  }
}