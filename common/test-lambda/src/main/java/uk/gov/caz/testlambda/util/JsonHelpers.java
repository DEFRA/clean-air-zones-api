package uk.gov.caz.testlambda.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class JsonHelpers {

  private final ObjectMapper objectMapper;

  /**
   * Converts {@code input} to its JSON representation.
   *
   * @param input An object that needs to be converted.
   * @return {@code input} serialized to JSON.
   * @throws MapToJsonException upon exception while serialization.
   */
  public String toJson(Object input) {
    try {
      return objectMapper.writeValueAsString(input);
    } catch (JsonProcessingException e) {
      throw new MapToJsonException(e);
    }
  }

  /**
   * An exception that indicates an error while serializing object to JSON.
   */
  public static class MapToJsonException extends RuntimeException {

    /**
     * Creates an instance of {@link MapToJsonException} with {@code e} as a cause.
     * @param e A root cause of the error.
     */
    MapToJsonException(JsonProcessingException e) {
      super(e);
    }
  }
}
