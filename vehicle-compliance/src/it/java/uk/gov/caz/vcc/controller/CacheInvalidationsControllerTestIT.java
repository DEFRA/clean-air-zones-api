package uk.gov.caz.vcc.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.SneakyThrows;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.vcc.annotation.MockedMvcIntegrationTest;
import uk.gov.caz.vcc.controller.CacheInvalidationsController;
import uk.gov.caz.vcc.dto.VrmsDto;

@MockedMvcIntegrationTest
public class CacheInvalidationsControllerTestIT {

  private static final String ANY_CORRELATION_ID = UUID.randomUUID().toString();

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  public void shouldCacheEvictWithCleanAirZones() throws Exception {
    mockMvc.perform(post(cacheInvalidationForCleanAirZonesUrl())
        .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isAccepted())
        .andExpect(header().string(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID));
  }

  @Test
  public void shouldCacheEvictWithLicences() throws Exception {
    mockMvc.perform(post(cacheInvalidationForLicencesUrl())
        .content(createPayloadWithVrms())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .header(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID))
        .andExpect(status().isAccepted())
        .andExpect(header().string(X_CORRELATION_ID_HEADER, ANY_CORRELATION_ID));
  }

  @SneakyThrows
  private String createPayloadWithVrms() {
    VrmsDto vrmsDto = VrmsDto.builder()
        .vrms(Lists.newArrayList("SW61BYD", "MK16YZR"))
        .build();

    return objectMapper.writeValueAsString(vrmsDto);
  }

  private String cacheInvalidationForCleanAirZonesUrl() {
    return CacheInvalidationsController.CACHE_INVALIDATION_PATH + "/clean-air-zones";
  }

  private String cacheInvalidationForLicencesUrl() {
    return CacheInvalidationsController.CACHE_INVALIDATION_PATH + "/licences";
  }
}