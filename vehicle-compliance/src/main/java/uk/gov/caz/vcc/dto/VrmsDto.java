package uk.gov.caz.vcc.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

/**
 * Wrapper class for list of vrm.
 */
@Value
@Builder(toBuilder = true)
public class VrmsDto {

  @ApiModelProperty(notes = "${swagger.model.descriptions.vrms.vrmsDetails}")
  @NotNull
  @Valid
  List<String> vrms;
}