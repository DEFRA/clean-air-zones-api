package uk.gov.caz.vcc.util;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.JsonBody.json;

import com.google.common.net.MediaType;
import java.nio.file.Files;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.util.ResourceUtils;

public abstract class MockServerTestIT {

  private static final String VAR1_TEMPLATE = "VAR1_TEMPLATE";
  protected static ClientAndServer mockServer;

  protected static final String BATH_CAZ = "131af03c-f7f4-4aef-81ee-aae4f56dbeb5";
  protected static final String BIRMINGHAM_CAZ = "0d7ab5c4-5fff-4935-8c4e-56267c0c9493";

  @BeforeAll
  public static void setupMockServer() {
    mockServer = startClientAndServer(1080);
  }

  @AfterAll
  public static void cleanUp() {
    mockServer.stop();
  }

  @SneakyThrows
  public static String readJson(String file) {
    return new String(
        Files.readAllBytes(ResourceUtils.getFile("classpath:data/json/response/" + file).toPath()));
  }

  private static String substituteVar1With(String template, String var1Substitute) {
    return template.replace(VAR1_TEMPLATE, var1Substitute);
  }

  public static HttpResponse response(String responseFile) {
    return HttpResponse.response()
        .withStatusCode(200)
        .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
        .withBody(readJson(responseFile));
  }

  public static HttpResponse response(String responseFile, String var1Substitute) {
    return HttpResponse.response()
        .withStatusCode(200)
        .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
        .withBody(substituteVar1With(readJson(responseFile), var1Substitute));
  }

  public static HttpResponse paymentV1Response(String responseFile) {
    return HttpResponse.response()
        .withStatusCode(200)
        .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
        .withBody(readJson("payments/v1/" + responseFile));
  }

  public static HttpRequest requestGet(String url) {
    return prepareRequest(url, "GET");
  }

  public static HttpRequest requestPost(String url, String requestFile) {
    return prepareRequest(url, "POST")
        .withBody(json(readJson(requestFile), MediaType.parse("application/json")));
  }

  public static HttpRequest requestPost(String url) {
    return prepareRequest(url, "POST");
  }

  public static HttpRequest requestPost(String url, String requestFile, String var1Substitute) {
    return prepareRequest(url, "POST")
        .withBody(json(substituteVar1With(readJson(requestFile), var1Substitute),
            MediaType.parse("application/json")));
  }

  public static HttpRequest prepareRequest(String url, String method) {
    return HttpRequest.request()
        .withPath(url)
        .withMethod(method);
  }

  protected void whenEachCazHasTariffInfo() {
    whenEachCazHasTariffInfo(BIRMINGHAM_CAZ, "tariff-rates-first-response.json");
    whenEachCazHasTariffInfo(BATH_CAZ, "tariff-rates-bath-response.json");
  }

  protected void whenEachCazHasTariffInfo(String cazId, String filename) {
    mockTariffCall(cazId, filename);
  }

  protected void whenVehicleIsInTaxiDb(String vrn) {
    whenVehicleIsInTaxiDb(vrn, "ntr-first-response.json");
  }

  protected void whenVehicleIsInTaxiDb(String vrn, String filename) {
    mockServer.when(requestGet("/v1/vehicles/" + vrn + "/licence-info"))
        .respond(response(filename));
  }

  protected void whenVehiclesAreInTaxiDbBulkForBulkRetrofitTest() {
    mockServer
        .when(
            requestPost("/v1/vehicles/licences-info/search"))
        .respond(response("ntr-bulk-response-for-bulk-retrofit.json"));
  }

  protected void whenVehiclesAreInTaxiDbBulkForBulkWhitelistTest() {
    mockServer
        .when(
            requestPost("/v1/vehicles/licences-info/search"))
        .respond(response("ntr-bulk-response-for-bulk-whitelist.json"));
  }
  
  protected void whenVehiclesAreInTaxiDbBulkForBulkNTRTest() {
	    mockServer
	        .when(
	            requestPost("/v1/vehicles/licences-info/search"))
	        .respond(response("ntr-bulk-response-for-bulk-checker.json"));
	  }


  protected void whenVehiclesAreInTaxiDbBulkForChargeabilityService() {
    mockServer
        .when(
            requestPost("/v1/vehicles/licences-info/search",
                "ntr-bulk-request-for-charge-calculation-service.json"),
            exactly(1))
        .respond(response("ntr-bulk-response-for-charge-calculation-service.json"));

  }

  protected void whenVehiclesAreInTaxiDbBulkForChargeabilityServiceEuroStatus() {
    mockServer
        .when(
            requestPost("/v1/vehicles/licences-info/search",
                "ntr-bulk-request-for-charge-calculation-service-euro-status.json"),
            exactly(1))
        .respond(response("ntr-bulk-response-for-charge-calculation-service-euro-status.json"));

  }

  protected void whenVehicleIsNotInTaxiDb(String vrn) {
    mockServer.when(requestGet("/v1/vehicles/" + vrn + "/licence-info"))
        .respond(HttpResponse.response().withStatusCode(404));
  }

  protected void whenVehicleIsNotInTaxiDbBulk(String vrn) {
    mockServer
        .when(
            requestPost("/v1/vehicles/licences-info/search", "ntr-bulk-details-request.json", vrn),
            exactly(1))
        .respond(response("ntr-empty-bulk-response.json"));
  }

  public void mockModForVrn(String vrn) {
    mockServer.when(requestGet("/v1/mod/" + vrn))
        .respond(response("mod-vehicle-response.json"));
  }

  public void mockModForVrns() {
    mockServer.when(HttpRequest.request("/v1/mod/search"))
        .respond(response("mod-vehicle-bulk-search-response.json"));
  }

  protected void whenCazInfoIsInTariffService() {
    whenCazInfoIsInTariffService("/v1/clean-air-zones", "caz-first-response.json");
  }

  protected void whenCazInfoIsInTariffService(String path, String filename) {
    mockServer.when(requestGet(path))
        .respond(response(filename));
  }

  protected void mockCazListCall(String file) {
    mockServer.when(requestGet("/v1/clean-air-zones"))
        .respond(response(file));
  }

  protected void mockTariffCall(String cazId, String file) {
    mockServer.when(requestGet("/v1/clean-air-zones/" + cazId + "/tariff"))
        .respond(response(file));
  }
}