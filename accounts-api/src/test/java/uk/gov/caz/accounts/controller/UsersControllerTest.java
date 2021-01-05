package uk.gov.caz.accounts.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.GlobalExceptionHandlerConfiguration;
import uk.gov.caz.accounts.dto.LoginRequestDto;
import uk.gov.caz.accounts.model.User;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.repository.exception.IdentityProviderUnavailableException;
import uk.gov.caz.accounts.repository.exception.InvalidCredentialsException;
import uk.gov.caz.accounts.service.AccountFetcherService;
import uk.gov.caz.accounts.service.LoginService;
import uk.gov.caz.accounts.service.UserService;
import uk.gov.caz.accounts.service.exception.EmailNotConfirmedException;
import uk.gov.caz.accounts.service.exception.UserNotFoundException;
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.correlationid.Constants;

@ExtendWith(MockitoExtension.class)
public class UsersControllerTest {

  private static final UUID ACCOUNT_USER_ID = UUID.randomUUID();
  private static final UUID ACCOUNT_ID = UUID.randomUUID();

  @Mock
  private UserService userService;

  @Mock
  private AccountFetcherService accountFetcherService;

  @InjectMocks
  private UsersController usersController;

  @Test
  public void whenThereIsNoAccountForUserThenIllegalStateExceptionIsThrown() {
    // given
    when(userService.findUser(ACCOUNT_USER_ID))
        .thenReturn(Optional.of(UserEntity.builder().accountId(ACCOUNT_ID).build()));
    when(accountFetcherService.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

    // when
    Throwable throwable = catchThrowable(() -> usersController.getUser(ACCOUNT_USER_ID));

    // then
    assertThat(throwable).isInstanceOf(IllegalStateException.class);
  }
}
