package uk.gov.caz.whitelist.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class WhitelistVehicleCommand {

  String action;

  WhitelistVehicle whitelistVehicle;
}
