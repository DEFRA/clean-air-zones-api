package uk.gov.caz.definitions.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.definitions.serializers.RateDtoSerializer;

/**
 * Value object that holds rates.
 */
@Value
@Builder
public class RatesDto {

  @JsonSerialize(using = RateDtoSerializer.class)
  BigDecimal bus;

  @JsonSerialize(using = RateDtoSerializer.class)
  BigDecimal coach;

  @JsonSerialize(using = RateDtoSerializer.class)
  BigDecimal taxi;

  @JsonSerialize(using = RateDtoSerializer.class)
  BigDecimal phv;

  @JsonSerialize(using = RateDtoSerializer.class)
  BigDecimal hgv;

  @JsonSerialize(using = RateDtoSerializer.class)
  BigDecimal miniBus;

  @JsonSerialize(using = RateDtoSerializer.class)
  BigDecimal van;

  @JsonSerialize(using = RateDtoSerializer.class)
  BigDecimal car;

  @JsonSerialize(using = RateDtoSerializer.class)
  BigDecimal motorcycle;

  @JsonSerialize(using = RateDtoSerializer.class)
  BigDecimal moped;
}

