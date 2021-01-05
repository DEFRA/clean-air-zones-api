package uk.gov.caz.accounts.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.accounts.dto.LoginRequestDto;
import uk.gov.caz.accounts.repository.exception.IdentityProviderUnavailableException;
import uk.gov.caz.accounts.repository.exception.InvalidCredentialsException;
import uk.gov.caz.accounts.repository.exception.PendingEmailChangeException;
import uk.gov.caz.accounts.service.LoginService;
import uk.gov.caz.accounts.service.exception.EmailNotConfirmedException;
import uk.gov.caz.accounts.service.exception.UserNotFoundException;
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.correlationid.Constants;

@ContextConfiguration(classes = {
    ExceptionController.class,
    Configuration.class,
    LoginController.class
})
@WebMvcTest
public class LoginControllerTest {

  @MockBean
  private LoginService loginService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  private static final String LOGIN_PATH = LoginController.LOGIN_PATH;
  private static final String ANY_CORRELATION_ID = "03d339e2-875f-4b3f-9dfa-1f6aa57cc119";

  @Test
  public void shouldReturn500StatusCodeWhenUserAuthorizedButNotFoundInDB() throws Exception {
    // given
    String payload = requestPayload();

    // when
    doThrow(new UserNotFoundException("User not found"))
        .when(loginService).login(any(), any());

    // then
    mockMvc.perform(post(LOGIN_PATH).content(payload)
        .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.message").value("User not found"));
  }

  @Test
  public void shouldReturn401StatusCodeWhenUserUnauthorized() throws Exception {
    // given
    String payload = requestPayload();

    // when
    doThrow(new InvalidCredentialsException("Invalid credentials"))
        .when(loginService).login(any(), any());

    // then
    mockMvc.perform(post(LOGIN_PATH).content(payload)
        .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value("401"))
        .andExpect(jsonPath("$.errorCode").value("invalidCredentials"))
        .andExpect(jsonPath("$.message").value("Invalid credentials"));
  }

  @Test
  public void shouldReturn401StatusCodeWhenEmailChangeInProgress() throws Exception {
    // given
    String payload = requestPayload();

    // when
    doThrow(new PendingEmailChangeException("Pending email change")).when(loginService)
        .login(any(), any());

    // then
    mockMvc.perform(post(LOGIN_PATH).content(payload)
        .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value("401"))
        .andExpect(jsonPath("$.errorCode").value("pendingEmailChange"))
        .andExpect(jsonPath("$.message").value("Pending email change"));
  }

  @Test
  public void shouldReturn422StatusCodeWhenUserEmailIsNotConfirmed() throws Exception {
    // given
    String payload = requestPayload();

    // when
    doThrow(new EmailNotConfirmedException())
        .when(loginService).login(any(), any());

    // then
    mockMvc.perform(post(LOGIN_PATH).content(payload)
        .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.status").value(422))
        .andExpect(jsonPath("$.message").value("Email not confirmed"));
  }

  @Test
  public void shouldReturn503StatusCodeWhenIdentityProviderFails() throws Exception {
    // given
    String payload = requestPayload();

    // when
    doThrow(new IdentityProviderUnavailableException("External Service Failure"))
        .when(loginService).login(any(), any());

    // then
    mockMvc.perform(post(LOGIN_PATH).content(payload)
        .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
        .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value(503))
        .andExpect(jsonPath("$.message").value("External Service Failure"));
  }

  private String requestPayload() {
    LoginRequestDto request = LoginRequestDto.builder()
        .email("test@email.com")
        .password("password")
        .build();
    return toJson(request);
  }

  @SneakyThrows
  private String toJson(Object request) {
    return objectMapper.writeValueAsString(request);
  }
}
