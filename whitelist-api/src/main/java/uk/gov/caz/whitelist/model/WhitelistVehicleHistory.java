package uk.gov.caz.whitelist.model;

import java.io.Serializable;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

/**
 * Class that contains historical info of licence.
 */
@Value
@Builder
public class WhitelistVehicleHistory implements Serializable {

  private static final long serialVersionUID = -1697344242305227788L;

  /**
   * Date when licence was modified.
   */
  LocalDate modifyDate;

  /**
   * Action on licence, ie. D - removed, I - added, U - edited.
   */
  String action;

  /**
   * Reason which describes why record was created.
   */
  String reasonUpdated;

  /**
   * A company, which build vehicle, i.e. Fiat, Audi
   */
  String manufacturer;

  /**
   * A category of vehicle.
   */
  String category;

  /**
   * Id of person who update record.
   */
  String modifierId;

  /**
   * Email of person who update record.
   */
  String modifierEmail;
}