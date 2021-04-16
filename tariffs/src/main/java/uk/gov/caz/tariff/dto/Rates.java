package uk.gov.caz.tariff.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

/**
 * Value object that holds rates.
 */
@Value
@Builder
public class Rates {

  @ApiModelProperty(value = "${swagger.model.descriptions.rate.bus}")
  @JsonSerialize(using = RateSerializer.class)
  BigDecimal bus;

  @ApiModelProperty(value = "${swagger.model.descriptions.rate.coach}")
  @JsonSerialize(using = RateSerializer.class)
  BigDecimal coach;

  @ApiModelProperty(value = "${swagger.model.descriptions.rate.taxi}")
  @JsonSerialize(using = RateSerializer.class)
  BigDecimal taxi;

  @ApiModelProperty(value = "${swagger.model.descriptions.rate.phv}")
  @JsonSerialize(using = RateSerializer.class)
  BigDecimal phv;

  @ApiModelProperty(value = "${swagger.model.descriptions.rate.hgv}")
  @JsonSerialize(using = RateSerializer.class)
  BigDecimal hgv;

  @ApiModelProperty(value = "${swagger.model.descriptions.rate.miniBus}")
  @JsonSerialize(using = RateSerializer.class)
  BigDecimal miniBus;

  @ApiModelProperty(value = "${swagger.model.descriptions.rate.van}")
  @JsonSerialize(using = RateSerializer.class)
  BigDecimal van;

  @ApiModelProperty(value = "${swagger.model.descriptions.rate.car}")
  @JsonSerialize(using = RateSerializer.class)
  BigDecimal car;

  @ApiModelProperty(value = "${swagger.model.descriptions.rate.motorcycle}")
  @JsonSerialize(using = RateSerializer.class)
  BigDecimal motorcycle;

  @ApiModelProperty(value = "${swagger.model.descriptions.rate.moped}")
  @JsonSerialize(using = RateSerializer.class)
  BigDecimal moped;
}

