package uk.gov.caz.taxiregister.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.taxiregister.model.LicenseDates;
import uk.gov.caz.taxiregister.model.LicensingAuthority;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicence;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.model.VrmSet;
import uk.gov.caz.taxiregister.repository.AuditingRepository;
import uk.gov.caz.taxiregister.repository.TaxiPhvLicencePostgresRepository;
import uk.gov.caz.taxiregister.service.RegisterService.RegisterContext;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

  private static final UUID ANY_UPLOADER_ID = UUID
      .fromString("c5052136-46b9-4a07-8051-7da01b5c84c5");

  @Mock
  private TaxiPhvLicencePostgresRepository taxiPhvLicencePostgresRepository;
  
  @Mock
  private AuditingRepository auditingRepository;

  @Mock
  private RegisterContextFactory registerContextFactory;

  @Mock
  private VehicleComplianceCheckerService vehicleComplianceCheckerService;

  @InjectMocks
  private RegisterService registerService;

  @Captor
  private ArgumentCaptor<Set<Integer>> integerSetCaptor;

  @Captor
  private ArgumentCaptor<Set<TaxiPhvVehicleLicence>> vehicleSetInsertCaptor;

  @Captor
  private ArgumentCaptor<Set<TaxiPhvVehicleLicence>> vehicleSetUpdateCaptor;

  @Captor
  private ArgumentCaptor<VrmSet> vrmSetArgumentCaptor;

  private static final TaxiPhvVehicleLicence ANY_EXISTING_TAXI_PHV_VEHICLE_LICENCE = TaxiPhvVehicleLicence
      .builder()
      .id(1)
      .uploaderId(ANY_UPLOADER_ID)
      .vrm("8839GF")
      .wheelchairAccessible(true)
      .licensePlateNumber("old")
      .description("TAXI")
      .licenseDates(new LicenseDates(LocalDate.now(), LocalDate.now().plusDays(1)))
      .licensingAuthority(
          new LicensingAuthority(99, "la"))
      .build();

  private static final TaxiPhvVehicleLicence ANY_NEW_TAXI_PHV_VEHICLE_LICENCE = ANY_EXISTING_TAXI_PHV_VEHICLE_LICENCE
      .toBuilder()
      .id(null)
      .uploaderId(null)
      .licensingAuthority(LicensingAuthority.withNameOnly("la"))
      .build();

  @Test
  public void shouldThrowNullPointerExceptionWhenVehiclesIsNull() {
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> registerService.register(null, ANY_UPLOADER_ID))
        .withMessage("licences cannot be null");
  }

  @Test
  public void shouldThrowNullPointerExceptionWhenUploaderIdIsNull() {
    UUID uploaderId = null;

    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> registerService.register(Collections.emptySet(), uploaderId))
        .withMessage("uploaderId cannot be null");
  }

  @Nested
  class Insert {

    @Test
    public void shouldInsertLicenceIfAbsentInDatabase() {
      LicensingAuthority licensingAuthority = ANY_EXISTING_TAXI_PHV_VEHICLE_LICENCE
          .getLicensingAuthority();
      TaxiPhvVehicleLicence toBeRegistered = ANY_NEW_TAXI_PHV_VEHICLE_LICENCE;
      mockData(toLicensingAuthorityMap(licensingAuthority), Collections.emptySet());

      registerService.register(Collections.singleton(toBeRegistered), ANY_UPLOADER_ID);

      TaxiPhvVehicleLicence expected = ANY_EXISTING_TAXI_PHV_VEHICLE_LICENCE.toBuilder()
          .id(null)
          .uploaderId(ANY_UPLOADER_ID)
          .licensingAuthority(licensingAuthority)
          .build();

      assertThat(extractInserted()).containsOnly(expected);
      assertThat(extractDeletedIds()).isEmpty();
      assertThat(extractUpdated()).isEmpty();
      shouldRefreshCacheForVrms();
    }

    @Test
    public void shouldNotInsertLicenceIfNotMatchesExistingLicensingAuthority() {
      Set<TaxiPhvVehicleLicence> toBeRegistered = Collections.singleton(
          ANY_NEW_TAXI_PHV_VEHICLE_LICENCE.toBuilder()
              .licensingAuthority(LicensingAuthority.withNameOnly("absent"))
              .build()
      );
      given(registerContextFactory.createContext(toBeRegistered, ANY_UPLOADER_ID))
          .willThrow(new LicensingAuthorityMismatchException(new ArrayList<>(toBeRegistered)));

      RegisterResult result = registerService.register(toBeRegistered, ANY_UPLOADER_ID);

      then(result.isSuccess()).isFalse();
      then(result.getValidationErrors()).containsExactly(
          ValidationError.valueError(
              toBeRegistered.iterator().next().getVrm(),
              "Vehicle's licensing authority absent does not match any existing ones."
          ));
      verifyZeroInteractions(taxiPhvLicencePostgresRepository);
      shouldNotTouchCache();
    }

    @Test
    public void shouldInsertNewLicenceAndDeleteOldIfLicensePlateNumberChanged() {
      LicensingAuthority currentLicensingAuthority = ANY_EXISTING_TAXI_PHV_VEHICLE_LICENCE
          .getLicensingAuthority();
      TaxiPhvVehicleLicence toBeRegistered = ANY_EXISTING_TAXI_PHV_VEHICLE_LICENCE.toBuilder()
          .id(null)
          .uploaderId(null)
          .licensingAuthority(LicensingAuthority.withNameOnly(currentLicensingAuthority.getName()))
          .licensePlateNumber("new")
          .build();

      mockData(ANY_EXISTING_TAXI_PHV_VEHICLE_LICENCE);

      registerService.register(Collections.singleton(toBeRegistered), ANY_UPLOADER_ID);

      TaxiPhvVehicleLicence expected = toBeRegistered.toBuilder()
          .licensingAuthority(currentLicensingAuthority)
          .uploaderId(ANY_UPLOADER_ID)
          .build();
      assertThat(extractUpdated()).isEmpty();
      assertThat(extractDeletedIds()).containsOnly(ANY_EXISTING_TAXI_PHV_VEHICLE_LICENCE.getId());
      assertThat(extractInserted()).containsOnly(expected);
      shouldRefreshCacheForVrms();
    }

    @ParameterizedTest
    @MethodSource("uk.gov.caz.taxiregister.service.RegisterServiceTest#licenseDatesMapperProvider")
    public void shouldInsertNewLicenceAndDeleteOldOneIfLicenseDatesChanged(
        Function<LicenseDates, LicenseDates> mapper) {
      TaxiPhvVehicleLicence toBeRegistered = ANY_EXISTING_TAXI_PHV_VEHICLE_LICENCE.toBuilder()
          .id(null)
          .licensingAuthority(LicensingAuthority.withNameOnly(
              ANY_EXISTING_TAXI_PHV_VEHICLE_LICENCE.getLicensingAuthority().getName()
          ))
          .licenseDates(mapper.apply(ANY_EXISTING_TAXI_PHV_VEHICLE_LICENCE.getLicenseDates()))
          .build();

      mockData(ANY_EXISTING_TAXI_PHV_VEHICLE_LICENCE);

      registerService.register(Collections.singleton(toBeRegistered), ANY_UPLOADER_ID);

      TaxiPhvVehicleLicence expectedToBeInserted = toBeRegistered.toBuilder()
          .licensingAuthority(ANY_EXISTING_TAXI_PHV_VEHICLE_LICENCE.getLicensingAuthority())
          .build();
      assertThat(extractInserted()).containsOnly(expectedToBeInserted);
      assertThat(extractDeletedIds())
          .containsExactly(ANY_EXISTING_TAXI_PHV_VEHICLE_LICENCE.getId());
      assertThat(extractUpdated()).isEmpty();
      shouldRefreshCacheForVrms();
    }

    @Test
    public void anyExceptionDuringCacheRefreshShouldNotBreakRegisterOperation() {
      LicensingAuthority licensingAuthority = ANY_EXISTING_TAXI_PHV_VEHICLE_LICENCE
          .getLicensingAuthority();
      TaxiPhvVehicleLicence toBeRegistered = ANY_NEW_TAXI_PHV_VEHICLE_LICENCE;
      mockData(toLicensingAuthorityMap(licensingAuthority), Collections.emptySet());
      willAnswer(invocation -> {
        throw new Exception("Exc");
      }).given(vehicleComplianceCheckerService).purgeCacheOfNtrData(any(VrmSet.class));

      registerService.register(Collections.singleton(toBeRegistered), ANY_UPLOADER_ID);

      TaxiPhvVehicleLicence expected = ANY_EXISTING_TAXI_PHV_VEHICLE_LICENCE.toBuilder()
          .id(null)
          .uploaderId(ANY_UPLOADER_ID)
          .licensingAuthority(licensingAuthority)
          .build();

      assertThat(extractInserted()).containsOnly(expected);
      assertThat(extractDeletedIds()).isEmpty();
      assertThat(extractUpdated()).isEmpty();
      shouldRefreshCacheForVrms();
    }
  }

  @Nested
  class Update {

    @Test
    public void shouldUpdateVehicleIfWheelchairAccessibleChangedFromFalseToTrue() {
      shouldUpdateVehicleIfWheelchairAccessibleChanged(false, true);
    }

    @Test
    public void shouldUpdateVehicleIfWheelchairAccessibleChangedFromFalseToNull() {
      shouldUpdateVehicleIfWheelchairAccessibleChanged(false, null);
    }

    @Test
    public void shouldUpdateVehicleIfWheelchairAccessibleChangedFromTrueToFalse() {
      shouldUpdateVehicleIfWheelchairAccessibleChanged(true, false);
    }

    @Test
    public void shouldUpdateVehicleIfWheelchairAccessibleChangedFromTrueToNull() {
      shouldUpdateVehicleIfWheelchairAccessibleChanged(true, null);
    }

    @Test
    public void shouldUpdateVehicleIfWheelchairAccessibleChangedFromNullToTrue() {
      shouldUpdateVehicleIfWheelchairAccessibleChanged(null, true);
    }

    @Test
    public void shouldUpdateVehicleIfWheelchairAccessibleChangedFromNullToFalse() {
      shouldUpdateVehicleIfWheelchairAccessibleChanged(null, false);
    }

    @ParameterizedTest
    @MethodSource("uk.gov.caz.taxiregister.service.RegisterServiceTest#wheelchairAccessibilityFlagProvider")
    public void shouldNotUpdateVehicleIfWheelchairAccessibleFlagsAreTheSame(Boolean from) {
      Boolean to = from == null ? null : new Boolean(from);
      TaxiPhvVehicleLicence current = TaxiPhvVehicleLicence.builder()
          .id(1)
          .uploaderId(ANY_UPLOADER_ID)
          .vrm("8839GF")
          .wheelchairAccessible(from)
          .licensePlateNumber("plate-number")
          .description("TAXI")
          .licenseDates(new LicenseDates(LocalDate.now(), LocalDate.now().plusDays(1)))
          .licensingAuthority(
              new LicensingAuthority(99, "new-name-1"))
          .build();

      TaxiPhvVehicleLicence toBeRegistered = current.toBuilder()
          .id(null)
          .uploaderId(null)
          .licensingAuthority(
              LicensingAuthority.withNameOnly(current.getLicensingAuthority().getName()))
          .wheelchairAccessible(to)
          .build();

      mockData(current);

      registerService.register(Collections.singleton(toBeRegistered), ANY_UPLOADER_ID);

      assertThat(extractUpdated()).isEmpty();
      assertThat(extractDeletedIds()).isEmpty();
      assertThat(extractInserted()).isEmpty();
      shouldNotTouchCache();
    }

    @Test
    public void shouldUpdateVehicleIfVehicleTypeChangedFromPhvToTaxi() {
      shouldUpdateVehicleIfVehicleTypeChanged("PHV", "TAXI");
    }

    @Test
    public void shouldUpdateVehicleIfVehicleTypeChangedFromTaxiToPhv() {
      shouldUpdateVehicleIfVehicleTypeChanged("TAXI", "PHV");
    }

    @Test
    public void shouldNotUpdateVehicleIfAttributesAreTheSame() {
      TaxiPhvVehicleLicence toBeRegistered = ANY_NEW_TAXI_PHV_VEHICLE_LICENCE;

      mockData(ANY_EXISTING_TAXI_PHV_VEHICLE_LICENCE);

      registerService.register(Collections.singleton(toBeRegistered), ANY_UPLOADER_ID);

      assertThat(extractUpdated()).isEmpty();
      assertThat(extractDeletedIds()).isEmpty();
      assertThat(extractInserted()).isEmpty();
      shouldNotTouchCache();
    }

    private void shouldUpdateVehicleIfVehicleTypeChanged(String from, String to) {
      TaxiPhvVehicleLicence current = ANY_EXISTING_TAXI_PHV_VEHICLE_LICENCE.toBuilder()
          .description(from)
          .build();

      TaxiPhvVehicleLicence toBeRegistered = current.toBuilder()
          .id(null)
          .licensingAuthority(
              LicensingAuthority.withNameOnly(current.getLicensingAuthority().getName()))
          .description(to)
          .build();

      mockData(current);

      registerService.register(Collections.singleton(toBeRegistered), ANY_UPLOADER_ID);

      TaxiPhvVehicleLicence expected = current.toBuilder()
          .description(toBeRegistered.getDescription())
          .build();
      assertThat(extractUpdated()).containsOnly(expected);
      assertThat(extractDeletedIds()).isEmpty();
      assertThat(extractInserted()).isEmpty();
      shouldRefreshCacheForVrms();
    }

    private void shouldUpdateVehicleIfWheelchairAccessibleChanged(Boolean from, Boolean to) {
      TaxiPhvVehicleLicence current = TaxiPhvVehicleLicence.builder()
          .id(1)
          .uploaderId(ANY_UPLOADER_ID)
          .vrm("8839GF")
          .wheelchairAccessible(from)
          .licensePlateNumber("plate-number")
          .description("TAXI")
          .licenseDates(new LicenseDates(LocalDate.now(), LocalDate.now().plusDays(1)))
          .licensingAuthority(
              new LicensingAuthority(99, "new-name-1"))
          .build();

      TaxiPhvVehicleLicence toBeRegistered = current.toBuilder()
          .id(null)
          .uploaderId(null)
          .licensingAuthority(
              LicensingAuthority.withNameOnly(current.getLicensingAuthority().getName()))
          .wheelchairAccessible(to)
          .build();

      mockData(current);

      registerService.register(Collections.singleton(toBeRegistered), ANY_UPLOADER_ID);

      TaxiPhvVehicleLicence expected = current.toBuilder()
          .wheelchairAccessible(toBeRegistered.getWheelchairAccessible())
          .build();
      assertThat(extractUpdated()).containsOnly(expected);
      assertThat(extractDeletedIds()).isEmpty();
      assertThat(extractInserted()).isEmpty();
      shouldRefreshCacheForVrms();
    }
  }

  @Nested
  class Delete {

    @Test
    public void shouldDeleteLicencesInDbWhichAreNotUpdatedAndInserted() {
      Set<TaxiPhvVehicleLicence> existing = createExisting();
      Set<TaxiPhvVehicleLicence> toBeInserted = createToBeInserted();
      Set<TaxiPhvVehicleLicence> toBeUpdated = pickOneToBeUpdated(existing);
      Set<TaxiPhvVehicleLicence> toBeRegistered = Sets.union(toBeInserted, toBeUpdated);
      mockData(existing);

      registerService.register(toBeRegistered, ANY_UPLOADER_ID);

      Set<Integer> expectedToBeDeleted = Sets.difference(toIdSet(existing), toIdSet(toBeUpdated));
      assertThat(extractInserted()).hasSameSizeAs(toBeInserted);
      assertThat(extractDeletedIds()).containsExactlyElementsOf(expectedToBeDeleted);
      assertThat(extractUpdated()).hasSameSizeAs(toBeUpdated);
      shouldRefreshCacheForVrms();
    }

    private Set<Integer> toIdSet(Set<TaxiPhvVehicleLicence> existing) {
      return existing.stream().map(TaxiPhvVehicleLicence::getId).collect(
          Collectors.toSet());
    }

    private Set<TaxiPhvVehicleLicence> pickOneToBeUpdated(
        Set<TaxiPhvVehicleLicence> existingTaxiPhvVehicleLicences) {
      TaxiPhvVehicleLicence existing = existingTaxiPhvVehicleLicences.iterator().next();
      return Collections.singleton(
          existing.toBuilder()
              .wheelchairAccessible(!existing.getWheelchairAccessible())
              .build()
      );
    }

    private Set<TaxiPhvVehicleLicence> createToBeInserted() {
      return IntStream.rangeClosed(1, 9)
          .mapToObj(i -> ANY_NEW_TAXI_PHV_VEHICLE_LICENCE.toBuilder()
              .vrm("883" + i + "GF")
              .build())
          .collect(Collectors.toSet());
    }

    private Set<TaxiPhvVehicleLicence> createExisting() {
      return IntStream.rangeClosed(1, 5)
          .mapToObj(i -> ANY_EXISTING_TAXI_PHV_VEHICLE_LICENCE.toBuilder()
              .id(i)
              .vrm("87" + i + "9GF")
              .build())
          .collect(Collectors.toSet());
    }
  }

  static Stream<Boolean> wheelchairAccessibilityFlagProvider() {
    return Stream.of(Boolean.TRUE, Boolean.FALSE, null);
  }

  static Stream<Function<LicenseDates, LicenseDates>> licenseDatesMapperProvider() {
    return Stream.of(
        (licenseDates) -> new LicenseDates(licenseDates.getStart(),
            licenseDates.getEnd().plusDays(2)),
        (licenseDates) -> new LicenseDates(licenseDates.getStart().minusDays(1),
            licenseDates.getEnd()),
        (licenseDates) -> new LicenseDates(licenseDates.getStart().plusDays(1),
            licenseDates.getEnd().plusDays(2))
    );
  }

  private void mockData(Collection<TaxiPhvVehicleLicence> toBeRegistered) {
    List<LicensingAuthority> licensingAuthorities = toBeRegistered.stream()
        .map(TaxiPhvVehicleLicence::getLicensingAuthority)
        .collect(Collectors.toList());
    mockData(toLicensingAuthorityMap(licensingAuthorities), ImmutableSet.copyOf(toBeRegistered));
  }

  private void mockData(TaxiPhvVehicleLicence taxiPhvVehicleLicenceInDatabase) {
    mockData(toLicensingAuthorityMap(taxiPhvVehicleLicenceInDatabase.getLicensingAuthority()),
        Collections.singleton(taxiPhvVehicleLicenceInDatabase));
  }

  private void mockData(Map<String, LicensingAuthority> licensingAuthorityMap,
      Set<TaxiPhvVehicleLicence> licencesInDb) {
    when(registerContextFactory.createContext(anySet(), any())).thenAnswer(answer -> {
      UUID uploaderId = answer.getArgument(1);
      Set<TaxiPhvVehicleLicence> licences = answer.getArgument(0);
      return new RegisterContext(uploaderId,
          licences.stream()
              .filter(licence -> licensingAuthorityMap
                  .containsKey(licence.getLicensingAuthority().getName()))
              .map(licence -> licence.toBuilder().licensingAuthority(
                  licensingAuthorityMap.get(licence.getLicensingAuthority().getName())).build())
              .collect(Collectors
                  .groupingBy(TaxiPhvVehicleLicence::getLicensingAuthority, Collectors.toSet())),
          licencesInDb.stream()
              .collect(Collectors.groupingBy(TaxiPhvVehicleLicence::getLicensingAuthority,
                  Collectors.toMap(UniqueLicenceAttributes::from, Function.identity()))),
          licensingAuthorityMap);
    });
  }

  private Set<TaxiPhvVehicleLicence> extractUpdated() {
    verify(taxiPhvLicencePostgresRepository, atLeastOnce())
        .update(vehicleSetUpdateCaptor.capture());
    return vehicleSetUpdateCaptor.getAllValues().stream().flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  private Set<TaxiPhvVehicleLicence> extractInserted() {
    verify(taxiPhvLicencePostgresRepository, atLeastOnce())
        .insert(vehicleSetInsertCaptor.capture());
    return vehicleSetInsertCaptor.getAllValues().stream().flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  private Set<Integer> extractDeletedIds() {
    verify(taxiPhvLicencePostgresRepository).delete(integerSetCaptor.capture());
    return integerSetCaptor.getValue();
  }

  private Map<String, LicensingAuthority> toLicensingAuthorityMap(
      LicensingAuthority... licensingAuthorities) {
    return toLicensingAuthorityMap(Lists.newArrayList(licensingAuthorities));
  }

  private Map<String, LicensingAuthority> toLicensingAuthorityMap(
      Collection<LicensingAuthority> licensingAuthorities) {
    return licensingAuthorities.stream()
        .collect(Collectors.toMap(LicensingAuthority::getName, Function.identity(), (a, b) -> a));
  }

  private void shouldRefreshCacheForVrms() {
    verify(taxiPhvLicencePostgresRepository).cacheEvictLicensingRepository();

    verify(vehicleComplianceCheckerService).purgeCacheOfNtrData(vrmSetArgumentCaptor.capture());
    VrmSet actualAffectedVrmSet = vrmSetArgumentCaptor.getValue();
    assertThat(actualAffectedVrmSet).isNotNull();
  }

  private void shouldNotTouchCache() {
    verifyZeroInteractions(vehicleComplianceCheckerService);
  }
}