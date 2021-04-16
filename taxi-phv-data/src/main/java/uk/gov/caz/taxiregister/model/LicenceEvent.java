package uk.gov.caz.taxiregister.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Class that contains description of the event that happened to the licence.
 */
@Value
@Builder(toBuilder = true)
public class LicenceEvent implements Serializable {

  private static final long serialVersionUID = 7798458207961752597L;

  /**
   * Licence's Vehicle Registration Mark (Number).
   */
  String vrm;

  /**
   * Timestamp on which event happened.
   */
  LocalDateTime eventTimestamp;

  /**
   * Timestamp on which original row was created/inserted.
   */
  LocalDateTime insertTimestamp;

  /**
   * Action on licence, ie. D - removed, I - added, U - edited.
   */
  String action;

  /**
   * GUID identifying entity/person making a change.
   */
  UUID uploaderId;

  /**
   * ID of the licence authority.
   */
  int licensingAuthorityId;

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

  /**
   * Licence description.
   */
  String description;

  /**
   * Checks whether licence event happened after specified timestamp.
   *
   * @param timestamp Timestamp to check if event happened after.
   * @return true if event happened after specified timestamp.
   */
  public boolean happenedAfter(LocalDateTime timestamp) {
    return eventTimestamp.isAfter(timestamp);
  }

  /**
   * Checks whether licence event happened before specified timestamp.
   *
   * @param timestamp Timestamp to check if event happened before.
   * @return true if event happened before specified timestamp.
   */
  public boolean happenedBefore(LocalDateTime timestamp) {
    return eventTimestamp.isBefore(timestamp);
  }

  /**
   * Checks whether licence is active within two dates.
   *
   * @param start Start date.
   * @param end End date.
   * @return true if licence is active within start and end dates.
   */
  public boolean activeLicenceOverlapsWindow(LocalDate start, LocalDate end) {
    return licenceActivationIsBeforeOrEqual(end) && licenceDeactivationIsAfterOrEqual(start);
  }

  /**
   * Returns true if licence activation starts before or at end date.
   */
  private boolean licenceActivationIsBeforeOrEqual(LocalDate end) {
    return licenceStartDate.isEqual(end) || licenceStartDate.isBefore(end);
  }

  /**
   * Returns true if licence activation ends after or at specified start date.
   */
  private boolean licenceDeactivationIsAfterOrEqual(LocalDate start) {
    return licenceEndDate.isEqual(start) || licenceEndDate.isAfter(start);
  }

  /**
   * Return true if event is of INSERT type.
   */
  public boolean isInsert() {
    return "I".equalsIgnoreCase(action);
  }

  /**
   * Return true if event is of UPDATE type.
   */
  public boolean isUpdate() {
    return "U".equalsIgnoreCase(action);
  }

  /**
   * Return true if event is of DELETE type.
   */
  public boolean isDelete() {
    return "D".equalsIgnoreCase(action);
  }

  /**
   * Calculates hash value of attributes that uniquely identify licence. They are vrm + authorityId
   * + licencePlateNumber + licenceStartDate + licenceEndDate.
   */
  public int hashOfUniqueAttributes() {
    return Objects
        .hash(vrm, licensingAuthorityId, licencePlateNumber, licenceStartDate, licenceEndDate);
  }
}