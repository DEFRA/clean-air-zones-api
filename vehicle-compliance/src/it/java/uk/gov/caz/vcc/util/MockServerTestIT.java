package uk.gov.caz.vcc.util;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
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

  protected static ClientAndServer mockServer;

  @BeforeAll
  public static void setupMockServer() {
    mockServer = startClientAndServer(1080);
  }

  @AfterAll
  public static void cleanUp(){
    mockServer.stop();
  }

  @SneakyThrows
  public static String readJson(String file) {
    return new String(
        Files.readAllBytes(ResourceUtils.getFile("classpath:data/json/response/" + file).toPath()));
  }

  public static HttpResponse response(String responseFile) {
    return HttpResponse.response()
        .withStatusCode(200)
        .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
        .withBody(readJson(responseFile));
  }

  public static HttpRequest requestGet(String url) {
    return prepareRequest(url, "GET");
  }

  public static HttpRequest requestPost(String url, String requestFile) {
    return prepareRequest(url, "POST")
        .withBody(json(readJson(requestFile), MediaType.parse("application/json")));
  }

  public static HttpRequest prepareRequest(String url, String method) {
    return HttpRequest.request()
        .withPath(url)
        .withMethod(method);
  }
}