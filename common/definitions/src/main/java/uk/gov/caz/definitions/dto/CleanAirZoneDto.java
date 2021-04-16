package uk.gov.caz.definitions.dto;

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

  private static final long serialVersionUID = 4591587329402037941L;

  @ApiModelProperty(value = "${swagger.model.descriptions.cleanAirZone.cleanAirZoneId}")
  @NotNull
  UUID cleanAirZoneId;

  @ApiModelProperty(value = "${swagger.model.descriptions.cleanAirZone.name}")
  @NotNull
  @Size(min = 1, max = 60)
  String name;

  @ApiModelProperty(value = "${swagger.model.descriptions.cleanAirZone.boundaryUrl}")
  URI boundaryUrl;

  @ApiModelProperty(value = "${swagger.model.descriptions.cleanAirZone.exemptionUrl}")
  URI exemptionUrl;

  @ApiModelProperty(value = "${swagger.model.descriptions.cleanAirZone.mainInfoUrl}")
  URI mainInfoUrl;

  @ApiModelProperty(value = "${swagger.model.descriptions.cleanAirZone.paymentsComplianceUrl}")
  URI paymentsComplianceUrl;

  @ApiModelProperty(value = "${swagger.model.descriptions.cleanAirZone.activeChargeStartDateText}")
  String activeChargeStartDateText;

  @ApiModelProperty(value = "${swagger.model.descriptions.cleanAirZone.displayFrom}")
  String displayFrom;

  @ApiModelProperty(value = "${swagger.model.descriptions.cleanAirZone.displayOrder}")
  Integer displayOrder;

  @ApiModelProperty(value = "${swagger.model.descriptions.cleanAirZone.privacyPolicyUrl}")
  URI privacyPolicyUrl;

  @ApiModelProperty(value = "${swagger.model.descriptions.cleanAirZone.fleetsComplianceUrl}")
  URI fleetsComplianceUrl;

  @ApiModelProperty(value = "${swagger.model.descriptions.cleanAirZone.activeChargeStartDate}")
  String activeChargeStartDate;

  @ApiModelProperty(value = "${swagger.model.descriptions.cleanAirZone.operatorName}")
  String operatorName;

  @ApiModelProperty(value = "${swagger.model.descriptions.cleanAirZone.directDebitEnabled}")
  boolean directDebitEnabled;

  @ApiModelProperty(value = "${swagger.model.descriptions.cleanAirZone.directDebitStartDateText}")
  String directDebitStartDateText;
}
