package uk.gov.caz.vcc.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

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
  BigDecimal largeVan;

  @JsonSerialize(using = RateDtoSerializer.class)
  BigDecimal miniBus;

  @JsonSerialize(using = RateDtoSerializer.class)
  BigDecimal smallVan;

  @JsonSerialize(using = RateDtoSerializer.class)
  BigDecimal car;

  @JsonSerialize(using = RateDtoSerializer.class)
  BigDecimal motorcycle;

  @JsonSerialize(using = RateDtoSerializer.class)
  BigDecimal moped;
}

