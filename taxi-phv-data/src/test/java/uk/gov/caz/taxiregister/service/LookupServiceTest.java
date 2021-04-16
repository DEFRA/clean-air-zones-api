package uk.gov.caz.taxiregister.service;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.assertj.core.api.Condition;
import org.assertj.core.condition.Not;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.taxiregister.DateHelper;
import uk.gov.caz.taxiregister.model.LicenseDates;
import uk.gov.caz.taxiregister.model.LicensingAuthority;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicence;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicence.TaxiPhvVehicleLicenceBuilder;
import uk.gov.caz.taxiregister.model.VehicleLicenceLookupInfo;
import uk.gov.caz.taxiregister.repository.LicensingAuthorityPostgresRepository;
import uk.gov.caz.taxiregister.repository.TaxiPhvLicencePostgresRepository;

@ExtendWith(MockitoExtension.class)
class LookupServiceTest {

  private static final String ANY_VRM = "8839GF";

  private static final UUID ANY_UPLOADER_ID = UUID.randomUUID();

  private static final Condition<VehicleLicenceLookupInfo> activeLicenceCondition = new Condition<>(
      VehicleLicenceLookupInfo::hasAnyOperatingLicenceActive,
      "isActiveCondition"
  );

  private static final Condition<VehicleLicenceLookupInfo> inactiveLicenceCondition = Not.not(
      activeLicenceCondition
  );
  private static final Condition<VehicleLicenceLookupInfo> wheelchairAccessibleCondition = new Condition<>(
      vehicleLicenceLookupInfo -> Boolean.TRUE
          .equals(vehicleLicenceLookupInfo.getWheelchairAccessible()),
      "wheelchairAccessibleCondition"
  );

  private static final Condition<VehicleLicenceLookupInfo> wheelchairInaccessibleCondition = new Condition<>(
      vehicleLicenceLookupInfo -> Boolean.FALSE
          .equals(vehicleLicenceLookupInfo.getWheelchairAccessible()),
      "wheelchairInaccessibleCondition"
  );

  private static final Condition<VehicleLicenceLookupInfo> nullWheelchairAccessibleCondition = new Condition<>(
      vehicleLicenceLookupInfo -> Objects
          .isNull(vehicleLicenceLookupInfo.getWheelchairAccessible()),
      "nullWheelchairAccessibleCondition"
  );

  @Mock
  private TaxiPhvLicencePostgresRepository taxiPhvLicencePostgresRepository;

  @Mock
  private LicensingAuthorityPostgresRepository licensingAuthorityPostgresRepository;

  @InjectMocks
  private LookupService lookupService;

  @Test
  public void shouldReturnEmptyOptionalWhenThereIsNoVrmInDb() {
    // given
    String vrm = ANY_VRM;
    given(findVehicleLicencesBy(vrm)).willReturn(Collections.emptyList());

    // when
    Optional<VehicleLicenceLookupInfo> licenceInfo = lookupService.getLicenceInfoBy(vrm);

    // then
    then(licenceInfo).isEmpty();
  }

  @Nested
  class WithAtLeastOneActiveLicence {

    @Nested
    class WithAtLeastOneWheelchairAccessibleFlagSetToTrue {

      @ParameterizedTest
      @MethodSource("uk.gov.caz.taxiregister.service.LookupServiceTest#atLeastOneActiveLicenceProviderWithAtLeastOneAccessibleTrueFlag")
      public void shouldReturnTrue(List<TaxiPhvVehicleLicence> licences) {
        // given
        given(findVehicleLicencesBy(ANY_VRM)).willReturn(licences);
        given(findAuthorityNamesBy(ANY_VRM)).willReturn(sampleAuthirityNames());

        // when
        Optional<VehicleLicenceLookupInfo> lookupInfo = lookupService.getLicenceInfoBy(ANY_VRM);

        // then
        then(lookupInfo).hasValueSatisfying(activeLicenceCondition);
        then(lookupInfo).hasValueSatisfying(wheelchairAccessibleCondition);
        then(lookupInfo.get().getLicensingAuthoritiesNames()).hasSize(3);
      }
    }

    @Nested
    class WithoutWheelchairAccessibleFlagSetToTrue { // flags are null or false

      @ParameterizedTest
      @MethodSource("uk.gov.caz.taxiregister.service.LookupServiceTest#atLeastOneActiveLicenceProviderWithNullAccessibleFlag")
      public void shouldReturnNullWhenAllFlagsAreNull(List<TaxiPhvVehicleLicence> licences) {
        // given
        given(findVehicleLicencesBy(ANY_VRM)).willReturn(licences);

        // when
        Optional<VehicleLicenceLookupInfo> lookupInfo = lookupService.getLicenceInfoBy(ANY_VRM);

        // then
        then(lookupInfo).hasValueSatisfying(activeLicenceCondition);
        then(lookupInfo).hasValueSatisfying(nullWheelchairAccessibleCondition);
      }

      @ParameterizedTest
      @MethodSource("uk.gov.caz.taxiregister.service.LookupServiceTest#atLeastOneActiveLicenceProviderWithAtLeastOneAccessibleFalseFlag")
      public void shouldReturnFalseWhenExistsNonNullFlag(List<TaxiPhvVehicleLicence> licences) {
        // given
        given(findVehicleLicencesBy(ANY_VRM)).willReturn(licences);

        // when
        Optional<VehicleLicenceLookupInfo> lookupInfo = lookupService.getLicenceInfoBy(ANY_VRM);

        // then
        then(lookupInfo).hasValueSatisfying(activeLicenceCondition);
        then(lookupInfo).hasValueSatisfying(wheelchairInaccessibleCondition);
      }
    }
  }

  @Nested
  class WithoutActiveLicences {

    @Nested
    class WhenAllFlagsAreNull {

      @ParameterizedTest
      @MethodSource("uk.gov.caz.taxiregister.service.LookupServiceTest#inactiveLicenceProviderWithNullAccessibleFlag")
      public void shouldReturnNull(List<TaxiPhvVehicleLicence> licences) {
        // given
        given(findVehicleLicencesBy(ANY_VRM)).willReturn(licences);

        // when
        Optional<VehicleLicenceLookupInfo> lookupInfo = lookupService.getLicenceInfoBy(ANY_VRM);

        // then
        then(lookupInfo).hasValueSatisfying(inactiveLicenceCondition);
        then(lookupInfo).hasValueSatisfying(nullWheelchairAccessibleCondition);
      }
    }

    @Nested
    class WhenExistsNonnullFlags {

      @ParameterizedTest
      @MethodSource("uk.gov.caz.taxiregister.service.LookupServiceTest#inactiveLicenceProviderWithAtLeastOneAccessibleFalseFlag")
      public void shouldReturnFalse(List<TaxiPhvVehicleLicence> licences) {
        // given
        given(findVehicleLicencesBy(ANY_VRM)).willReturn(licences);

        // when
        Optional<VehicleLicenceLookupInfo> lookupInfo = lookupService.getLicenceInfoBy(ANY_VRM);

        // then
        then(lookupInfo).hasValueSatisfying(inactiveLicenceCondition);
        then(lookupInfo).hasValueSatisfying(wheelchairInaccessibleCondition);
      }
    }
  }

  private List<TaxiPhvVehicleLicence> findVehicleLicencesBy(String vrm) {
    return taxiPhvLicencePostgresRepository.findByVrm(vrm);
  }

  private List<String> findAuthorityNamesBy(String vrm) {
    return licensingAuthorityPostgresRepository.findAuthorityNamesByVrm(vrm);
  }

  private static TaxiPhvVehicleLicence.TaxiPhvVehicleLicenceBuilder accessibleWheelchairVehicle() {
    return licenceWithWheelchairAccessibleFlag(true);
  }

  private static TaxiPhvVehicleLicence.TaxiPhvVehicleLicenceBuilder notAccessibleWheelchairVehicle() {
    return licenceWithWheelchairAccessibleFlag(false);
  }

  private static TaxiPhvVehicleLicence.TaxiPhvVehicleLicenceBuilder emptyAccessibleWheelchairVehicle() {
    return licenceWithWheelchairAccessibleFlag(null);
  }

  private static TaxiPhvVehicleLicenceBuilder licenceWithWheelchairAccessibleFlag(Boolean flag) {
    return coreLicence()
        .wheelchairAccessible(flag);
  }

  private static TaxiPhvVehicleLicence notAccessibleWheelchairLicenceActiveTill(LocalDate end) {
    return notAccessibleWheelchairVehicle()
        .licenseDates(
            new LicenseDates(end.minusDays(10), end))
        .build();
  }

  private static TaxiPhvVehicleLicence accessibleWheelchairLicenceActiveTill(LocalDate end) {
    return accessibleWheelchairVehicle()
        .licenseDates(
            new LicenseDates(end.minusYears(1), end))
        .build();
  }

  private static TaxiPhvVehicleLicence nullAccessibleWheelchairLicenceActiveTill(LocalDate end) {
    return emptyAccessibleWheelchairVehicle()
        .licenseDates(
            new LicenseDates(end.minusYears(1), end))
        .build();
  }

  private static TaxiPhvVehicleLicence.TaxiPhvVehicleLicenceBuilder coreLicence() {
    return TaxiPhvVehicleLicence.builder()
        .id(1)
        .uploaderId(ANY_UPLOADER_ID)
        .vrm(ANY_VRM)
        .licensePlateNumber("plate-1")
        .description("TAXI")
        .licensingAuthority(
            new LicensingAuthority(99, "la"))
        .addedTimestamp(LocalDateTime.now());
  }

  static Stream<List<TaxiPhvVehicleLicence>> atLeastOneActiveLicenceProviderWithAtLeastOneAccessibleTrueFlag() {
    return Stream.of(
        Collections
            .singletonList(accessibleWheelchairLicenceActiveTill(DateHelper.today())),
        Arrays.asList(
            nullAccessibleWheelchairLicenceActiveTill(DateHelper.weekAgo()),
            accessibleWheelchairLicenceActiveTill(DateHelper.tomorrow()) // active
        ),
        Arrays.asList(
            nullAccessibleWheelchairLicenceActiveTill(DateHelper.monthAgo()),
            nullAccessibleWheelchairLicenceActiveTill(DateHelper.tomorrow()), // active
            nullAccessibleWheelchairLicenceActiveTill(DateHelper.yearAgo()),
            accessibleWheelchairLicenceActiveTill(DateHelper.tomorrow().plusDays(1)) // active
        )
    );
  }

  static Stream<List<TaxiPhvVehicleLicence>> atLeastOneActiveLicenceProviderWithNullAccessibleFlag() {
    return Stream.of(
        Collections
            .singletonList(nullAccessibleWheelchairLicenceActiveTill(DateHelper.today())),
        Arrays.asList(
            nullAccessibleWheelchairLicenceActiveTill(DateHelper.weekAgo()),
            nullAccessibleWheelchairLicenceActiveTill(DateHelper.tomorrow()) // active
        ),
        Arrays.asList(
            nullAccessibleWheelchairLicenceActiveTill(DateHelper.monthAgo()),
            nullAccessibleWheelchairLicenceActiveTill(DateHelper.tomorrow()), // active
            nullAccessibleWheelchairLicenceActiveTill(DateHelper.yearAgo()),
            nullAccessibleWheelchairLicenceActiveTill(DateHelper.tomorrow().plusDays(1)) // active
        )
    );
  }

  static Stream<List<TaxiPhvVehicleLicence>> atLeastOneActiveLicenceProviderWithAtLeastOneAccessibleFalseFlag() {
    return Stream.of(
        Collections
            .singletonList(notAccessibleWheelchairLicenceActiveTill(DateHelper.today())),
        Arrays.asList(
            notAccessibleWheelchairLicenceActiveTill(DateHelper.weekAgo()),
            // inaccessible & inactive
            accessibleWheelchairLicenceActiveTill(DateHelper.weekAgo()), // accessible & inactive
            nullAccessibleWheelchairLicenceActiveTill(DateHelper.tomorrow()), // null & active
            notAccessibleWheelchairLicenceActiveTill(DateHelper.today()) // inaccessible & active
        ),
        Arrays.asList(
            nullAccessibleWheelchairLicenceActiveTill(DateHelper.monthAgo()), // inactive
            accessibleWheelchairLicenceActiveTill(DateHelper.weekAgo()), // inactive
            nullAccessibleWheelchairLicenceActiveTill(DateHelper.today()), // null & active
            notAccessibleWheelchairLicenceActiveTill(DateHelper.tomorrow().plusDays(1))
            // inaccessible & active
        )
    );
  }

  static Stream<List<TaxiPhvVehicleLicence>> inactiveLicenceProviderWithAtLeastOneAccessibleFalseFlag() {
    return Stream.of(
        Collections
            .singletonList(notAccessibleWheelchairLicenceActiveTill(DateHelper.yesterday())),
        Arrays.asList(
            notAccessibleWheelchairLicenceActiveTill(DateHelper.weekAgo()),
            nullAccessibleWheelchairLicenceActiveTill(DateHelper.weekAgo())
        ),
        Arrays.asList(
            nullAccessibleWheelchairLicenceActiveTill(DateHelper.monthAgo()),
            accessibleWheelchairLicenceActiveTill(DateHelper.weekAgo()),
            accessibleWheelchairLicenceActiveTill(DateHelper.weekAgo().minusDays(1)),
            notAccessibleWheelchairLicenceActiveTill(DateHelper.yesterday())
        )
    );
  }

  static Stream<List<TaxiPhvVehicleLicence>> inactiveLicenceProviderWithNullAccessibleFlag() {
    return Stream.of(
        Collections
            .singletonList(nullAccessibleWheelchairLicenceActiveTill(DateHelper.yesterday())),
        Arrays.asList(
            nullAccessibleWheelchairLicenceActiveTill(DateHelper.weekAgo()),
            nullAccessibleWheelchairLicenceActiveTill(DateHelper.yesterday())
        )
    );
  }

  private List<String> sampleAuthirityNames() {
    return Lists.newArrayList("la-1", "la-2", "la-3");
  }
}