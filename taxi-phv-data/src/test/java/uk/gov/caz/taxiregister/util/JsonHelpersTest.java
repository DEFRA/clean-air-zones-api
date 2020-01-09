package uk.gov.caz.taxiregister.util;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.taxiregister.util.JsonHelpers.MapToJsonException;

@ExtendWith(MockitoExtension.class)
class JsonHelpersTest {

  @Mock
  private ObjectMapper objectMapper;

  @InjectMocks
  private JsonHelpers jsonHelpers;

  @Test
  public void shouldThrowMapToJsonExceptionUponMappingError() throws JsonProcessingException {
    // given
    Map<String, String> input = Collections.singletonMap("key", "value");
    BDDMockito.given(objectMapper.writeValueAsString(input)).willThrow(JsonParseException.class);

    // when
    Throwable throwable = catchThrowable(() -> jsonHelpers.toJson(input));

    // then
    assertThat(throwable).isInstanceOf(MapToJsonException.class);
  }

  @Test
  public void shouldMapInputToJson() {
    // given
    jsonHelpers = new JsonHelpers(new ObjectMapper());
    Map<String, String> input = Collections.singletonMap("key", "value");

    // when
    String json = jsonHelpers.toJson(input);

    // then
    assertThat(json).isEqualTo("{\"key\":\"value\"}");
  }
}