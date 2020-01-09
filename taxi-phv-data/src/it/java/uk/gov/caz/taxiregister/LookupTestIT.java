package uk.gov.caz.taxiregister;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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
import static uk.gov.caz.taxiregister.controller.Constants.CORRELATION_ID_HEADER;
import static uk.gov.caz.taxiregister.util.JsonReader.allCombinationVRM;
import static uk.gov.caz.taxiregister.util.JsonReader.nullWheelchairFlagActiveVRM;
import static uk.gov.caz.taxiregister.util.JsonReader.wheelchairAccessibleInactiveVRM;
import static uk.gov.caz.taxiregister.util.JsonReader.wheelchairInaccessibleActiveVRM;
import static uk.gov.caz.taxiregister.util.JsonReader.wheelchairInaccessibleInactiveVRM;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.caz.taxiregister.annotation.MockedMvcIntegrationTest;
import uk.gov.caz.taxiregister.controller.LookupController;
import uk.gov.caz.taxiregister.model.VehicleLicenceLookupInfo;

@MockedMvcIntegrationTest
@Sql(scripts = {
    "classpath:data/sql/licensing-authority-data.sql",
    "classpath:data/sql/taxi-phv-data.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@Slf4j
public class LookupTestIT {

  // valid VRMs from 'taxi-phv-data.sql`
  private static final String ALL_COMBINATION_VRM = "BD51SMR";
  private static final String WHEELCHAIR_ACCESSIBLE_INACTIVE_LICENCE_VRM = "AB51PMR";
  private static final String WHEELCHAIR_INACCESSIBLE_ACTIVE_LICENCE_VRM = "CB51QMR";
  private static final String WHEELCHAIR_INACCESSIBLE_INACTIVE_LICENCE_VRM = "DA51QMR";
  private static final String NULL_WHEELCHAIR_FLAG_ACTIVE_LICENCE_VRM = "EB12QMD";

  private static final String NOT_EXISTING_VRM = "GAD975C";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private CacheManager cacheManager;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  public void lookupTest() throws Exception {
    // at least one active licence, wheelchair accessible
    whenRequestedFor(ALL_COMBINATION_VRM)
        .thenResponseStatusIsOk()
        .withResponseBody(allCombinationVRM())
        .andCachedResponse();

    // at least one active licence, wheelchair inaccessible
    whenRequestedFor(WHEELCHAIR_INACCESSIBLE_ACTIVE_LICENCE_VRM)
        .thenResponseStatusIsOk()
        .withResponseBody(wheelchairInaccessibleActiveVRM())
        .andCachedResponse();

    // all inactive licences, wheelchair accessible
    whenRequestedFor(WHEELCHAIR_ACCESSIBLE_INACTIVE_LICENCE_VRM)
        .thenResponseStatusIsOk()
        .withResponseBody(wheelchairAccessibleInactiveVRM())
        .andCachedResponse();

    // all inactive licences, wheelchair inaccessible
    whenRequestedFor(WHEELCHAIR_INACCESSIBLE_INACTIVE_LICENCE_VRM)
        .thenResponseStatusIsOk()
        .withResponseBody(wheelchairInaccessibleInactiveVRM())
        .andCachedResponse();

    // active licence, null wheelchair flag
    whenRequestedFor(NULL_WHEELCHAIR_FLAG_ACTIVE_LICENCE_VRM)
        .thenResponseStatusIsOk()
        .withResponseBody(nullWheelchairFlagActiveVRM())
        .andCachedResponse();

    // vrm does not exist
    whenRequestedFor(NOT_EXISTING_VRM)
        .thenResponseStatusIsNotFound()
        .andCachedResponse();
  }

  private LookupAssertion whenRequestedFor(String vrm) {
    return LookupAssertion.whenRequestedFor(mockMvc, cacheManager.getCache("vehicles"),
        objectMapper, vrm);
  }

  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  private static class LookupAssertion {

    private final MockMvc mockMvc;
    private final Cache cache;
    private final ObjectMapper objectMapper;
    private final String vrm;
    private String correlationId;

    private ResultActions resultActions;
    private Map<String, Object> responseBody;

    static LookupAssertion whenRequestedFor(MockMvc mockMvc,
        Cache cache, ObjectMapper objectMapper, String vrm) {
      return new LookupAssertion(mockMvc, cache, objectMapper, vrm);
    }

    public LookupAssertion thenResponseStatusIsOk() throws Exception {
      resultActions = executeRequest()
          .andExpect(status().isOk())
          .andExpect(header().string(CORRELATION_ID_HEADER, correlationId))
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
      return this;
    }

    public LookupAssertion thenResponseStatusIsNotFound() throws Exception {
      resultActions = executeRequest()
          .andExpect(status().isNotFound())
          .andExpect(header().string(CORRELATION_ID_HEADER, correlationId));
      return this;
    }

    public LookupAssertion withResponseBody(String content) throws Exception {
      resultActions = resultActions.andExpect(content().json(content));
      responseBody = objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {
      });
      return this;
    }

    public LookupAssertion andCachedResponse() {
      assertThat(cache.get(vrm)).isNotNull();

      if (responseBody != null) {
        VehicleLicenceLookupInfo actual = cache.get(vrm, VehicleLicenceLookupInfo.class);
        assertThat(actual.getWheelchairAccessible()).isEqualTo(expectedWheelchairAccessible());
        assertThat(actual.getLicensingAuthoritiesNames()).containsExactlyInAnyOrderElementsOf(
            expectedLicensingAuthoritiesNames());
      }
      return this;
    }

    @SuppressWarnings("unchecked")
    private List<String> expectedLicensingAuthoritiesNames() {
      return (List<String>) responseBody.get("licensingAuthoritiesNames");
    }

    private Boolean expectedWheelchairAccessible() {
      return (Boolean) responseBody.get("wheelchairAccessible");
    }

    private ResultActions executeRequest() throws Exception {
      correlationId = UUID.randomUUID().toString();
      return mockMvc.perform(get(LookupController.PATH, vrm)
          .header(CORRELATION_ID_HEADER, correlationId)
          .accept(MediaType.APPLICATION_JSON_UTF8_VALUE));
    }
  }
}
