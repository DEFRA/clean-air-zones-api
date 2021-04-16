package uk.gov.caz.correlationid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@ContextConfiguration(classes = {Configuration.class, TestController.class})
@WebMvcTest
class CorrelationIdTest {

  @SpyBean
  private MdcAdapter mdc;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void shouldGet200IfXCorrelationIdHeaderNotPresentAndInterceptorIsNotConfiguredOnThisPath()
      throws Exception {
    mockMvc.perform(get("/test"))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist(X_CORRELATION_ID_HEADER));
  }

  @ParameterizedTest
  @ValueSource(strings = {"/test", "/test-with-response-body"})
  void shouldGet200IfXCorrelationIdHeaderIsPresentAndInterceptorIsNotConfiguredOnThisPath(
      String url)
      throws Exception {
    String correlationId = UUID.randomUUID().toString();
    mockMvc.perform(get(url)
        .header(X_CORRELATION_ID_HEADER, correlationId))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist(X_CORRELATION_ID_HEADER));

    verify(mdc, never()).put(X_CORRELATION_ID_HEADER, correlationId);
  }

  @ParameterizedTest
  @ValueSource(strings = {"/v1/test-without-response-body", "/v1/test-with-response-body"})
  void shouldGet200IfXCorrelationIdHeaderIsPresentAndRequestIsForV1Path(String url)
      throws Exception {
    String correlationId = UUID.randomUUID().toString();
    mockMvc.perform(get(url)
        .header(X_CORRELATION_ID_HEADER, correlationId))
        .andExpect(status().isOk())
        .andExpect(header().string(X_CORRELATION_ID_HEADER, correlationId));

    verify(mdc).remove(X_CORRELATION_ID_HEADER);
  }

  @ParameterizedTest
  @ValueSource(strings = {"/v1/test-without-response-body", "/v1/test-with-response-body"})
  void shouldGet400IfXCorrelationIdHeaderIsNotPresentAndRequestIsForV1Path(String url)
      throws Exception {
    mockMvc.perform(get(url))
        .andExpect(status().isBadRequest())
        .andExpect(header().doesNotExist(X_CORRELATION_ID_HEADER));
  }

  @ParameterizedTest
  @ValueSource(strings = {"/v1/test-without-response-body", "/v1/test-with-response-body"})
  void shouldGet400IfXCorrelationIdHeaderHasWrongFormat(String url)
      throws Exception {
    mockMvc.perform(get(url)
        .header(X_CORRELATION_ID_HEADER, "not uuid format"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldGet500WithCorrelationIdHeaderIfHeaderIsPresentAndExceptionIsThrown()
      throws Exception {
    String correlationId = UUID.randomUUID().toString();
    mockMvc.perform(get("/v1/throw-exception")
        .header(X_CORRELATION_ID_HEADER, correlationId))
        .andExpect(status().isOk())
        .andExpect(header().string(X_CORRELATION_ID_HEADER, correlationId));

    verify(mdc).remove(X_CORRELATION_ID_HEADER);
  }

  @Test
  void shouldGet400WithoutCorrelationIdHeaderIfHeaderIsAbsentAndExceptionIsThrown()
      throws Exception {
    mockMvc.perform(get("/v1/throw-exception"))
        .andExpect(status().isBadRequest())
        .andExpect(header().doesNotExist(X_CORRELATION_ID_HEADER));
  }

  @Test
  void shouldReturnCurrentCorrelationIdFromMDC() {
    MDC.put(X_CORRELATION_ID_HEADER, "1");
    assertThat(MdcCorrelationIdInjector.getCurrentValue()).isEqualTo("1");
  }
}