package uk.gov.caz.accounts.service.emailnotifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.model.Account;

@ExtendWith(MockitoExtension.class)
class UserInvitationEmailServiceTest {

  @Mock
  private ObjectMapper objectMapper;

  private UserInvitationEmailService userInvitationEmailService;

  @BeforeEach
  public void setUp() {
    userInvitationEmailService = new UserInvitationEmailService("some-template",
        objectMapper);
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionWhenContextDoesNotHaveAccount() {
    // given
    EmailContext context = EmailContext.empty();

    // when
    Throwable throwable = catchThrowable(() ->
        userInvitationEmailService.createPersonalisationMap(context));

    // then
    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Account cannot be null");
  }

  @Test
  public void shouldReturnAccountNameInMapUnderOrganisationKey() {
    // given
    String accountName = "my-account";
    EmailContext context = createContextWithAccount(accountName);

    // when
    Map<String, Object> result = userInvitationEmailService
        .createPersonalisationMap(context);

    // then
    assertThat(result).hasSize(1);
    assertThat(result).containsEntry("organisation", accountName);
  }

  private EmailContext createContextWithAccount(String accountName) {
    Account account = Account.builder().name(accountName).build();
    return EmailContext.of(account);
  }

}