package uk.gov.caz.taxiregister.model;

import java.io.Serializable;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.taxiregister.tasks.ActiveLicencesInReportingWindowStarter;

/**
 * Represents an event related to licence and that event is meaningul in specified reporting window.
 * Objects of this class are produced by reporting query that lists all active licences during
 * reporting window. See {@link ActiveLicencesInReportingWindowStarter} and {@link
 * ActiveLicencesCollator}.
 */
@Value
@Builder
public class ActiveLicenceInReportingWindow implements Serializable {

  private static final long serialVersionUID = 1840763261132996788L;

  /**
   * Status of the licence event as viewed from reporting window.
   */
  public enum StatusInReportingWindow {
    /**
     * Event happened before reporting window: maybe an insert or update.
     */
    EXISTING,
    /**
     * Insert happened within reporting window.
     */
    INSERT,
    /**
     * Update happened within reporting window.
     */
    UPDATE,
    /**
     * Delete happened within reporting window.
     */
    DELETE
  }

  /**
   * Raw reference to {@link LicenceEvent} that happened.
   */
  LicenceEvent licenceEvent;

  /**
   * Status of the licence event as viewed from reporting window.
   */
  StatusInReportingWindow statusInReportingWindow;
}