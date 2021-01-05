package uk.gov.caz.accounts.controller.util;

import com.google.common.base.Strings;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.gov.caz.accounts.controller.exception.VehicleRetrievalDtoValidationException;

@Component
@Slf4j
public class QueryStringValidator {

  private static final String PAGE_SIZE_QUERY_PARAM = "pageSize";
  private static final String CHARGEABLE_CAZ_ID = "chargeableCazId";
  private static final String QUERY_PARAM = "query";

  /**
   * Ensures a given map contains the correct keys, with correctly formatted values.
   *
   * @param map the map to check
   * @param requiredParams required keys in the map
   */
  public void validateRequest(Map<String, String> map, List<String> requiredParams) {
    List<String> errorParams = requiredParams.stream()
        .filter(param -> validateParameter(param, map))
        .collect(Collectors.toList());

    if (!errorParams.isEmpty()) {
      throw new VehicleRetrievalDtoValidationException(errorParams);
    }
  }

  private Boolean validateParameter(String key, Map<String, String> map) {
    if (key.equals(CHARGEABLE_CAZ_ID)) {
      return uuidQueryStringInvalid(key, map);
    }

    if (key.equals(QUERY_PARAM)) {
      return alphanumericStringInvalid(key, map);
    }

    return numericalQueryStringInvalid(key, map);
  }

  private Boolean uuidQueryStringInvalid(String key, Map<String, String> map) {
    // query string invalid if empty or if is not a valid UUID string
    boolean queryStringInvalid = !StringUtils.hasText(map.get(key));
    if (queryStringInvalid) {
      return true;
    }

    try {
      UUID.fromString(map.get(key));
      queryStringInvalid = false;
    } catch (IllegalArgumentException e) {
      log.info("Parameter {} was not a UUID", key);
      queryStringInvalid = true;
    }

    return queryStringInvalid;
  }

  private Boolean numericalQueryStringInvalid(String key, Map<String, String> map) {
    // query string invalid if not in map, if empty or if less than 0
    boolean queryStringInvalid = !map.containsKey(key) || !StringUtils.hasText(map.get(key));
    if (queryStringInvalid) {
      return true;
    }

    try {
      int i = Integer.parseInt(map.get(key));
      queryStringInvalid = i < 0;

      // bespoke rules
      if (key.equals(PAGE_SIZE_QUERY_PARAM)) {
        queryStringInvalid = i < 1;
      }
    } catch (Exception e) {
      log.info("Parameter {} was not a number", key);
      queryStringInvalid = true;
    }

    return queryStringInvalid;
  }

  private boolean alphanumericStringInvalid(String key, Map<String, String> map) {
    String queryParam = map.get(key);
    if (!Strings.isNullOrEmpty(queryParam) && !queryParam.matches("[a-zA-Z0-9]+")) {
      return true;
    } else {
      return false;
    }
  }
}
