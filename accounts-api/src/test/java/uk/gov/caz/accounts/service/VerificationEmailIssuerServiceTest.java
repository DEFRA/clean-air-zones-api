package uk.gov.caz.accounts.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.AccountUserCodeRepository;
import uk.gov.caz.accounts.service.emailnotifications.VerificationEmailSender;

@ExtendWith(MockitoExtension.class)
class VerificationEmailIssuerServiceTest {

  @Mock
  private TokenToHashConverter tokenToHashConverter;

  @Mock
  private VerificationEmailSender verificationEmailSender;

  @Mock
  private AccountUserCodeRepository accountUserCodeRepository;

  @InjectMocks
  private VerificationEmailIssuerService verificationEmailIssuerService;

  private static final URI ANY_VERIFICATION_URI = URI.create("http://example.com");

  private static final LocalDateTime ANY_EXPIRATION_DATE = LocalDateTime.now().plusDays(1);

  private static final String ANY_EMAIL = "sample@email.com";

  private static final String ANY_HASHED_TOKEN = "sample-hashed-token";

  @Test
  public void shouldCreateNewVerificationCode() {
    UserEntity user = getSampleUser();
    mockTokenGeneration();

    verificationEmailIssuerService.generateVerificationTokenAndSendVerificationEmail(
        user, ANY_VERIFICATION_URI, ANY_EXPIRATION_DATE
    );

    verify(accountUserCodeRepository).save(any());
  }

  @Test
  public void shouldCallEmailSender() {
    UserEntity user = getSampleUser();
    mockTokenGeneration();

    verificationEmailIssuerService.generateVerificationTokenAndSendVerificationEmail(
        user, ANY_VERIFICATION_URI, ANY_EXPIRATION_DATE
    );

    verify(verificationEmailSender).send(any(), any());
  }

  private void mockTokenGeneration() {
    when(tokenToHashConverter.convert(any())).thenReturn(ANY_HASHED_TOKEN);
  }

  private UserEntity getSampleUser() {
    return UserEntity.builder()
        .id(UUID.randomUUID())
        .accountId(UUID.randomUUID())
        .identityProviderUserId(UUID.randomUUID())
        .email(ANY_EMAIL)
        .emailVerified(true)
        .isOwner(true)
        .build();
  }
}