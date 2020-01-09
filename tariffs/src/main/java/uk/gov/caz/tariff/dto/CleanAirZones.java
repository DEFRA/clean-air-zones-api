package uk.gov.caz.tariff.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
public class CleanAirZones {

  @ApiModelProperty(notes = "${swagger.model.descriptions.cleanAirZones.cleanAirZoneDetails}")
  @NotNull
  @Valid
  List<CleanAirZone> cleanAirZones;
}