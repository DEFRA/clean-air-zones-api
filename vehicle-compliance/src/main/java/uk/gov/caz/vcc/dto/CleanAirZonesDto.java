package uk.gov.caz.vcc.dto;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

/**
 * Wrapper class for Clean Air Zone Listing API response object.
 *
 */
@Value
@Builder(toBuilder = true)
public class CleanAirZonesDto implements Serializable {

  private static final long serialVersionUID = -7657312800513927892L;

  @ApiModelProperty(notes = "${swagger.model.descriptions.cleanAirZones.cleanAirZoneDetails}")
  @NotNull
  @Valid
  private List<CleanAirZoneDto> cleanAirZones;
  
}
