package uk.gov.caz.taxiregister.service;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.caz.testutils.NtrAssertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.gov.caz.taxiregister.model.ActiveLicenceInReportingWindow;
import uk.gov.caz.taxiregister.model.ActiveLicenceInReportingWindow.StatusInReportingWindow;
import uk.gov.caz.taxiregister.model.LicenceEvent;

class ActiveLicencesCollatorTest {

  private static final int LA_LONDON = 1;
  private static final int LA_BATH = 2;
  private static final String BMW_VRM = "BMW";
  private static final String AUDI_VRM = "AUDI";
  private static final String INSERT = "I";
  private static final String UPDATE = "U";
  private static final String DELETE = "D";
  private static final LocalDate _1_JANUARY = LocalDate.of(2019, 1, 1);
  private static final LocalDateTime _1_JANUARY_EVENT = LocalDateTime.of(2019, 1, 1, 5, 45);
  private static final LocalDateTime _1_FEBRUARY_EVENT = LocalDateTime.of(2019, 2, 1, 5, 45);
  private static final LocalDate _1_MARCH = LocalDate.of(2019, 3, 1);
  private static final LocalDateTime _1_MARCH_EVENT = LocalDateTime.of(2019, 3, 1, 5, 45);
  private static final LocalDateTime _21_MARCH_EVENT = LocalDateTime.of(2019, 3, 22, 5, 45);
  private static final LocalDateTime _1_APRIL_EVENT = LocalDateTime.of(2019, 4, 1, 5, 45);
  private static final LocalDate _1_APRIL = LocalDate.of(2019, 4, 1);
  private static final LocalDateTime _2_APRIL_EVENT = LocalDateTime.of(2019, 4, 2, 5, 45);
  private static final LocalDateTime _3_APRIL_EVENT = LocalDateTime.of(2019, 4, 3, 5, 45);
  private static final LocalDate _15_APRIL = LocalDate.of(2019, 4, 15);
  private static final LocalDateTime _15_APRIL_EVENT = LocalDateTime.of(2019, 4, 15, 8, 44);
  private static final LocalDate _30_APRIL = LocalDate.of(2019, 4, 30);
  private static final LocalDate _1_MAY = LocalDate.of(2019, 5, 1);
  private static final LocalDateTime _10_MAY_EVENT = LocalDateTime.of(2019, 5, 10, 8, 44);
  private static final LocalDate _22_MAY = LocalDate.of(2019, 5, 22);
  private static final LocalDateTime _20_MAY_EVENT = LocalDateTime.of(2019, 5, 20, 8, 44);
  private static final LocalDateTime _22_MAY_EVENT = LocalDateTime.of(2019, 5, 22, 8, 44);
  private static final LocalDate _3_JUNE = LocalDate.of(2019, 6, 3);
  private static final LocalDateTime _1_JUNE_EVENT = LocalDateTime.of(2019, 6, 1, 18, 6);
  private static final LocalDateTime _3_JUNE_EVENT = LocalDateTime.of(2019, 6, 3, 18, 6);
  private static final LocalDate _10_JUNE = LocalDate.of(2019, 6, 10);
  private static final LocalDateTime _10_JUNE_EVENT = LocalDateTime.of(2019, 6, 10, 9, 5);
  private static final LocalDate _16_JUNE = LocalDate.of(2019, 6, 16);
  private static final LocalDateTime _16_JUNE_EVENT = LocalDateTime.of(2019, 6, 16, 9, 5);
  private static final LocalDateTime _17_JUNE_EVENT = LocalDateTime.of(2019, 6, 17, 9, 5);
  private static final LocalDateTime _18_JUNE_EVENT = LocalDateTime.of(2019, 6, 18, 9, 5);
  private static final LocalDateTime _20_JUNE_EVENT = LocalDateTime.of(2019, 6, 20, 9, 5);
  private static final LocalDateTime _22_JUNE_EVENT = LocalDateTime.of(2019, 6, 22, 9, 5);
  private static final LocalDate _1_JULY = LocalDate.of(2019, 7, 1);
  private static final LocalDate _3_JULY = LocalDate.of(2019, 7, 3);
  private static final LocalDateTime _20_AUGUST_EVENT = LocalDateTime.of(2019, 8, 20, 8, 44);
  private static final LocalDate _1_SEPTEMBER = LocalDate.of(2019, 9, 1);
  private static final LocalDateTime _1_SEPTEMBER_EVENT = LocalDateTime.of(2019, 9, 1, 8, 44);
  private static final LocalDate _3_OCTOBER = LocalDate.of(2019, 10, 3);
  private static final LocalDate _30_NOVEMBER = LocalDate.of(2019, 11, 30);

  @Test
  public void testDifferentEventStreamScenarios() {
    // given
    ActiveLicencesCollator collator = new ActiveLicencesCollator();

    // when
    List<ActiveLicenceInReportingWindow> licenceEvents = collator
        .collate(prepareLicenceEvents(), _1_MAY, _1_JULY);

    // then
    // Expected events in time order:
    // 1.  EXISTING AUDI in London on 01.01 with licence active range: 01.01 - 03.06
    // 2.  EXISTING AUDI in London on 01.04 with licence active range: 01.05 - 01.05
    // 3.  EXISTING BMW in Bath on 03.04 with licence active range: 01.05 - 03.07 (wheelchair false)
    // 4.  INSERT of BMW in Bath on 22.05 with licence active range: 22.05 - 01.09
    // 5.  UPDATE of BMW in Bath on 01.06 with licence active range: 01.05 - 03.07 (wheelchair true)
    // 6.  DELETE of AUDI in London on 03.06 with licence active range: 01.01 - 03.06
    // 7.  INSERT of BMW in London on 10.06 with licence active range: 10.06 - 30.09
    // 8.  INSERT of AUDI in Bath on 16.06 with licence active range: 16.06 - 03.10
    // 9.  UPDATE of AUDI in London on 17.06 with licence active range: 01.05 - 01.05
    // 10  DELETE of AUDI in Bath on 18.06 with licence active range: 16.06 - 03.10
    // 11. INSERT of AUDI in Bath on 20.06 with licence active range: 16.06 - 03.10
    // 12. UPDATE of BMW in London on 22.06 to wheelchair false with licence active range: 10.06 - 30.11
    assertThat(licenceEvents).hasSize(12);

    assertThat(licenceEvents.get(0))
        .hasStatus(StatusInReportingWindow.EXISTING)
        .isFor(AUDI_VRM)
        .in(LA_LONDON)
        .happenedOn(_1_JANUARY_EVENT)
        .withLicenceStartDate(_1_JANUARY)
        .withLicenceEndDate(_3_JUNE);

    assertThat(licenceEvents.get(1))
        .hasStatus(StatusInReportingWindow.EXISTING)
        .isFor(AUDI_VRM)
        .in(LA_LONDON)
        .happenedOn(_1_APRIL_EVENT)
        .withLicenceStartDate(_1_MAY)
        .withLicenceEndDate(_1_MAY);

    assertThat(licenceEvents.get(2))
        .hasStatus(StatusInReportingWindow.EXISTING)
        .isFor(BMW_VRM)
        .in(LA_BATH)
        .happenedOn(_3_APRIL_EVENT)
        .withLicenceStartDate(_1_MAY)
        .withLicenceEndDate(_3_JULY);

    assertThat(licenceEvents.get(3))
        .hasStatus(StatusInReportingWindow.INSERT)
        .isFor(BMW_VRM)
        .in(LA_BATH)
        .happenedOn(_22_MAY_EVENT)
        .withLicenceStartDate(_22_MAY)
        .withLicenceEndDate(_1_SEPTEMBER);

    assertThat(licenceEvents.get(4))
        .hasStatus(StatusInReportingWindow.UPDATE)
        .isFor(BMW_VRM)
        .in(LA_BATH)
        .happenedOn(_1_JUNE_EVENT)
        .withLicenceStartDate(_1_MAY)
        .withLicenceEndDate(_3_JULY);

    assertThat(licenceEvents.get(5))
        .hasStatus(StatusInReportingWindow.DELETE)
        .isFor(AUDI_VRM)
        .in(LA_LONDON)
        .happenedOn(_3_JUNE_EVENT)
        .withLicenceStartDate(_1_JANUARY)
        .withLicenceEndDate(_3_JUNE);

    assertThat(licenceEvents.get(6))
        .hasStatus(StatusInReportingWindow.INSERT)
        .isFor(BMW_VRM)
        .in(LA_LONDON)
        .happenedOn(_10_JUNE_EVENT)
        .withLicenceStartDate(_10_JUNE)
        .withLicenceEndDate(_30_NOVEMBER);

    assertThat(licenceEvents.get(7))
        .hasStatus(StatusInReportingWindow.INSERT)
        .isFor(AUDI_VRM)
        .in(LA_BATH)
        .happenedOn(_16_JUNE_EVENT)
        .withLicenceStartDate(_16_JUNE)
        .withLicenceEndDate(_3_OCTOBER);

    assertThat(licenceEvents.get(8))
        .hasStatus(StatusInReportingWindow.UPDATE)
        .isFor(AUDI_VRM)
        .in(LA_LONDON)
        .happenedOn(_17_JUNE_EVENT)
        .withLicenceStartDate(_1_MAY)
        .withLicenceEndDate(_1_MAY);

    assertThat(licenceEvents.get(9))
        .hasStatus(StatusInReportingWindow.DELETE)
        .isFor(AUDI_VRM)
        .in(LA_BATH)
        .happenedOn(_18_JUNE_EVENT)
        .withLicenceStartDate(_16_JUNE)
        .withLicenceEndDate(_3_OCTOBER);

    assertThat(licenceEvents.get(10))
        .hasStatus(StatusInReportingWindow.INSERT)
        .isFor(AUDI_VRM)
        .in(LA_BATH)
        .happenedOn(_20_JUNE_EVENT)
        .withLicenceStartDate(_16_JUNE)
        .withLicenceEndDate(_3_OCTOBER);

    assertThat(licenceEvents.get(11))
        .hasStatus(StatusInReportingWindow.UPDATE)
        .isFor(BMW_VRM)
        .in(LA_LONDON)
        .happenedOn(_22_JUNE_EVENT)
        .withLicenceStartDate(_10_JUNE)
        .withLicenceEndDate(_30_NOVEMBER);
  }

  private List<LicenceEvent> prepareLicenceEvents() {
    List<LicenceEvent> events = newArrayList();

    // See each test case code for details regarding what it does
    testCase_1(events);
    testCase_2(events);
    testCase_3(events);
    testCase_4(events);
    testCase_5(events);
    testCase_6(events);
    testCase_7(events);
    testCase_8(events);
    testCase_9(events);
    testCase_10(events);

    events.sort(Comparator.comparing(LicenceEvent::getEventTimestamp));
    return events;
  }

  //
  // Legend:
  // (I) - Insert operation happened
  // (U) - Update operation happened
  // (D) - Delete operation happened
  // ====  - Licence was present in the database at this time
  // ~~~~~ - Licence was active at this time
  // The Box - reporting window
  //

  private void testCase_1(List<LicenceEvent> events) {
    //                  ______________
    //                 |              |
    // (I)========(D)  |              |
    //  ~~~~~~~~~~~~   |______________|
    //
    // BMW in London
    // Expected output:
    //       0 events in reporting window
    LicenceEvent licenceEvent = LicenceEvent.builder()
        .vrm(BMW_VRM)
        .licensingAuthorityId(LA_LONDON)
        .description("BMW in London that was deleted before reporting window")
        .wheelchairAccessible(false)
        .licencePlateNumber(BMW_VRM)
        .action(INSERT)
        .eventTimestamp(_2_APRIL_EVENT)
        .licenceStartDate(_1_APRIL)
        .licenceEndDate(_15_APRIL)
        .build();
    events.add(licenceEvent);

    licenceEvent = licenceEvent.toBuilder()
        .action(DELETE)
        .eventTimestamp(_15_APRIL_EVENT)
        .build();
    events.add(licenceEvent);
  }

  private void testCase_2(List<LicenceEvent> events) {
    //          ______________
    //         |              |
    //         |   (I)========|========(D)
    //         |   ~~~~~~~~~~~|~~~~~~~~~~
    //         |______________|
    //
    // BMW in Bath
    // Expected output:
    //       1 INSERT event in reporting window on 22.05
    LicenceEvent licenceEvent = LicenceEvent.builder()
        .vrm(BMW_VRM)
        .licensingAuthorityId(LA_BATH)
        .description(
            "BMW in Bath that was inserted within reporting window and deleted much later")
        .wheelchairAccessible(true)
        .licencePlateNumber(BMW_VRM)
        .action(INSERT)
        .eventTimestamp(_22_MAY_EVENT)
        .licenceStartDate(_22_MAY)
        .licenceEndDate(_1_SEPTEMBER)
        .build();
    events.add(licenceEvent);

    licenceEvent = licenceEvent.toBuilder()
        .action(DELETE)
        .eventTimestamp(_1_SEPTEMBER_EVENT)
        .build();
    events.add(licenceEvent);
  }

  private void testCase_3(List<LicenceEvent> events) {
    //          ______________
    //         |              |
    //         |   (I)===(U)==|=======================
    //         |   ~~~~~~~~~~~|~~~~~~~~~~~~~
    //         |______________|
    //
    // BMW in London
    // Expected output:
    //       1 INSERT event on 10.06
    //       1 UPDATE event with wheelchair accessible true on 22.06
    LicenceEvent licenceEvent = LicenceEvent.builder()
        .vrm(BMW_VRM)
        .licensingAuthorityId(LA_LONDON)
        .description("BMW in London that was inserted and updated in reporting window")
        .wheelchairAccessible(false)
        .licencePlateNumber(BMW_VRM)
        .action(INSERT)
        .eventTimestamp(_10_JUNE_EVENT)
        .licenceStartDate(_10_JUNE)
        .licenceEndDate(_30_NOVEMBER)
        .build();
    events.add(licenceEvent);

    licenceEvent = licenceEvent.toBuilder()
        .wheelchairAccessible(true) // wheelchair accessible changed
        .action(UPDATE)
        .eventTimestamp(_22_JUNE_EVENT)
        .build();
    events.add(licenceEvent);
  }

  private void testCase_4(List<LicenceEvent> events) {
    //          ______________
    //         |              |
    //         |   (I)========|=======================
    // ~~~~~~~~|              |
    //         |______________|
    //
    // Audi in London
    // Expected output:
    //       0 events in reporting window
    LicenceEvent licenceEvent = LicenceEvent.builder()
        .vrm(AUDI_VRM)
        .licensingAuthorityId(LA_LONDON)
        .description(
            "AUDI in London which is inserted in DB during reporting window but licence is not active in this window")
        .wheelchairAccessible(false)
        .licencePlateNumber(AUDI_VRM)
        .action(INSERT)
        .eventTimestamp(_10_MAY_EVENT)
        .licenceStartDate(_1_MARCH)
        .licenceEndDate(_30_APRIL)
        .build();
    events.add(licenceEvent);
  }

  private void testCase_5(List<LicenceEvent> events) {
    //                ______________
    //               |              |
    // (I)===========|===(U)========|=====(D)
    //       ~~~~~~~~|              |
    //               |______________|
    //
    // Audi in London
    // Expected output:
    //       0 events in reporting window
    LicenceEvent licenceEvent = LicenceEvent.builder()
        .vrm(AUDI_VRM)
        .licensingAuthorityId(LA_LONDON)
        .description(
            "AUDI in London which is present in DB during reporting window but licence is not active in this window")
        .wheelchairAccessible(false)
        .licencePlateNumber(AUDI_VRM)
        .action(INSERT)
        .eventTimestamp(_1_JANUARY_EVENT)
        .licenceStartDate(_1_MARCH)
        .licenceEndDate(_30_APRIL)
        .build();
    events.add(licenceEvent);

    licenceEvent = licenceEvent.toBuilder()
        .action(UPDATE)
        // wheelchair accessible was updated, but licence was not active in reporting window
        .wheelchairAccessible(true)
        .eventTimestamp(_20_MAY_EVENT)
        .build();
    events.add(licenceEvent);

    licenceEvent = licenceEvent.toBuilder()
        .action(DELETE)
        .eventTimestamp(_20_AUGUST_EVENT)
        .build();
    events.add(licenceEvent);
  }

  private void testCase_6(List<LicenceEvent> events) {
    //                ______________
    //               |              |
    // (I)===========|======(D)     |
    // ~~~~~~~~~~~~~~|~~~~~~~~      |
    //               |______________|
    //
    // Audi in London
    // Expected output:
    //       1 EXISTING event (because licence was active and present before reporting window)
    //       1 DELETE event (during reporting window)
    LicenceEvent licenceEvent = LicenceEvent.builder()
        .vrm(AUDI_VRM)
        .licensingAuthorityId(LA_LONDON)
        .description(
            "AUDI in London which is present in DB during reporting window and deleted inside window")
        .wheelchairAccessible(false)
        .licencePlateNumber(AUDI_VRM)
        .action(INSERT)
        .eventTimestamp(_1_JANUARY_EVENT)
        .licenceStartDate(_1_JANUARY)
        .licenceEndDate(_3_JUNE)
        .build();
    events.add(licenceEvent);

    licenceEvent = licenceEvent.toBuilder()
        .action(DELETE)
        .eventTimestamp(_3_JUNE_EVENT)
        .build();
    events.add(licenceEvent);
  }

  private void testCase_7(List<LicenceEvent> events) {
    //          __________________
    //         |                  |
    //         |    (I)=(D) (I)===|=====================
    //         |     ~~~~~~~~~~~~~|~~~~~~~
    //         |__________________|
    //
    // Audi in Bath
    // Expected output:
    //       1 INSERT event
    //       1 DELETE event
    //       1 INSERT event
    LicenceEvent licenceEvent = LicenceEvent.builder()
        .vrm(AUDI_VRM)
        .licensingAuthorityId(LA_BATH)
        .description(
            "AUDI in Bath which is inserted, deleted and again inserted in DB during reporting windoww")
        .wheelchairAccessible(false)
        .licencePlateNumber(AUDI_VRM)
        .action(INSERT)
        .eventTimestamp(_16_JUNE_EVENT)
        .licenceStartDate(_16_JUNE)
        .licenceEndDate(_3_OCTOBER)
        .build();
    events.add(licenceEvent);

    licenceEvent = licenceEvent.toBuilder()
        .action(DELETE)
        .eventTimestamp(_18_JUNE_EVENT)
        .build();
    events.add(licenceEvent);

    licenceEvent = licenceEvent.toBuilder()
        .action(INSERT)
        .eventTimestamp(_20_JUNE_EVENT)
        .build();
    events.add(licenceEvent);
  }

  private void testCase_8(List<LicenceEvent> events) {
    //               __________________
    //              |                  |
    //  (I)====(D)  |                  |
    //              |~~~~~~~~~~~~~~~~~~|~
    //              |__________________|
    //
    // Audi in Bath
    // Expected output:
    //       0 events - licence was active during reporting window, but not was present in DB in this time
    LicenceEvent licenceEvent = LicenceEvent.builder()
        .vrm(AUDI_VRM)
        .licensingAuthorityId(LA_BATH)
        .description(
            "AUDI in Bath which is inserted and deleted before reporting window")
        .wheelchairAccessible(false)
        .licencePlateNumber(AUDI_VRM)
        .action(INSERT)
        .eventTimestamp(_1_FEBRUARY_EVENT)
        .licenceStartDate(_1_MAY)
        .licenceEndDate(_3_JULY)
        .build();
    events.add(licenceEvent);

    licenceEvent = licenceEvent.toBuilder()
        .action(DELETE)
        .eventTimestamp(_21_MARCH_EVENT)
        .build();
    events.add(licenceEvent);
  }

  private void testCase_9(List<LicenceEvent> events) {
    //                     ______________
    //                    |              |
    // (I)===(U)===(U)====|======(U)     |
    //                    |~~~~~~~~~~~~~~|~
    //                    |______________|
    //
    // Audi in London
    // Expected output:
    //       1 EXISTING event (because licence was active and present before reporting window).
    //            Wheelchair accessible should be false because of the latest UPDATE.
    //       1 UPDATE event (during reporting window)
    //            Wheelchair accessible should be true
    LicenceEvent licenceEvent = LicenceEvent.builder()
        .vrm(BMW_VRM)
        .licensingAuthorityId(LA_BATH)
        .description(
            "BMW in Bath which is inserted and updated before reporting window and updated during")
        .wheelchairAccessible(false)
        .licencePlateNumber(BMW_VRM)
        .action(INSERT)
        .eventTimestamp(_1_FEBRUARY_EVENT)
        .licenceStartDate(_1_MAY)
        .licenceEndDate(_3_JULY)
        .build();
    events.add(licenceEvent);

    licenceEvent = licenceEvent.toBuilder()
        .action(UPDATE)
        .wheelchairAccessible(true)
        .eventTimestamp(_1_MARCH_EVENT)
        .build();
    events.add(licenceEvent);

    licenceEvent = licenceEvent.toBuilder()
        .action(UPDATE)
        .wheelchairAccessible(false)
        .eventTimestamp(_3_APRIL_EVENT)
        .build();
    events.add(licenceEvent);

    licenceEvent = licenceEvent.toBuilder()
        .action(UPDATE)
        .wheelchairAccessible(true)
        .eventTimestamp(_1_JUNE_EVENT)
        .build();
    events.add(licenceEvent);
  }

  private void testCase_10(List<LicenceEvent> events) {
    //                     ______________
    //                    |              |
    //             (I)====|======(U)     |
    //                    ~              |
    //                    |______________|
    //
    // Audi in London
    // Expected output:
    //       1 EXISTING event (because licence was active and present before reporting window).
    //       1 UPDATE event (during reporting window)
    LicenceEvent licenceEvent = LicenceEvent.builder()
        .vrm(AUDI_VRM)
        .licensingAuthorityId(LA_LONDON)
        .description(
            "AUDI in London that has active licence for only 1 day at the beginnig of reporting window")
        .wheelchairAccessible(false)
        .licencePlateNumber(AUDI_VRM)
        .action(INSERT)
        .eventTimestamp(_1_APRIL_EVENT)
        .licenceStartDate(_1_MAY)
        .licenceEndDate(_1_MAY)
        .build();
    events.add(licenceEvent);

    licenceEvent = licenceEvent.toBuilder()
        .action(UPDATE)
        .wheelchairAccessible(true)
        .eventTimestamp(_17_JUNE_EVENT)
        .build();
    events.add(licenceEvent);
  }
}