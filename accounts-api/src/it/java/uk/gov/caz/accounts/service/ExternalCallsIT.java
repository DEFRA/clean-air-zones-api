package uk.gov.caz.accounts.service;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.exactly;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.http.HttpStatus;
import org.springframework.util.ResourceUtils;
import uk.gov.caz.definitions.dto.ComplianceResultsDto;

public class ExternalCallsIT {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  private static ClientAndServer vccsMockServer;
  protected static List<String> chargeableVrns = new ArrayList<>();

  @BeforeAll
  public static void startVccsMockServer() {
    vccsMockServer = startClientAndServer(1090);
  }

  @AfterAll
  public static void stopVccsMockServer() {
    vccsMockServer.stop();
  }

  @AfterEach
  public void resetMockServers() {
    vccsMockServer.reset();
  }

  public void mockVccsCleanAirZonesCall() {
    vccsMockServer
        .when(HttpRequest.request()
            .withPath("/v1/compliance-checker/clean-air-zones")
            .withMethod("GET"))
        .respond(HttpResponse.response()
            .withStatusCode(200)
            .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
            .withBody(readFile("get-clean-air-zones.json")));
  }

  public void mockVccsCleanAirZonesCallForCsvGenerator() {
    vccsMockServer
        .when(HttpRequest.request()
            .withPath("/v1/compliance-checker/clean-air-zones")
            .withMethod("GET"))
        .respond(HttpResponse.response()
            .withStatusCode(200)
            .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
            .withBody(readFile("get-clean-air-zones-for-csv-export.json")));
  }

  @SneakyThrows
  public void mockVccsBulkComplianceCallForVrnsFromRequest(UUID birminghamCazId, UUID bathCazId){
    vccsMockServer
        .when(requestPost("/v1/compliance-checker/vehicles/bulk-compliance"))
        .respond(httpRequest -> bulkComplianceResponseWithVrnAndCleanAirZoneId(
            "vehicle-compliance-response.json",
            extractVrnsFromRequest(httpRequest),
            birminghamCazId, bathCazId,
            HttpStatus.OK.value())
        );
  }

  @SneakyThrows
  public void mockVccsBulkComplianceCallForVrnsFromRequestExceptFor(UUID birminghamCazId,
      UUID bathCazId, Set<String> notExistingVrns) {
    vccsMockServer
        .when(requestPost("/v1/compliance-checker/vehicles/bulk-compliance"))
        .respond(httpRequest -> bulkComplianceResponseWithVrnAndCleanAirZoneId(
            "vehicle-compliance-response.json",
            extractVrnsFromRequest(httpRequest),
            notExistingVrns,
            birminghamCazId, bathCazId,
            HttpStatus.OK.value())
        );
  }

  @SneakyThrows
  public void mockVccsBulkComplianceCall(Set<String> vrns, UUID birminghamCazId, UUID bathCazId,
      String filePath, int statusCode) {
    vccsMockServer
        .when(requestPost("/v1/compliance-checker/vehicles/bulk-compliance"),
            exactly(1))
        .respond(bulkComplianceResponseWithVrnAndCleanAirZoneId(filePath, vrns,
            birminghamCazId, bathCazId, statusCode));
  }

  private Set<String> extractVrnsFromRequest(HttpRequest httpRequest) throws java.io.IOException {
    return objectMapper.readValue(httpRequest.getBodyAsRawBytes(),
        new TypeReference<Set<String>>() {});
  }

  public void mockVccsBulkComplianceCallWithError(int statusCode) throws JsonProcessingException {
    vccsMockServer
        .when(requestPost("/v1/compliance-checker/vehicles/bulk-compliance"),
            exactly(1))
        .respond(emptyResponse(statusCode));
  }

  public void mockVccsBulkComplianceCallError(String vrn, int statusCode) {
    vccsMockServer
        .when(requestGet("/v1/compliance-checker/vehicles/bulk-compliance"),
            exactly(1))
        .respond(emptyResponse(statusCode));
  }

  @SneakyThrows
  private String readFile(String filename) {
    return Resources.toString(Resources.getResource("data/external/" + filename),
        Charsets.UTF_8);
  }

  @SneakyThrows
  public static String readJson(String file) {
    return new String(
        Files.readAllBytes(ResourceUtils.getFile("classpath:data/json/response/" + file).toPath()));
  }

  public static HttpResponse response(String responseFile, int statusCode) {
    return HttpResponse.response()
        .withStatusCode(statusCode)
        .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
        .withBody(readJson(responseFile));
  }

  @SneakyThrows
  public static HttpResponse bulkComplianceResponseWithVrnAndCleanAirZoneId(String filePath,
      Set<String> vrns, UUID birminghamCazId, UUID bathCazId, int statusCode) {
    return bulkComplianceResponseWithVrnAndCleanAirZoneId(filePath, vrns, Collections.emptySet(),
        birminghamCazId, bathCazId, statusCode);
  }

  @SneakyThrows
  public static HttpResponse bulkComplianceResponseWithVrnAndCleanAirZoneId(String filePath,
      Set<String> vrns, Set<String> notExistingVrns, UUID birminghamCazId, UUID bathCazId,
      int statusCode) {
    List<ComplianceResultsDto> responses = new ArrayList<>();

    for (String vrnWithCompliance : Sets.difference(vrns, notExistingVrns)) {
      responses.add(getBody(filePath, vrnWithCompliance, birminghamCazId, bathCazId));
      chargeableVrns.add(vrnWithCompliance);
    }

    // nonExistingVrn -> vrn for which there is no compliance data
    for (String nonExistingVrn : Sets.intersection(notExistingVrns, vrns)) {
      responses.add(getBody("vehicle-compliance-non-existing-vrn-response.json",
          nonExistingVrn, birminghamCazId, bathCazId));
    }

    return HttpResponse.response()
        .withStatusCode(statusCode)
        .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
        .withBody(objectMapper.writeValueAsString(responses));
  }

  public static HttpResponse emptyResponse(int statusCode) {
    return HttpResponse.response()
        .withStatusCode(statusCode);
  }

  public static HttpRequest requestGet(String url) {
    return prepareRequest(url, "GET");
  }

  public static HttpRequest requestPost(String url) {
    return prepareRequest(url, "POST");
  }

  public static HttpRequest prepareRequest(String url, String method) {
    return HttpRequest.request()
        .withPath(url)
        .withMethod(method);
  }

  private static ComplianceResultsDto getBody(String filePath, String vrn, UUID birminghamCazId,
      UUID bathCazId)
      throws JsonProcessingException {
    return objectMapper.readValue(readJson(filePath).replace("TEST_VRN", vrn)
        .replace("BIRMINGHAM_CAZ_ID", birminghamCazId.toString())
        .replace("BATH_CAZ_ID", bathCazId.toString()), ComplianceResultsDto.class);
  }
}
