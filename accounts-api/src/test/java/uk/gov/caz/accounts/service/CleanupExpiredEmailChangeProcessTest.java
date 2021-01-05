package uk.gov.caz.accounts.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.model.AccountUserCode;
import uk.gov.caz.accounts.model.CodeStatus;
import uk.gov.caz.accounts.model.CodeType;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountUserCodeRepository;

@ExtendWith(MockitoExtension.class)
class CleanupExpiredEmailChangeProcessTest {

  @Mock
  private AccountUserCodeRepository accountUserCodeRepository;

  @Mock
  private EmailChangeService emailChangeService;

  @InjectMocks
  private CleanupExpiredEmailChangeProcess cleanupExpiredEmailChangeProcess;

  @Nested
  class CleanupExpiredEmailChangeForUser {

    @Test
    public void shouldNotUpdateAnythingWhenNoAccountCodesFound() {
      //given
      UserEntity user = createRandomUser();
      mockMissingAccountUserCodes(user.getId());

      //when
      cleanupExpiredEmailChangeProcess.cleanupExpiredEmailChangeForUser(user);

      //then
      verify(accountUserCodeRepository, never()).setStatusForCode(any(), any());
    }

    @Test
    public void shouldUpdatesExpiredCodesAndDiscardEmailChangeProcess() {
      //given
      UserEntity user = createRandomUser();
      mockFoundAccountUserCodes(user.getId());

      //when
      cleanupExpiredEmailChangeProcess.cleanupExpiredEmailChangeForUser(user);

      //then
      verify(accountUserCodeRepository, times(1)).setStatusForCode(any(), any());
      verify(emailChangeService).discardEmailChange(user);
    }
  }

  private void mockMissingAccountUserCodes(UUID accountUserId) {
    when(accountUserCodeRepository.findByAccountUserIdAndStatusAndCodeType(accountUserId,
        CodeStatus.ACTIVE, CodeType.EMAIL_CHANGE_VERIFICATION)
    ).thenReturn(Collections.emptyList());
  }

  private void mockFoundAccountUserCodes(UUID accountUserId) {
    List<AccountUserCode> accountUserCodes = new ArrayList<>();
    accountUserCodes.add(buildAccountUserCode(LocalDateTime.now().plusDays(1)));
    accountUserCodes.add(buildAccountUserCode(LocalDateTime.now().minusDays(1)));

    when(accountUserCodeRepository.findByAccountUserIdAndStatusAndCodeType(accountUserId,
        CodeStatus.ACTIVE, CodeType.EMAIL_CHANGE_VERIFICATION)
    ).thenReturn(accountUserCodes);
  }

  private UserEntity createRandomUser() {
    return UserEntity.builder()
        .id(UUID.randomUUID())
        .identityProviderUserId(UUID.randomUUID())
        .accountId(UUID.randomUUID())
        .build();
  }

  private AccountUserCode buildAccountUserCode(LocalDateTime expiration) {
    return AccountUserCode.builder()
        .accountUserId(UUID.fromString("4e581c88-3ba3-4df0-91a3-ad46fb48bfd1"))
        .code("test")
        .expiration(expiration)
        .codeType(CodeType.EMAIL_CHANGE_VERIFICATION)
        .status(CodeStatus.ACTIVE)
        .build();
  }
}