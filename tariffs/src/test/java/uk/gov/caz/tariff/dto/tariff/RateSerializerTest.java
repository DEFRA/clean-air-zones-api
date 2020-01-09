package uk.gov.caz.tariff.dto.tariff;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import uk.gov.caz.tariff.dto.RateSerializer;

class RateSerializerTest {

  @Test
  public void shouldRemoveZeros() throws IOException {
    Writer jsonWriter = new StringWriter();
    JsonGenerator jsonGenerator = new JsonFactory().createGenerator(jsonWriter);
    new RateSerializer().serialize(new BigDecimal(50.00), jsonGenerator, null);
    jsonGenerator.flush();

    assertThat(jsonWriter.toString()).isEqualTo("50");
  }
}