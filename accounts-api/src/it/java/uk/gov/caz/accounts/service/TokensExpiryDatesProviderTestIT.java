package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.caz.accounts.annotation.IntegrationTest;

@IntegrationTest
class TokensExpiryDatesProviderTestIT {

  @Autowired
  private TokensExpiryDatesProvider tokensExpiryDatesProvider;

  @Test
  public void shouldReturnResetTokenExpiryDateInTheFuture() {
    LocalDateTime currentTime = LocalDateTime.now();
    LocalDateTime expiryDate = tokensExpiryDatesProvider.getResetTokenExpiryDateFromNow();

    assertThat(currentTime).isBefore(expiryDate);
  }

  @Test
  public void shouldReturnVerificationEmailExpiryDateInTheFuture() {
    LocalDateTime currentTime = LocalDateTime.now();
    LocalDateTime expiryDate = tokensExpiryDatesProvider.getVerificationEmailExpiryDateFromNow();

    assertThat(currentTime).isBefore(expiryDate);
  }
}
