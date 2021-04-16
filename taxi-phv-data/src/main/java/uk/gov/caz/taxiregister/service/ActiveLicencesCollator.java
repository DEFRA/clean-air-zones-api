package uk.gov.caz.taxiregister.service;

import static java.util.stream.Collectors.toList;

import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.ObjectStack;
import com.carrotsearch.hppc.cursors.ObjectCursor;
import com.google.common.collect.Streams;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.taxiregister.model.ActiveLicenceInReportingWindow;
import uk.gov.caz.taxiregister.model.ActiveLicenceInReportingWindow.StatusInReportingWindow;
import uk.gov.caz.taxiregister.model.LicenceEvent;

/**
 * Service that traverses event store of licences and collates events that are meaningful in
 * specified reporting time window. This includes checking if licence was active during reporting
 * window or if it was present or not before.
 */
@Service
@Slf4j
public class ActiveLicencesCollator {

  /**
   * Given list of events related to licences, sorted by date, from the oldest to the newest,
   * collates and maps them into events meaningful in specified reporting time window.
   *
   * @param licenceEventsSortedByDate List of events related to licences, sorted by date, from
   *     the oldest to the newest.
   * @param reportingWindowStartDate Reporting window start date.
   * @param reportingWindowEndDate Reporting window end date.
   * @return List of {@link ActiveLicenceInReportingWindow} events that are meaningful in reporting
   *     time window.
   */
  public List<ActiveLicenceInReportingWindow> collate(List<LicenceEvent> licenceEventsSortedByDate,
      LocalDate reportingWindowStartDate, LocalDate reportingWindowEndDate) {
    //
    // WARNING!
    // Input List<LicenceEvent> MUST be sorted by event timestamp (when the event happened) from
    // the oldest to the newest. Algorithm here absolutely requires this sort and will return
    // incorrect results otherwise!
    //

    log.info("Starting collating licence events.");
    ActiveLicencesInReportingWindowStack activeLicencesStack =
        new ActiveLicencesInReportingWindowStack();
    for (LicenceEvent licenceEvent : licenceEventsSortedByDate) {
      if (eventHappenedAfterReportingWindow(reportingWindowEndDate, licenceEvent)) {
        break;
      }

      if (eventHappenedBeforeReportingWindow(reportingWindowStartDate, licenceEvent)) {
        processBefore(reportingWindowStartDate, reportingWindowEndDate, activeLicencesStack,
            licenceEvent);
        continue;
      }

      // As events that happened before reporting window have already been handled, at this moment
      // we are dealing with events that happened within the reporting window. We only process
      // licences that were at least partially active within the window. Such events can be one
      // of three types: Inserts, Updates or Deletes.
      if (licenceEvent.activeLicenceOverlapsWindow(reportingWindowStartDate,
          reportingWindowEndDate)) {
        activeLicencesStack.pushAsInsertUpdateOrDelete(licenceEvent);
      }
    }
    List<ActiveLicenceInReportingWindow> activeLicencesInReportingWindow =
        flattenActiveLicencesInReportingWindowAndSortThemFromOldestToNewest(activeLicencesStack);
    log.info("Finishing collating licence events. Found {} matching events.",
        activeLicencesInReportingWindow.size());
    return activeLicencesInReportingWindow;
  }

  /**
   * Checks whether licence event happened after reporting window start date.
   */
  private boolean eventHappenedAfterReportingWindow(LocalDate reportingWindowEndDate,
      LicenceEvent licenceEvent) {
    if (licenceEvent.happenedAfter(theEndOfTheDay(reportingWindowEndDate))) {
      log.info(
          "Got first event that happened after the end of reporting window. Breaking processing.");
      // Break processing on first occurrence of event that happened after the end of the reporting
      // window. All following events must be even further in the future and they are not
      // relevant to our report.
      return true;
    }
    return false;
  }

  /**
   * Checks whether licence event happened before reporting window start date.
   */
  private boolean eventHappenedBeforeReportingWindow(LocalDate reportingWindowStartDate,
      LicenceEvent licenceEvent) {
    return licenceEvent.happenedBefore(theStartOfTheDay(reportingWindowStartDate));
  }

  /**
   * Processes licence event that happened before reporting window. Such an event can be finally
   * marked as "EXISTING" or be a DELETE operation which means that licence was not in the database
   * and this event should not be present in final report.
   */
  private void processBefore(LocalDate reportingWindowStartDate, LocalDate reportingWindowEndDate,
      ActiveLicencesInReportingWindowStack activeLicencesStack, LicenceEvent licenceEvent) {
    // If event happened before reporting window start date then there are 2 possibilities:
    // 1. If it was a licence delete operation then we are sure that this licence is not present in
    //    the database within reporting window. So we remove it completely from the stack.
    // 2. If it was licence insert or update AND licence is active (can just barely overlap) during
    //    reporting window, then we push it to the stack as "EXISTING" because from reporting point
    //    of view this licence was already present in the database during reporting window.
    // If licence is not active (not overlapping) in reporting window we don't add it to the stack
    // because it is not required by the reporting requirements.
    if (licenceEvent.isDelete()) {
      activeLicencesStack.clearForLicence(licenceEvent);
    } else if (licenceEvent
        .activeLicenceOverlapsWindow(reportingWindowStartDate, reportingWindowEndDate)) {
      activeLicencesStack.pushAsExisting(licenceEvent);
    }
  }

  /**
   * Maps {@link LocalDate} to {@link LocalDateTime} at the beginning of the day.
   */
  private LocalDateTime theStartOfTheDay(LocalDate justADate) {
    return justADate.atStartOfDay();
  }

  /**
   * Maps {@link LocalDate} to {@link LocalDateTime} at the end of the day.
   */
  private LocalDateTime theEndOfTheDay(LocalDate justADate) {
    return justADate.atTime(LocalTime.MAX);
  }

  /**
   * This method takes {@link IntObjectHashMap} from {@link ActiveLicencesInReportingWindowStack}
   * and flattens its values extracting final {@link ActiveLicenceInReportingWindow} objects. Then
   * sorts them from oldest to newest and returns final-flat list which can be used for outputting
   * reporting data in any format fit.
   */
  private List<ActiveLicenceInReportingWindow>
      flattenActiveLicencesInReportingWindowAndSortThemFromOldestToNewest(
      ActiveLicencesInReportingWindowStack activeLicencesStack) {
    return Streams
        .stream(values(activeLicencesStack))
        .flatMap(toStreamOfObjectCursorsOverActiveLicencesInReportingWindow())
        .map(extractActiveLicenceInReportingWindow())
        .sorted(sortByEventTimestamp())
        .collect(toList());
  }

  /**
   * Extracts values from {@link IntObjectHashMap} as consumable by Java Streams.
   */
  private Iterator<ObjectCursor<ObjectStack<ActiveLicenceInReportingWindow>>> values(
      ActiveLicencesInReportingWindowStack activeLicencesStack) {
    return activeLicencesStack.matchingEvents.values().iterator();
  }

  /**
   * Helper function to extract flat Stream of {@link ActiveLicenceInReportingWindow} objects.
   */
  private Function<ObjectCursor<ObjectStack<ActiveLicenceInReportingWindow>>,
      Stream<? extends ObjectCursor<ActiveLicenceInReportingWindow>>>
      toStreamOfObjectCursorsOverActiveLicencesInReportingWindow() {
    return objectStackCursor -> Streams.stream(objectStackCursor.value.iterator());
  }

  /**
   * Extract single instance of {@link ActiveLicenceInReportingWindow} object.
   */
  private Function<ObjectCursor<ActiveLicenceInReportingWindow>, ActiveLicenceInReportingWindow>
      extractActiveLicenceInReportingWindow() {
    return objectCursor -> objectCursor.value;
  }

  /**
   * Creates a comparator sorting events by time from the oldest to the newest.
   */
  private Comparator<ActiveLicenceInReportingWindow> sortByEventTimestamp() {
    return Comparator.comparing(o -> o.getLicenceEvent().getEventTimestamp());
  }

  /**
   * Abstracts map of licences to Stack of events that are meaningful in reporting time window.
   */
  private static final class ActiveLicencesInReportingWindowStack {

    private static final int INITIAL_EXPECTED_COUNT_OF_EVENTS_FOR_LICENCE = 8;
    private static final int INITIAL_EXPECTED_COUNT_OF_LICENCES = 1_000_000;

    /**
     * Map of licences identified by hash of unique attributes to Stack of events that are
     * meaningful in reporting time window.
     */
    private final IntObjectHashMap<ObjectStack<ActiveLicenceInReportingWindow>> matchingEvents =
        new IntObjectHashMap<>(INITIAL_EXPECTED_COUNT_OF_LICENCES);

    /**
     * Clears stack of events for specified licence.
     *
     * @param licenceEvent Licence for which stack of events should be cleared.
     */
    public void clearForLicence(LicenceEvent licenceEvent) {
      matchingEvents.remove(licenceEvent.hashOfUniqueAttributes());
    }

    /**
     * Transforms and pushes {@link LicenceEvent} as meaningful licence event of type "EXISTING". It
     * means that licence was present before reporting window and was either inserted or updated.
     *
     * @param licenceEvent Licence event which should be pushed to the stack.
     */
    public void pushAsExisting(LicenceEvent licenceEvent) {
      clearForLicence(licenceEvent);
      ActiveLicenceInReportingWindow activeLicenceInReportingWindow =
          buildActiveLicenceInReportingWindow(licenceEvent, StatusInReportingWindow.EXISTING);
      ObjectStack<ActiveLicenceInReportingWindow> eventStack = new ObjectStack<>(
          INITIAL_EXPECTED_COUNT_OF_EVENTS_FOR_LICENCE);
      eventStack.push(activeLicenceInReportingWindow);
      matchingEvents.put(licenceEvent.hashOfUniqueAttributes(), eventStack);
    }

    /**
     * Transforms and pushes {@link LicenceEvent} as meaningful licence event of type "INSERT" or
     * "UPDATE" or "DELETE" depending on 'action' attribute of the event. It means that licence was
     * inserted/updated/deleted during reporting window.
     *
     * @param licenceEvent Licence event which should be pushed to the stack.
     */
    public void pushAsInsertUpdateOrDelete(LicenceEvent licenceEvent) {
      ActiveLicenceInReportingWindow activeLicenceInReportingWindow =
          buildActiveLicenceInReportingWindow(licenceEvent,
              toStatusInReportingWindow(licenceEvent));

      ObjectStack<ActiveLicenceInReportingWindow> eventStack = matchingEvents
          .getOrDefault(licenceEvent.hashOfUniqueAttributes(),
              new ObjectStack<>(INITIAL_EXPECTED_COUNT_OF_EVENTS_FOR_LICENCE));
      eventStack.push(activeLicenceInReportingWindow);
      matchingEvents.put(licenceEvent.hashOfUniqueAttributes(), eventStack);
    }

    /**
     * Constructs {@link ActiveLicenceInReportingWindow} instance from {@link LicenceEvent} object.
     */
    private ActiveLicenceInReportingWindow buildActiveLicenceInReportingWindow(
        LicenceEvent licenceEvent,
        StatusInReportingWindow statusInReportingWindow) {
      return ActiveLicenceInReportingWindow
          .builder()
          .licenceEvent(licenceEvent)
          .statusInReportingWindow(statusInReportingWindow)
          .build();
    }

    /**
     * Simple mapper of event action "I"/"U"/"D" to {@link StatusInReportingWindow} enum.
     */
    private StatusInReportingWindow toStatusInReportingWindow(LicenceEvent licenceEvent) {
      if (licenceEvent.isInsert()) {
        return StatusInReportingWindow.INSERT;
      }
      if (licenceEvent.isUpdate()) {
        return StatusInReportingWindow.UPDATE;
      }
      return StatusInReportingWindow.DELETE;
    }
  }
}
