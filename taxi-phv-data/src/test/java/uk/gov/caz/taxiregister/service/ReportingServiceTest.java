package uk.gov.caz.taxiregister.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.taxiregister.DateHelper;
import uk.gov.caz.taxiregister.repository.ReportingRepository;
import uk.gov.caz.testutils.TestObjects;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {

  @Mock
  private ReportingRepository reportingRepository;

  @InjectMocks
  private ReportingService reportingService;

  @Nested
  class LicensingAuthoritiesOfActiveLicencesForVrmOn {
    @Test
    public void shouldThrowNullPointerExceptionIfVrmIsNull() {
      // given
      String vrm = null;

      // when
      Throwable throwable = catchThrowable(() -> reportingService
          .getLicensingAuthoritiesOfActiveLicencesForVrmOn(vrm, LocalDate.now()));

      // then
      assertThat(throwable)
          .isInstanceOf(NullPointerException.class)
          .hasMessage("Vrm cannot be null");
    }

    @ParameterizedTest
    @MethodSource("uk.gov.caz.taxiregister.service.ReportingServiceTest#futureDatesProvider")
    public void shouldThrowIllegalArgumentExceptionIfFutureDateIsPassed(LocalDate date) {
      // given
      String vrm = "some vrm";

      // when
      Throwable throwable = catchThrowable(() -> reportingService
          .getLicensingAuthoritiesOfActiveLicencesForVrmOn(vrm, date));

      // then
      assertThat(throwable)
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Cannot process a future date");
    }

    @Test
    public void shouldThrowNullPointerExceptionIfDateIsNull() {
      // given
      LocalDate date = null;

      // when
      Throwable throwable = catchThrowable(() -> reportingService
          .getLicensingAuthoritiesOfActiveLicencesForVrmOn("some vrm", date));

      // then
      assertThat(throwable)
          .isInstanceOf(NullPointerException.class)
          .hasMessage("Date cannot be null");
    }

    @Test
    public void shouldDelegateCallToReportingRepository() {
      // given
      LocalDate date = LocalDate.now();
      String vrm = "some vrm";
      Set<String> expected = Collections.singleton("la-1");
      BDDMockito.given(reportingRepository.getLicensingAuthoritiesOfActiveLicencesForVrmOn(vrm, date))
          .willReturn(expected);

      // when
      Set<String> actual = reportingService
          .getLicensingAuthoritiesOfActiveLicencesForVrmOn(vrm, date);

      // then
      assertThat(actual).containsExactlyElementsOf(expected);
      Mockito.verify(reportingRepository).getLicensingAuthoritiesOfActiveLicencesForVrmOn(vrm, date);
    }
  }

  @Nested
  class ActiveLicencesForLicensingAuthorityOn {
    @Test
    public void shouldThrowNullPointerExceptionIfDateIsNull() {
      // given
      LocalDate date = null;

      // when
      Throwable throwable = catchThrowable(() -> reportingService
          .getActiveLicencesForLicensingAuthorityOn(1, date));

      // then
      assertThat(throwable)
          .isInstanceOf(NullPointerException.class)
          .hasMessage("Date cannot be null");
    }

    @ParameterizedTest
    @MethodSource("uk.gov.caz.taxiregister.service.ReportingServiceTest#futureDatesProvider")
    public void shouldThrowIllegalArgumentExceptionIfFutureDateIsPassed(LocalDate date) {
      // given
      int licensingAuthorityId = 1;

      // when
      Throwable throwable = catchThrowable(() -> reportingService
          .getActiveLicencesForLicensingAuthorityOn(licensingAuthorityId, date));

      // then
      assertThat(throwable)
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Cannot process a future date");
    }

    @Test
    public void shouldDelegateCallToReportingRepository() {
      // given
      LocalDate date = LocalDate.now();
      int licensingAuthorityId = 1;
      Set<String> expected = Collections.singleton(TestObjects.Licences.validVrm());
      BDDMockito.given(reportingRepository.getActiveLicencesForLicensingAuthorityOn(licensingAuthorityId, date))
          .willReturn(expected);

      // when
      Set<String> actual = reportingService
          .getActiveLicencesForLicensingAuthorityOn(licensingAuthorityId, date);

      // then
      assertThat(actual).containsExactlyElementsOf(expected);
      Mockito.verify(reportingRepository).getActiveLicencesForLicensingAuthorityOn(licensingAuthorityId, date);
    }
  }

  private static Stream<LocalDate> futureDatesProvider() {
    return Stream.of(
        DateHelper.tomorrow(),
        DateHelper.nextWeek()
    );
  }

}