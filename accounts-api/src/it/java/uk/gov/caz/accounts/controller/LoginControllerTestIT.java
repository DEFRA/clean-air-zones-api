package uk.gov.caz.accounts.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.accounts.controller.LoginController.LOGIN_PATH;
import static uk.gov.caz.accounts.util.JsonReader.readLoginResponse;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.CACHE_CONTROL_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.CACHE_CONTROL_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.CONTENT_SECURITY_POLICY_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.CONTENT_SECURITY_POLICY_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.PRAGMA_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.PRAGMA_HEADER_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.STRICT_TRANSPORT_SECURITY_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.STRICT_TRANSPORT_SECURITY_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.X_CONTENT_TYPE_OPTIONS_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.X_CONTENT_TYPE_OPTIONS_VALUE;
import static uk.gov.caz.security.SecurityHeadersInjector.X_FRAME_OPTIONS_HEADER;
import static uk.gov.caz.security.SecurityHeadersInjector.X_FRAME_OPTIONS_VALUE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.accounts.annotation.MockedMvcIntegrationTest;
import uk.gov.caz.accounts.dto.LoginRequestDto;
import uk.gov.caz.accounts.model.Account;
import uk.gov.caz.accounts.model.LoginData;
import uk.gov.caz.accounts.model.Permission;
import uk.gov.caz.accounts.model.UserEntity;
import uk.gov.caz.accounts.service.LoginService;

@MockedMvcIntegrationTest
public class LoginControllerTestIT {

  private static final String SOME_CORRELATION_ID = "63be7528-7efd-4f31-ae68-11a6b709ff1c";

  @MockBean
  private LoginService loginService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void shouldReturnInformationAboutLoggedUser() throws Exception {
    LoginRequestDto loginRequest = createLoginRequest();
    mockLoginServiceResult(loginRequest);

    mockMvc.perform(post(LOGIN_PATH)
        .content(objectMapper.writeValueAsString(loginRequest))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID))
        .andExpect(status().isOk())
        .andExpect(content().json(readLoginResponse()))
        .andExpect(
            header().string(STRICT_TRANSPORT_SECURITY_HEADER, STRICT_TRANSPORT_SECURITY_VALUE))
        .andExpect(
            header().string(PRAGMA_HEADER, PRAGMA_HEADER_VALUE))
        .andExpect(
            header().string(X_CONTENT_TYPE_OPTIONS_HEADER, X_CONTENT_TYPE_OPTIONS_VALUE))
        .andExpect(
            header().string(X_FRAME_OPTIONS_HEADER, X_FRAME_OPTIONS_VALUE))
        .andExpect(
            header().string(CONTENT_SECURITY_POLICY_HEADER, CONTENT_SECURITY_POLICY_VALUE))
        .andExpect(
            header().string(CACHE_CONTROL_HEADER, CACHE_CONTROL_VALUE));
  }

  private void mockLoginServiceResult(LoginRequestDto loginRequest) {
    when(loginService.login(loginRequest.getEmail(), loginRequest.getPassword())).thenReturn(
        LoginData.of(
            UserEntity.builder()
                .email(loginRequest.getEmail())
                .id(UUID.fromString("0aff1b60-c997-4fea-9f7b-f405ddb383b4"))
                .accountId(UUID.fromString("55701413-bc48-4e07-922c-830ac457ef52"))
                .isOwner(true)
                .build(),
            Account.builder()
                .name("AccountName")
                .build(),
            LocalDateTime.of(2020, 01, 10, 5, 42, 45),
            ImmutableList.of(Permission.MAKE_PAYMENTS),
            false
        )
    );
  }

  @ParameterizedTest
  @MethodSource("uk.gov.caz.accounts.util.TestObjectFactory#loginRequestTestDataAndExceptionMessage")
  void shouldReturnBadRequest(LoginRequestDto loginRequestDto, String message) throws Exception {
    mockMvc.perform(post(LOGIN_PATH)
        .content(objectMapper.writeValueAsString(loginRequestDto))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(message));
  }

  private LoginRequestDto createLoginRequest() {
    return LoginRequestDto.builder()
        .email("test@gov.uk")
        .password("pass")
        .build();
  }
}