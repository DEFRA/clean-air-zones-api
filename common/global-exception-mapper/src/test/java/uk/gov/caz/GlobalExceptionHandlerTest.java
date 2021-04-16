package uk.gov.caz;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.TestController.ERROR_STATUS;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {GlobalExceptionHandlerConfiguration.class, TestController.class})
@WebMvcTest
class GlobalExceptionHandlerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void shouldMapExceptionFromControllerToDefault() throws Exception {
    mockMvc.perform(get("/test/standardMessage")
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.message").value(GlobalExceptionHandler.ERROR_MESSAGE))
        .andExpect(jsonPath("$.status").value(500));
  }

  @Test
  void shouldMapExceptionFromControllerAndHideExceptionDetailsIfResponseStatusWithoutReasonIsSetOnException()
      throws Exception {
    mockMvc.perform(get("/test/customMessageWithoutReason")
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is(ERROR_STATUS.value()))
        .andExpect(jsonPath("$.message").value(GlobalExceptionHandler.ERROR_MESSAGE))
        .andExpect(jsonPath("$.status").value(ERROR_STATUS.value()));
  }

  @Test
  void shouldMapExceptionFromControllerAndShowReasonIfResponseStatusWithReasonIsSetOnException()
      throws Exception {
    mockMvc.perform(get("/test/customMessageWithReason")
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is(ERROR_STATUS.value()))
        .andExpect(jsonPath("$.message").value("Custom reason"))
        .andExpect(jsonPath("$.status").value(ERROR_STATUS.value()));
  }

  @Test
  void shouldMapResponseStatusExceptionFromControllerWithProperMessage()
      throws Exception {
    mockMvc.perform(get("/test/responseStatusException")
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is(NOT_FOUND.value()))
        .andExpect(jsonPath("$.message").value(TestController.CUSTOM_ERROR_MESSAGE))
        .andExpect(jsonPath("$.status").value(NOT_FOUND.value()));
  }

  @Test
  void shouldMapApplicationRuntimeExceptionWithCustomMessage()
      throws Exception {
    mockMvc.perform(get("/test/customExceptionThrownWithCustomMessage")
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is(BAD_GATEWAY.value()))
        .andExpect(jsonPath("$.message").value(TestController.CUSTOM_ERROR_MESSAGE))
        .andExpect(jsonPath("$.status").value(BAD_GATEWAY.value()));
  }

  @Test
  void shouldMapApplicationRuntimeExceptionWithCustomMessageFromReasonIfExists()
      throws Exception {
    mockMvc.perform(get("/test/customExceptionThrownWithCustomMessageAsReason")
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is(BAD_GATEWAY.value()))
        .andExpect(jsonPath("$.message").value(TestController.CUSTOM_ERROR_MESSAGE))
        .andExpect(jsonPath("$.status").value(BAD_GATEWAY.value()));
  }

  @Test
  void shouldMapExceptionToXml() throws Exception {
    mockMvc.perform(get("/test/standardMessage")
        .accept(MediaType.APPLICATION_XML))
        .andExpect(status().isInternalServerError())
        .andExpect(MockMvcResultMatchers.xpath("/errorResponse/status").number(500d))
        .andExpect(MockMvcResultMatchers.xpath("/errorResponse/message")
            .string(GlobalExceptionHandler.ERROR_MESSAGE));
  }

  @Test
  void shouldMapSpringExceptionToProperResponse()
      throws Exception {
    mockMvc.perform(post("/test/springConversionException")
        .content("{\"uuid\" : \"invalid\"")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is(BAD_REQUEST.value()))
        .andExpect(noResponseBody());
  }

  private ResultMatcher noResponseBody() {
    return jsonPath("$").doesNotExist();
  }
}