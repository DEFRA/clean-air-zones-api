package uk.gov.caz.tariff.util;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;

public class JsonReader {

  private static final String DATA_JSON = "data/json/";

  private static String readJson(String resourceName) throws IOException {
    return Resources.toString(Resources.getResource(resourceName), Charsets.UTF_8);
  }

  public static String sampleTariffJson() throws IOException {
    return readJson(DATA_JSON + "sample-tariff.json");
  }

  public static String tariffJson() throws IOException {
    return readJson(DATA_JSON + "tariff.json");
  }

  public static String sampleCleanAirZonesJson() throws IOException {
    return readJson(DATA_JSON + "sample-clean-air-zones.json");
  }

  public static String cleanAirZonesJson() throws IOException {
    return readJson(DATA_JSON + "clean-air-zones.json");
  }
}