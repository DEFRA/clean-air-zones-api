package uk.gov.caz.accounts.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.accounts.controller.PasswordController.BASE_PATH;
import static uk.gov.caz.accounts.controller.PasswordController.RESET_PATH;
import static uk.gov.caz.accounts.controller.PasswordController.SET_PATH;
import static uk.gov.caz.accounts.controller.PasswordController.VALIDATE_TOKEN_PATH;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import javax.ws.rs.core.MediaType;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.caz.GlobalExceptionHandlerConfiguration;
import uk.gov.caz.accounts.dto.PasswordResetRequest;
import uk.gov.caz.accounts.dto.SetPasswordRequest;
import uk.gov.caz.accounts.dto.ValidateTokenRequest;
import uk.gov.caz.accounts.model.CodeType;
import uk.gov.caz.accounts.service.PasswordResetService;
import uk.gov.caz.accounts.service.SetPasswordService;
import uk.gov.caz.accounts.service.UserCodeService;
import uk.gov.caz.accounts.service.exception.InvalidAccountUserPasswordResetCodeException;
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.correlationid.Constants;

@ContextConfiguration(classes = {
    GlobalExceptionHandlerConfiguration.class,
    Configuration.class,
    PasswordController.class,
    SetPasswordService.class
})
@WebMvcTest
public class PasswordControllerTest {

  private static final String ANY_CORRELATION_ID = "03d339e2-875f-4b3f-9dfa-1f6aa57cc119";
  private static final String PASSWORD_RESET_PATH = BASE_PATH + RESET_PATH;
  private static final String PASSWORD_VALIDATE_TOKEN_PATH = BASE_PATH + VALIDATE_TOKEN_PATH;
  private static final String PASSWORD_SET_PATH = BASE_PATH + SET_PATH;
  private static final String ANY_URL = "https://gov.uk";
  private static final String ANY_EMAIL = "test@email.com";
  private static final UUID ANY_TOKEN = UUID.randomUUID();
  private static final String ANY_PASSWORD = "Password";

  @MockBean
  private UserCodeService userCodeService;

  @MockBean
  private PasswordResetService passwordResetService;

  @MockBean
  private SetPasswordService setPasswordService;

  @MockBean
  private UpdatePasswordService updatePasswordService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  public void setup() {
    reset(userCodeService, passwordResetService, setPasswordService);
  }

  @Nested
  class Reset {

    @ParameterizedTest
    @ValueSource(strings = {
        "INVALID",
        "invalid@",
        "invalid@email.",
        "inva lid@email.pl"
    })
    public void invalidEmailShouldResultIn400(String email) throws Exception {
      String payload = payloadWithEmailAndResetUrl(email, ANY_URL);

      mockMvc.perform(post(PASSWORD_RESET_PATH).content(payload)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("Email is not valid."));
    }

    @Test
    public void emptyEmailShouldResultIn400() throws Exception {
      String payload = payloadWithEmailAndResetUrl("", ANY_URL);

      mockMvc.perform(post(PASSWORD_RESET_PATH).content(payload)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("Email cannot be empty."));
    }

    @Test
    public void emptyResetUrlShouldResultIn400() throws Exception {
      String payload = payloadWithResetUrl("");

      mockMvc.perform(post(PASSWORD_RESET_PATH).content(payload)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("ResetUrl cannot be empty."));
    }

    @Test
    public void shouldReturn200StatusWhenRequestWithParamsReceived() throws Exception {
      // given
      String payload = payloadWithEmailAndResetUrl(ANY_EMAIL, ANY_URL);

      // then
      mockMvc.perform(post(PASSWORD_RESET_PATH).content(payload)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isNoContent());
    }

    private String payloadWithEmailAndResetUrl(String email, String resetUrl) {
      PasswordResetRequest passwordResetRequest = PasswordResetRequest.builder()
          .email(email)
          .resetUrl(resetUrl)
          .build();

      return toJson(passwordResetRequest);
    }

    private String payloadWithResetUrl(String resetUrl) {
      PasswordResetRequest passwordResetRequest = PasswordResetRequest.builder()
          .resetUrl(resetUrl)
          .email(ANY_EMAIL)
          .build();

      return toJson(passwordResetRequest);
    }
  }

  @Nested
  class ValidateToken {

    @Test
    public void emptyTokenShouldResultIn400() throws Exception {
      String payload = payloadWithToken(null);

      mockMvc.perform(post(PASSWORD_VALIDATE_TOKEN_PATH).content(payload)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("Token cannot be null."));
    }

    @Test
    public void invalidTokenShouldResultIn400() throws Exception {
      // given
      UUID token = UUID.randomUUID();
      String payload = payloadWithToken(token);
      mockTokenIsNotValid(token);

      //then
      mockMvc.perform(post(PASSWORD_VALIDATE_TOKEN_PATH).content(payload)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("Token is invalid or expired"));
    }

    @Test
    public void shouldReturn200StatusWhenRequestWithParamsReceived() throws Exception {
      // given
      UUID token = UUID.randomUUID();
      String payload = payloadWithToken(token);
      mockTokenIsValid(token);

      // then
      mockMvc.perform(post(PASSWORD_VALIDATE_TOKEN_PATH).content(payload)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isNoContent());
    }

    private String payloadWithToken(UUID token) {
      ValidateTokenRequest validateTokenRequest = ValidateTokenRequest.builder()
          .token(token)
          .build();

      return toJson(validateTokenRequest);
    }

    private void mockTokenIsValid(UUID token) {
      when(userCodeService.isActive(token, CodeType.PASSWORD_RESET)).thenReturn(true);
    }

    private void mockTokenIsNotValid(UUID token) {
      when(userCodeService.isActive(token, CodeType.PASSWORD_RESET)).thenReturn(false);
    }
  }

  @Nested
  class SetPassword {

    @Test
    public void emptyTokenShouldResultIn400() throws Exception {
      String payload = payloadWithTokenAndPassword(null, ANY_PASSWORD);

      requestSetPassword(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("Token cannot be null."));
    }

    @Test
    public void emptyPasswordShouldResultIn400() throws Exception {
      String payload = payloadWithTokenAndPassword(ANY_TOKEN, "");

      requestSetPassword(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("Password cannot be null or empty."));
    }

    @Test
    public void nullPasswordShouldResultIn400() throws Exception {
      String payload = payloadWithTokenAndPassword(ANY_TOKEN, null);

      requestSetPassword(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("Password cannot be null or empty."));
    }

    @Test
    public void invalidTokenShouldResultIn400() throws Exception {
      // given
      String payload = payloadWithTokenAndPassword(ANY_TOKEN, ANY_PASSWORD);
      mockTokenIsNotValid(ANY_TOKEN);

      //then
      requestSetPassword(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("Token is invalid or expired"));
    }

    @Test
    public void shouldReturn204StatusWhenRequestWithValidParamsReceived()
        throws Exception {
      // given
      String payload = payloadWithTokenAndPassword(ANY_TOKEN, ANY_PASSWORD);

      // when
      ResultActions requestResponse = requestSetPassword(payload);

      // then
      verify204Response(requestResponse);
      verify(setPasswordService).process(ANY_TOKEN, ANY_PASSWORD);
    }

    private void verify204Response(ResultActions requestResponse) throws Exception {
      requestResponse
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isNoContent());
    }

    private ResultActions requestSetPassword(String payload) throws Exception {
      return mockMvc.perform(put(PASSWORD_SET_PATH).content(payload)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON));
    }

    private String payloadWithTokenAndPassword(UUID token, String password) {
      SetPasswordRequest setPasswordRequest = SetPasswordRequest.builder()
          .token(token)
          .password(password)
          .build();

      return toJson(setPasswordRequest);
    }

    private void mockTokenIsNotValid(UUID token) {
      doThrow(new InvalidAccountUserPasswordResetCodeException()).when(setPasswordService)
          .process(token, ANY_PASSWORD);
    }
  }

  @SneakyThrows
  private String toJson(Object request) {
    return objectMapper.writeValueAsString(request);
  }
}
