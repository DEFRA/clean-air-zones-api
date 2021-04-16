package uk.gov.caz.taxiregister.util;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;

public class JsonReader {

  private static final String DATA_JSON_PATH = "data/json/lookup/";

  private static String readJson(String resourceName) throws IOException {
    return Resources.toString(Resources
        .getResource(DATA_JSON_PATH + resourceName), Charsets.UTF_8);
  }

  public static String allCombinationVRM() throws IOException {
    return readJson("allCombinationVRM.json");
  }

  public static String wheelchairInaccessibleActiveVRM() throws IOException {
    return readJson("wheelchairInaccessibleActiveVRM.json");
  }

  public static String wheelchairAccessibleInactiveVRM() throws IOException {
    return readJson("wheelchairAccessibleInactiveVRM.json");
  }

  public static String wheelchairInaccessibleInactiveVRM() throws IOException {
    return readJson("wheelchairInaccessibleInactiveVRM.json");
  }

  public static String nullWheelchairFlagActiveVRM() throws IOException {
    return readJson("nullWheelchairFlagActiveVRM.json");
  }

  public static String multipleLicencesVRM() throws IOException {
    return readJson("multipleLicencesVRM.json");
  }
}