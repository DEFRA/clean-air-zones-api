package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.model.AccountPermission;
import uk.gov.caz.accounts.model.AccountUserPermission;
import uk.gov.caz.accounts.model.Permission;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountPermissionRepository;
import uk.gov.caz.accounts.repository.AccountUserPermissionRepository;
import uk.gov.caz.accounts.repository.UserRepository;
import uk.gov.caz.accounts.service.exception.AccountUserNotFoundException;

@ExtendWith(MockitoExtension.class)
class   UserPermissionsServiceTest {

  private static final UUID ANY_ACCOUNT_ID = UUID.randomUUID();
  private static final UUID ANY_ACCOUNT_USER_ID = UUID.randomUUID();

  @Mock
  private UserRepository userRepository;

  @Mock
  private AccountUserPermissionRepository accountUserPermissionRepository;

  @Mock
  private AccountPermissionRepository accountPermissionRepository;

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

  @Test
  public void shouldAddOnePermission() {
    // given
    mockRepositoryUserWithPermissions(Sets.newHashSet(Permission.MAKE_PAYMENTS));

    // when
    userPermissionsService.updatePermissions(ANY_ACCOUNT_ID, ANY_ACCOUNT_USER_ID,
        Sets.newHashSet(Permission.VIEW_PAYMENTS));

    // then
    ArgumentCaptor<Set> argumentCaptor = ArgumentCaptor.forClass(Set.class);
    verify(accountUserPermissionRepository).saveAll(argumentCaptor.capture());
    Set<AccountUserPermission> addedPermisions = argumentCaptor.getValue();
    assertThat(addedPermisions).hasSize(1);
  }

  @Test
  public void shouldAddAPermissionIfCurrentlyUserHasNullOfThem() {
    // given
    mockRepositoryUserWithPermissions(null);

    // when
    userPermissionsService.updatePermissions(ANY_ACCOUNT_ID, ANY_ACCOUNT_USER_ID,
        Sets.newHashSet(Permission.VIEW_PAYMENTS));

    // then
    ArgumentCaptor<Set> argumentCaptor = ArgumentCaptor.forClass(Set.class);
    verify(accountUserPermissionRepository).saveAll(argumentCaptor.capture());
    Set<AccountUserPermission> addedPermisions = argumentCaptor.getValue();
    assertThat(addedPermisions).hasSize(1);
  }

  @Test
  public void shouldNotAddAnyPermission() {
    // given
    mockRepositoryUserWithPermissions(Sets.newHashSet(Permission.MAKE_PAYMENTS));

    // when
    userPermissionsService.updatePermissions(ANY_ACCOUNT_ID, ANY_ACCOUNT_USER_ID,
        Sets.newHashSet(Permission.MAKE_PAYMENTS));

    // then
    ArgumentCaptor<Set> argumentCaptor = ArgumentCaptor.forClass(Set.class);
    verify(accountUserPermissionRepository).saveAll(argumentCaptor.capture());
    Set<AccountUserPermission> addedPermisions = argumentCaptor.getValue();
    assertThat(addedPermisions).isEmpty();
  }

  private AccountPermission simplePermission(Permission permission) {
    return AccountPermission.builder()
        .name(permission)
        .id(RandomUtils.nextLong())
        .description(RandomStringUtils.random(5))
        .build();
  }

  private void mockRepositoryUserWithPermissions(Set<Permission> permissions) {
    List<AccountPermission> simplifiedAccountPermissions = Lists.newArrayList(
        simplePermission(Permission.MAKE_PAYMENTS), simplePermission(Permission.VIEW_PAYMENTS)
    );
    List<AccountPermission> accountPermissions = null;
    if (permissions != null) {
      accountPermissions =
          permissions.stream().map(p -> AccountPermission.builder()
              .name(p)
              .description(RandomStringUtils.random(5))
              .id(RandomUtils.nextLong())
              .build()).collect(Collectors.toList());
    }
    given(accountPermissionRepository.findAll()).willReturn(simplifiedAccountPermissions);
    given(userRepository.findByIdAndAccountId(ANY_ACCOUNT_USER_ID, ANY_ACCOUNT_ID))
        .willReturn(Optional.of(UserEntity.builder()
            .identityProviderUserId(UUID.randomUUID())
            .accountPermissions(accountPermissions)
            .build()));
  }
}