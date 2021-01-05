package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PendingOwnersRemovalServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserRemovalService userRemovalService;

  @InjectMocks
  private PendingOwnersRemovalService pendingOwnersRemovalService;

  @Test
  public void shouldRemovePendingOwnerUsersBeforeCreatingNewOne() {
    // given
    mockExistingExpiredOwnerForAccount();
    mockSuccessfulUserRemoval();

    // when
    pendingOwnersRemovalService.removeNonVerifiedOwnerUsers(any());

    // then
    verify(userRemovalService).removeAnyUser(any(), any());
  }

  @Test
  public void shouldNotThrowExceptionOnUnsuccessfulUserRemoval() {
    // given
    mockExistingExpiredOwnerForAccount();
    mockUnsuccessfulUserRemoval();

    // when
    Throwable throwable = catchThrowable(
        () -> pendingOwnersRemovalService.removeNonVerifiedOwnerUsers(any()));

    // then
    assertThat(throwable).isNull();
  }

  private void mockUnsuccessfulUserRemoval() {
    when(userRemovalService.removeAnyUser(any(), any()))
        .thenReturn(UserRemovalStatus.ALREADY_DELETED);
  }

  private void mockSuccessfulUserRemoval() {
    when(userRemovalService.removeAnyUser(any(), any()))
        .thenReturn(UserRemovalStatus.SUCCESSFULLY_DELETED);
  }

  private void mockExistingExpiredOwnerForAccount() {
    UserEntity userEntity = UserEntity.builder()
        .accountId(UUID.randomUUID())
        .id(UUID.randomUUID())
        .isOwner(true)
        .build();

    when(userRepository.findOwnersForAccount(any())).thenReturn(Arrays.asList(userEntity));
  }

}