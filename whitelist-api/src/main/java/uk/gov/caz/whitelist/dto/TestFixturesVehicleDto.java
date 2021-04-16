package uk.gov.caz.whitelist.dto;

import lombok.Value;

/**
 * Request object of test fixtures vehicle.
 */
@Value
public class TestFixturesVehicleDto {

  /**
   * String containing vehicle registration number.
   */
  String vrn;

  /**
   * String containing category.
   */
  String category;

  /**
   * Reason given in json file.
   */
  String reasonUpdated;

  /**
   * String containing a company, which build vehicle.
   */
  String manufacturer;

  /**
   * String containing action.
   */
  String action;
}