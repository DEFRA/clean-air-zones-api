package uk.gov.caz.accounts.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MapPreservingOrderBuilderTest {
  
  @Test
  public void shouldPreserveOrderOfAddedElements() {
    //given
    Map<String, String> mapWithOrder = MapPreservingOrderBuilder.<String, String>builder()
        .put("1", "1")
        .put("2", "2")
        .build();

    //when
    String[] values = mapWithOrder.values().toArray(new String[2]);

    //then
    assertThat(values[0]).isEqualTo("1");
    assertThat(values[1]).isEqualTo("2");
  }
}