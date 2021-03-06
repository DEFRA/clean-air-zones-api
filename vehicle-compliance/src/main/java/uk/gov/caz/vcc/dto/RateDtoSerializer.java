package uk.gov.caz.vcc.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * Serializer implementation for parsing charge rates for clean air zones
 * retrieved from the Tariff API and converting response values to a
 * BigDecimal representation for internal use in the application.
 *
 */
public class RateDtoSerializer extends JsonSerializer<BigDecimal> {

  @Override
  public void serialize(BigDecimal value, JsonGenerator jgen,
      SerializerProvider provider) throws IOException {
    jgen.writeNumber(value.stripTrailingZeros().toPlainString());
  }
}
