package uk.gov.caz.accounts.util;

import java.nio.file.Files;
import lombok.SneakyThrows;
import org.springframework.util.ResourceUtils;

public class JsonReader {

  private static final String CLASSPATH_DATA_JSON = "classpath:data/json/";

  @SneakyThrows
  private static String readJson(String file) {
    return new String(Files.readAllBytes(ResourceUtils.getFile(file).toPath()));
  }

  public static String readAccountResponse() {
    return readJson(CLASSPATH_DATA_JSON + "account-response.json");
  }

  public static String readLoginResponse() {
    return readJson(CLASSPATH_DATA_JSON + "login-response.json");
  }
}