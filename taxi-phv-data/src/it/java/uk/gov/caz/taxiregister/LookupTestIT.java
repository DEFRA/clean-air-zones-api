package uk.gov.caz.taxiregister;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import static uk.gov.caz.taxiregister.util.JsonReader.multipleLicencesVRM;
import static uk.gov.caz.taxiregister.util.JsonReader.nullWheelchairFlagActiveVRM;
import static uk.gov.caz.taxiregister.util.JsonReader.wheelchairAccessibleInactiveVRM;
import static uk.gov.caz.taxiregister.util.JsonReader.wheelchairInaccessibleActiveVRM;
import static uk.gov.caz.taxiregister.util.JsonReader.wheelchairInaccessibleInactiveVRM;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.caz.taxiregister.annotation.MockedMvcIntegrationTest;
import uk.gov.caz.taxiregister.controller.LookupController;
import uk.gov.caz.taxiregister.dto.LicenceInfo;
import uk.gov.caz.taxiregister.dto.lookup.GetLicencesInfoRequestDto;
import uk.gov.caz.taxiregister.dto.lookup.GetLicencesInfoResponseDto;
import uk.gov.caz.taxiregister.model.VehicleLicenceLookupInfo;

@MockedMvcIntegrationTest
@Slf4j
public class LookupTestIT {

  // valid VRMs from 'taxi-phv-data.sql`
  private static final String ALL_COMBINATION_VRM = "BD51SMR";
  private static final String WHEELCHAIR_ACCESSIBLE_INACTIVE_LICENCE_PHV_VRM = "AB51PMR";
  private static final String WHEELCHAIR_INACCESSIBLE_ACTIVE_LICENCE_PHV_VRM = "CB51QMR";
  private static final String WHEELCHAIR_INACCESSIBLE_INACTIVE_LICENCE_VRM = "DA51QMR";
  private static final String NULL_WHEELCHAIR_FLAG_ACTIVE_LICENCE_VRM = "EB12QMD";
  private static final String MULTIPLE_ACTIVE_LICENCES_VRM = "FD51SMP";

  private static final String NOT_EXISTING_VRM = "GAD975C";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private CacheManager cacheManager;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  public void setUpDb() {
    executeSqlFrom("data/sql/licensing-authority-data.sql");
    executeSqlFrom("data/sql/taxi-phv-data.sql");
  }

  @AfterEach
  public void cleanUpDb() {
    executeSqlFrom("data/sql/clear.sql");
  }

  @Nested
  class SingleLookup {

    @Test
    public void lookupTest() throws Exception {
      // at least one active licence, wheelchair accessible
      whenRequestedFor(ALL_COMBINATION_VRM)
          .thenResponseStatusIsOk()
          .withResponseBody(allCombinationVRM())
          .andCachedResponse();

      // at least one active licence, wheelchair inaccessible
      whenRequestedFor(WHEELCHAIR_INACCESSIBLE_ACTIVE_LICENCE_PHV_VRM)
          .thenResponseStatusIsOk()
          .withResponseBody(wheelchairInaccessibleActiveVRM())
          .andCachedResponse();

      // all inactive licences, wheelchair accessible
      whenRequestedFor(WHEELCHAIR_ACCESSIBLE_INACTIVE_LICENCE_PHV_VRM)
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

      // vrm has multiple licences and some of them will be active in the future
      whenRequestedFor(MULTIPLE_ACTIVE_LICENCES_VRM)
          .thenResponseStatusIsOk()
          .withResponseBody(multipleLicencesVRM())
          .andCachedResponse();
    }
  }

  @Nested
  class BatchLookup {

    @Test
    public void lookupTest() throws Exception {
      whenBatchRequestedFor(
          ALL_COMBINATION_VRM,
          WHEELCHAIR_INACCESSIBLE_ACTIVE_LICENCE_PHV_VRM,
          WHEELCHAIR_ACCESSIBLE_INACTIVE_LICENCE_PHV_VRM,
          WHEELCHAIR_INACCESSIBLE_INACTIVE_LICENCE_VRM,
          NULL_WHEELCHAIR_FLAG_ACTIVE_LICENCE_VRM,
          NOT_EXISTING_VRM
      ).thenResponseStatusIsOk()
          .andLicenceInfoIsPresentAndEqualTo(ALL_COMBINATION_VRM, allCombinationVRM())
          .andLicenceInfoIsPresentAndEqualTo(WHEELCHAIR_INACCESSIBLE_ACTIVE_LICENCE_PHV_VRM,
              wheelchairInaccessibleActiveVRM())
          .andLicenceInfoIsPresentAndEqualTo(WHEELCHAIR_ACCESSIBLE_INACTIVE_LICENCE_PHV_VRM,
              wheelchairAccessibleInactiveVRM())
          .andLicenceInfoIsPresentAndEqualTo(WHEELCHAIR_INACCESSIBLE_INACTIVE_LICENCE_VRM,
              wheelchairInaccessibleInactiveVRM())
          .andLicenceInfoIsPresentAndEqualTo(NULL_WHEELCHAIR_FLAG_ACTIVE_LICENCE_VRM,
              nullWheelchairFlagActiveVRM())
          .andLicenceInfoIsAbsentFor(NOT_EXISTING_VRM);
    }

    @Nested
    class AbsentVrm {

      @Test
      public void shouldReturnEmptyMap() throws Exception {
        whenBatchRequestedFor(NOT_EXISTING_VRM)
            .thenResponseStatusIsOk()
            .responseIsEmpty();
      }
    }

    @Nested
    class WithNullVrns {

      @Test
      public void shouldReturn400StatusCode() throws Exception {
        whenBatchRequestedWithNullVrms()
            .thenResponseStatusIsBadRequest();
      }
    }

    @Nested
    class WithEmptyVrns {

      @Test
      public void shouldReturnEmptyMap() throws Exception {
        whenBatchRequestedWithEmptyVrns()
            .thenResponseStatusIsOk()
            .responseIsEmpty();
      }
    }

    @Nested
    class WithTooLongVrns {

      @Test
      public void shouldReturn400StatusCode() throws Exception {
        whenBatchRequestedFor("NO03KN", "toolongvrnpassedasargument")
            .thenResponseStatusIsBadRequest();
      }
    }

    @Nested
    class WithTooShortVrns {

      @Test
      public void shouldReturn400StatusCode() throws Exception {
        whenBatchRequestedFor("NO03KN", "a")
            .thenResponseStatusIsBadRequest();
      }
    }

    @Nested
    class WithTooManyVrns {

      @Test
      public void shouldReturn400StatusCode() throws Exception {
        List<String> vrms = IntStream.rangeClosed(1, GetLicencesInfoRequestDto.MAX_SIZE + 1)
            .mapToObj(i -> "AB" + i)
            .collect(Collectors.toList());

        whenBatchRequestedFor(vrms)
            .thenResponseStatusIsBadRequest();
      }
    }
  }

  private void executeSqlFrom(String classPathFile) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScripts(new ClassPathResource(classPathFile));
    populator.execute(dataSource);
  }

  private SingleLookupAssertion whenRequestedFor(String vrm) {
    return SingleLookupAssertion.whenRequestedFor(mockMvc, cacheManager.getCache("vehicles"),
        objectMapper, vrm);
  }

  private BatchLookupAssertion whenBatchRequestedFor(String... vrms) {
    return BatchLookupAssertion.whenBatchRequestedFor(mockMvc, objectMapper, Arrays.asList(vrms));
  }

  private BatchLookupAssertion whenBatchRequestedFor(List<String> vrms) {
    return BatchLookupAssertion.whenBatchRequestedFor(mockMvc, objectMapper, vrms);
  }

  private BatchLookupAssertion whenBatchRequestedWithNullVrms() {
    return BatchLookupAssertion.whenBatchRequestedFor(mockMvc, objectMapper, null);
  }

  private BatchLookupAssertion whenBatchRequestedWithEmptyVrns() {
    return BatchLookupAssertion.whenBatchRequestedFor(mockMvc, objectMapper,
        Collections.emptyList());
  }

  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  private static class BatchLookupAssertion {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final List<String> vrms;
    private String correlationId;

    private GetLicencesInfoResponseDto responseBody;
    private ResultActions resultActions;

    static BatchLookupAssertion whenBatchRequestedFor(MockMvc mockMvc,
        ObjectMapper objectMapper, List<String> vrms) {
      return new BatchLookupAssertion(mockMvc, objectMapper, vrms);
    }

    public BatchLookupAssertion thenResponseStatusIsOk() throws Exception {
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
      responseBody = objectMapper.readValue(
          resultActions.andReturn().getResponse().getContentAsString(),
          GetLicencesInfoResponseDto.class
      );
      return this;
    }

    private ResultActions executeRequest() throws Exception {
      correlationId = UUID.randomUUID().toString();
      GetLicencesInfoRequestDto request = new GetLicencesInfoRequestDto(
          vrms == null ? null : Sets.newHashSet(vrms));
      return mockMvc.perform(post(LookupController.BULK_PATH)
          .content(objectMapper.writeValueAsString(request))
          .header(CORRELATION_ID_HEADER, correlationId)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .accept(MediaType.APPLICATION_JSON_VALUE));
    }

    @SneakyThrows
    public BatchLookupAssertion andLicenceInfoIsPresentAndEqualTo(String vrm,
        String expectedValue) {
      assertThat(responseBody.getLicencesInformation()).containsKey(vrm);
      LicenceInfo actual = responseBody.getLicencesInformation().get(vrm);
      LicenceInfo expected = objectMapper.readValue(expectedValue, LicenceInfo.class);
      assertThat(actual).isEqualTo(expected);
      return this;
    }

    public BatchLookupAssertion andLicenceInfoIsAbsentFor(String notExistingVrm) {
      assertThat(responseBody.getLicencesInformation()).doesNotContainKey(notExistingVrm);
      return this;
    }

    public void responseIsEmpty() {
      assertThat(responseBody.getLicencesInformation()).isEmpty();
    }

    public void thenResponseStatusIsBadRequest() throws Exception {
      executeRequest().andExpect(status().isBadRequest());
    }
  }

  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  private static class SingleLookupAssertion {

    private final MockMvc mockMvc;
    private final Cache cache;
    private final ObjectMapper objectMapper;
    private final String vrm;
    private String correlationId;

    private ResultActions resultActions;
    private Map<String, Object> responseBody;

    static SingleLookupAssertion whenRequestedFor(MockMvc mockMvc,
        Cache cache, ObjectMapper objectMapper, String vrm) {
      return new SingleLookupAssertion(mockMvc, cache, objectMapper, vrm);
    }

    public SingleLookupAssertion thenResponseStatusIsOk() throws Exception {
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

    public SingleLookupAssertion thenResponseStatusIsNotFound() throws Exception {
      resultActions = executeRequest()
          .andExpect(status().isNotFound())
          .andExpect(header().string(CORRELATION_ID_HEADER, correlationId));
      return this;
    }

    public SingleLookupAssertion withResponseBody(String content) throws Exception {
      resultActions = resultActions.andExpect(content().json(content));
      responseBody = objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {
      });
      return this;
    }

    public SingleLookupAssertion andCachedResponse() {
      assertThat(cache.get(vrm)).isNotNull();

      if (responseBody != null) {
        VehicleLicenceLookupInfo actual = cache.get(vrm, VehicleLicenceLookupInfo.class);
        assertThat(actual.getWheelchairAccessible()).isEqualTo(expectedWheelchairAccessible());
        assertThat(actual.getDescription()).isEqualTo(expectedDescription());
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

    private String expectedDescription() {
      return (String) responseBody.get("description");
    }

    private ResultActions executeRequest() throws Exception {
      correlationId = UUID.randomUUID().toString();
      return mockMvc.perform(get(LookupController.PATH, vrm)
          .header(CORRELATION_ID_HEADER, correlationId)
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .accept(MediaType.APPLICATION_JSON_VALUE));
    }
  }
}
