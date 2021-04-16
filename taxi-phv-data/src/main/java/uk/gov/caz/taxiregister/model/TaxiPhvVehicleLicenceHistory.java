package uk.gov.caz.taxiregister.model;

import java.io.Serializable;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

/**
 * Class that contains historical info of licence.
 */
@Value
@Builder
public class TaxiPhvVehicleLicenceHistory implements Serializable {

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
   * Name of the licence authority.
   */
  String licensingAuthorityName;

  /**
   * Licence plate number.
   */
  String licencePlateNumber;

  /**
   * Licence start date.
   */
  LocalDate licenceStartDate;

  /**
   * Licence end date.
   */
  LocalDate licenceEndDate;

  /**
   * Wheelchair accessible.
   */
  Boolean wheelchairAccessible;
}