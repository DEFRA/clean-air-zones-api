package uk.gov.caz.vcc.dto;

import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.net.URI;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class CleanAirZoneDto implements Serializable {

  private static final long serialVersionUID = -244844465277860965L;

  @ApiModelProperty(value = "${swagger.model.descriptions.cleanAirZone.cleanAirZoneId}")
  @NotNull
  UUID cleanAirZoneId;

  @ApiModelProperty(value = "${swagger.model.descriptions.cleanAirZone.name}")
  @NotNull
  @Size(min = 1, max = 60)
  String name;

  @ApiModelProperty(value = "${swagger.model.descriptions.cleanAirZone.boundaryUrl}")
  URI boundaryUrl;  
  
}
