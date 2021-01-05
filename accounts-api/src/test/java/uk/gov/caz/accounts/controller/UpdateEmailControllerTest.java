package uk.gov.caz.accounts.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.caz.GlobalExceptionHandlerConfiguration;
import uk.gov.caz.accounts.dto.ConfirmEmailChangeRequest;
import uk.gov.caz.accounts.dto.InitiateEmailChangeRequest;
import uk.gov.caz.accounts.service.ConfirmEmailChangeService;
import uk.gov.caz.accounts.service.EmailChangeService;
import uk.gov.caz.correlationid.Configuration;
import uk.gov.caz.correlationid.Constants;

@ContextConfiguration(classes = {
    GlobalExceptionHandlerConfiguration.class,
    Configuration.class,
    UpdateEmailController.class,
})
@WebMvcTest
class UpdateEmailControllerTest {

  private static final String ANY_CORRELATION_ID = "03d339e2-875f-4b3f-9dfa-1f6aa57cc119";
  public static final String ANY_ACCOUNT_USER_ID = "b6d8dc6b-fb9b-4e84-a225-6830056b7d9e";
  public static final String ANY_EMAIL = "a@b.com";
  public static final String ANY_VALID_URL = "http://localhost";
  private static final String ANY_VERIFICATION_TOKEN = "04279cef-8a27-4b24-be2d-84169b37c302";
  private static final String ANY_VALID_PASSWORD = "p4ssw0rd1212...";
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private EmailChangeService emailChangeService;

  @MockBean
  private ConfirmEmailChangeService confirmEmailChangeService;

  @Nested
  class InitiateEmailChange {

    private static final String EMAIL_CHANGE_INITIATE_URL =
        UpdateEmailController.PATH + "/change-request";

    @Test
    public void shouldReturn400WhenEmailIsInvalid() throws Exception {
      String payload = requestWithPayload("invalid-email", ANY_VALID_URL, ANY_EMAIL);

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("email is not valid."));
    }

    @Test
    public void shouldReturn400WhenEmailIsBlank() throws Exception {
      String payload = requestWithPayload("", ANY_VALID_URL, ANY_EMAIL);

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("email cannot be null or empty."));
    }

    @Test
    public void shouldReturn400WhenAccountUserIdIsInvalid() throws Exception {
      String payload = requestWithPayload(ANY_EMAIL, ANY_VALID_URL, "not-uuid");

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("accountUserId should be a valid UUID."));
    }

    @Test
    public void shouldReturn400WhenAccountUserIdIsBlank() throws Exception {
      String payload = requestWithPayload(ANY_EMAIL, ANY_VALID_URL, "");

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("accountUserId cannot be blank."));
    }

    @Test
    public void shouldReturn400WhenConfirmUrlIsBlank() throws Exception {
      String payload = requestWithPayload(ANY_EMAIL, "", ANY_ACCOUNT_USER_ID);

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("confirmUrl cannot be null or empty."));
    }

    @Test
    public void shouldReturn400WhenConfirmUrlIsInvalid() throws Exception {
      String payload = requestWithPayload(ANY_EMAIL, "wrong-format of URL", ANY_ACCOUNT_USER_ID);

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("confirmUrl should be a valid URL."));
    }

    private ResultActions performRequestWithPayload(String payload) throws Exception {
      return mockMvc.perform(put(EMAIL_CHANGE_INITIATE_URL).content(payload)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));
    }

    private String requestWithPayload(String newEmail, String confirmUrl, String accountUserId) {
      InitiateEmailChangeRequest request = InitiateEmailChangeRequest
          .builder()
          .newEmail(newEmail)
          .confirmUrl(confirmUrl)
          .accountUserId(accountUserId)
          .build();

      return toJson(request);
    }
  }

  @Nested
  class ConfirmEmailChange {

    private static final String EMAIL_CHANGE_CONFIRM_URL =
        UpdateEmailController.PATH + "/change-confirm";

    @Test
    public void shouldReturn400WhenTokenIsBlank() throws Exception {
      String payload = requestWithPayload("", ANY_VALID_PASSWORD);

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("emailChangeVerificationToken cannot be null."));
    }

    @Test
    public void shouldReturn400WhenPasswordIsBlank() throws Exception {
      String payload = requestWithPayload(ANY_VERIFICATION_TOKEN, "");

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("password cannot be null or empty."));
    }

    @Test
    public void shouldReturn200WhenParamsAreValid() throws Exception {
      String updatedEmail = "updated@email.com";
      String payload = requestWithPayload(ANY_VERIFICATION_TOKEN, ANY_VALID_PASSWORD);
      when(confirmEmailChangeService.confirmEmailChange(any(), anyString()))
          .thenReturn(updatedEmail);

      performRequestWithPayload(payload)
          .andExpect(header().string(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.newEmail").value(updatedEmail));
    }

    private ResultActions performRequestWithPayload(String payload) throws Exception {
      return mockMvc.perform(put(EMAIL_CHANGE_CONFIRM_URL).content(payload)
          .header(Constants.X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));
    }

    private String requestWithPayload(String token, String password) {
      ConfirmEmailChangeRequest request = ConfirmEmailChangeRequest
          .builder()
          .emailChangeVerificationToken(token)
          .password(password)
          .build();

      return toJson(request);
    }
  }

  @SneakyThrows
  private String toJson(Object request) {
    return objectMapper.writeValueAsString(request);
  }
}