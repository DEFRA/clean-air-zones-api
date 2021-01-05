package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.caz.accounts.service.UserRemovalStatus.ALREADY_DELETED;
import static uk.gov.caz.accounts.service.UserRemovalStatus.SUCCESSFULLY_DELETED;
import static uk.gov.caz.accounts.service.UserRemovalStatus.USER_DOESNT_EXIST;
import static uk.gov.caz.accounts.service.UserRemovalStatus.USER_IS_AN_OWNER;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountUserCodeRepository;
import uk.gov.caz.accounts.repository.IdentityProvider;
import uk.gov.caz.accounts.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserRemovalServiceTest {

  private static final String ANY_EMAIL = "dev1@jaqu.gov";
  private static final UUID ANY_ACCOUNT_ID = UUID.randomUUID();
  private static final UUID ANY_ID = UUID.randomUUID();

  @Mock
  private IdentityProvider identityProvider;

  @Mock
  private UserRepository userRepository;

  @Mock
  private AccountUserCodeRepository accountUserCodeRepository;

  @InjectMocks
  private UserRemovalService userRemovalService;

  @Test
  void shouldReturnProperStatusIfUserDoesntExistInDb() {
    //given
    when(userRepository.findById(ANY_ID)).thenReturn(Optional.empty());

    //when
    UserRemovalStatus userRemovalStatus = userRemovalService
        .removeStandardUser(ANY_ACCOUNT_ID, ANY_ID);

    assertThat(userRemovalStatus).isEqualTo(USER_DOESNT_EXIST);
    verify(identityProvider, never()).deleteUser(ANY_EMAIL);
  }

  @Test
  void shouldReturnProperStatusIfUserIsAnOwnerAndTheirDeletionIsNotAllowed() {
    //given
    UserEntity user = prepareAdminUserMock();
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    //when
    UserRemovalStatus userRemovalStatus = userRemovalService
        .removeStandardUser(ANY_ACCOUNT_ID, user.getId());

    assertThat(userRemovalStatus).isEqualTo(USER_IS_AN_OWNER);
    verify(identityProvider, never()).deleteUser(ANY_EMAIL);
    verify(user, never()).setIdentityProviderUserId(null);
  }

  @Test
  void shouldReturnProperStatusIfUserIsAnOwnerAndTheirDeletionIsAllowed() {
    //given
    UserEntity user = prepareAdminUserMock();
    when(accountUserCodeRepository.findByAccountUserIdAndStatusAndCodeTypeIn(any(), any(), any()))
        .thenReturn(Collections.emptyList());
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    when(identityProvider.getEmailByIdentityProviderId(any())).thenReturn(ANY_EMAIL);
    doNothing().when(identityProvider).deleteUser(anyString());

    //when
    UserRemovalStatus userRemovalStatus = userRemovalService
        .removeAnyUser(ANY_ACCOUNT_ID, user.getId());

    assertThat(userRemovalStatus).isEqualTo(SUCCESSFULLY_DELETED);
    verify(identityProvider).deleteUser(ANY_EMAIL);
    verify(user).setIdentityProviderUserId(null);
  }

  @Test
  void shouldReturnProperStatusIfUserIsAlreadyDeletedInDb() {
    //given
    UserEntity user = prepareAlreadyRemovedUserMock();
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    //when
    UserRemovalStatus userRemovalStatus = userRemovalService
        .removeStandardUser(ANY_ACCOUNT_ID, user.getId());

    assertThat(userRemovalStatus).isEqualTo(ALREADY_DELETED);
    verify(identityProvider, never()).deleteUser(ANY_EMAIL);
    verify(user, never()).setIdentityProviderUserId(null);
  }

  @Test
  void shouldReturnProperStatusIfUserDoesntMatchAccountId() {
    //given
    UserEntity user = prepareUserMock();
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    //when
    UserRemovalStatus userRemovalStatus = userRemovalService
        .removeStandardUser(UUID.randomUUID(), user.getId());

    //then
    assertThat(userRemovalStatus).isEqualTo(USER_DOESNT_EXIST);
    verify(identityProvider, never()).deleteUser(ANY_EMAIL);
    verify(user, never()).setIdentityProviderUserId(null);
  }

  private UserEntity prepareUserMock() {
    UserEntity mock = Mockito.mock(UserEntity.class);
    when(mock.getAccountId()).thenReturn(ANY_ACCOUNT_ID);
    when(mock.getId()).thenReturn(ANY_ID);
    return mock;
  }

  private UserEntity prepareAdminUserMock() {
    UserEntity userEntity = prepareUserMock();
    when(userEntity.isOwner()).thenReturn(true);
    return userEntity;
  }

  private UserEntity prepareAlreadyRemovedUserMock() {
    UserEntity userEntity = prepareUserMock();
    when(userEntity.isRemoved()).thenReturn(true);
    return userEntity;
  }
}