package uk.gov.caz.taxiregister.service;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.taxiregister.model.LicensingAuthority;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicence;
import uk.gov.caz.taxiregister.repository.LicensingAuthorityPostgresRepository;
import uk.gov.caz.taxiregister.repository.TaxiPhvLicencePostgresRepository;
import uk.gov.caz.taxiregister.service.RegisterService.RegisterContext;
import uk.gov.caz.testutils.TestObjects;
import uk.gov.caz.testutils.TestObjects.Licences;

@ExtendWith(MockitoExtension.class)
class RegisterContextFactoryTest {

  @Mock
  private LicensingAuthorityPostgresRepository licensingAuthorityRepository;

  @Mock
  private TaxiPhvLicencePostgresRepository vehicleRepository;

  @InjectMocks
  private RegisterContextFactory registerContextFactory;

  @Test
  public void shouldReturnLicensingAuthoritiesFromDatabase() {
    // given
    Map<String, LicensingAuthority> licensingAuthorities = mockValidLicensingAuthoritiesInDatabase();

    // when
    RegisterContext context = registerContextFactory
        .createContext(emptySet(), TestObjects.Registration.uploaderId());

    // then
    BDDAssertions.then(context.getCurrentLicensingAuthoritiesByName())
        .isEqualTo(licensingAuthorities);
  }

  @Test
  public void shouldThrowLicensingAuthorityMismatchExceptionIfExistsLicenceWithNonMatchingLaInDb() {
    // given
    mockValidLicensingAuthoritiesInDatabase();
    TaxiPhvVehicleLicence mismatchedLicence = licenceWithMismatchedLicensingAuthority();
    Set<TaxiPhvVehicleLicence> licenses = ImmutableSet.of(Licences.toBeRegistered(), mismatchedLicence);

    // when
    Throwable throwable = catchThrowable(
        () -> registerContextFactory.createContext(licenses, TestObjects.Registration.uploaderId())
    );

    // then
    BDDAssertions.then(throwable).isInstanceOf(LicensingAuthorityMismatchException.class)
        .hasFieldOrPropertyWithValue("licencesWithNonMatchingLicensingAuthority",
            singletonList(mismatchedLicence));
  }

  @Test
  public void shouldGroupNewLicencesByLicensingAuthority() {
    // given
    Map<String, LicensingAuthority> licensingAuthorities = mockValidLicensingAuthoritiesInDatabase();

    TaxiPhvVehicleLicence licenceFromLaOne = Licences.toBeRegistered().toBuilder().vrm("8839GF")
        .licensingAuthority(createLicensingAuthority("la-1")).build();
    TaxiPhvVehicleLicence firstLicenceFromLaTwo = Licences.toBeRegistered().toBuilder().vrm("8839GG")
        .licensingAuthority(createLicensingAuthority("la-2")).build();
    TaxiPhvVehicleLicence secondLicenceFromLaTwo = Licences.toBeRegistered().toBuilder().vrm("8839GH")
        .licensingAuthority(createLicensingAuthority("la-2")).build();
    TaxiPhvVehicleLicence firstLicenceFromLaThree = Licences.toBeRegistered().toBuilder().vrm("8839GI")
        .licensingAuthority(createLicensingAuthority("la-3")).build();
    TaxiPhvVehicleLicence secondLicenceFromLaThree = Licences.toBeRegistered().toBuilder().vrm("8839GJ")
        .licensingAuthority(createLicensingAuthority("la-3")).build();
    TaxiPhvVehicleLicence thirdLicenceFromLaThree = Licences.toBeRegistered().toBuilder().vrm("8839GK")
        .licensingAuthority(createLicensingAuthority("la-3")).build();

    Set<TaxiPhvVehicleLicence> licenses = ImmutableSet.of(licenceFromLaOne, firstLicenceFromLaTwo,
        secondLicenceFromLaTwo, firstLicenceFromLaThree, secondLicenceFromLaThree,
        thirdLicenceFromLaThree
    );

    // when
    RegisterContext context = registerContextFactory.createContext(licenses,
        TestObjects.Registration.uploaderId());

    // then
    BDDAssertions.then(context.getNewLicencesByLicensingAuthority()).containsOnlyKeys(
        licensingAuthorities.get("la-1"),
        licensingAuthorities.get("la-2"),
        licensingAuthorities.get("la-3")
    );

    BDDAssertions.then(getNewLicenceByLaName(licensingAuthorities, context, "la-1"))
        .hasSize(1)
        .allMatch(licence -> Objects.nonNull(licence.getLicensingAuthority().getId()));
    BDDAssertions.then(getNewLicenceByLaName(licensingAuthorities, context, "la-2"))
        .hasSize(2)
        .allMatch(licence -> Objects.nonNull(licence.getLicensingAuthority().getId()));
    BDDAssertions.then(getNewLicenceByLaName(licensingAuthorities, context, "la-3"))
        .hasSize(3)
        .allMatch(licence -> Objects.nonNull(licence.getLicensingAuthority().getId()));
  }

  @Test
  public void shouldGroupExistingLicencesByLicensingAuthorityAndUniqueAttributes() {
    // given
    Map<String, LicensingAuthority> licensingAuthorities = mockValidLicensingAuthoritiesInDatabase();
    Map<Integer, Set<TaxiPhvVehicleLicence>> currentLicencesByLaId = mockCurrentLicences(
        licensingAuthorities);
    Set<TaxiPhvVehicleLicence> licenses = createLicencesForEachLicensingAuthority();

    // when
    RegisterContext context = registerContextFactory.createContext(licenses,
        TestObjects.Registration.uploaderId());

    // then
    Map<LicensingAuthority, Map<UniqueLicenceAttributes, TaxiPhvVehicleLicence>> result = context
        .getCurrentLicencesByLicensingAuthority();

    BDDAssertions.then(result).containsOnlyKeys(
        licensingAuthorities.get("la-1"),
        licensingAuthorities.get("la-2"),
        licensingAuthorities.get("la-3")
    );

    LicensingAuthority firstLicensingAuthority = licensingAuthorities.get("la-1");
    LicensingAuthority secondLicensingAuthority= licensingAuthorities.get("la-2");
    LicensingAuthority thirdLicensingAuthority = licensingAuthorities.get("la-3");

    assertResultContainsEntitiesFromDbFor(firstLicensingAuthority, result, currentLicencesByLaId);
    assertResultContainsEntitiesFromDbFor(secondLicensingAuthority, result, currentLicencesByLaId);
    assertResultContainsEntitiesFromDbFor(thirdLicensingAuthority, result, currentLicencesByLaId);
  }

  private void assertResultContainsEntitiesFromDbFor(
      LicensingAuthority licensingAuthority,
      Map<LicensingAuthority, Map<UniqueLicenceAttributes, TaxiPhvVehicleLicence>> result,
      Map<Integer, Set<TaxiPhvVehicleLicence>> currentLicencesByLaId) {

    Set<TaxiPhvVehicleLicence> actualLicences = ImmutableSet.copyOf(
        result.get(licensingAuthority).values()
    );
    Set<TaxiPhvVehicleLicence> expectedLicences = currentLicencesByLaId.get(licensingAuthority.getId());

    BDDAssertions.then(actualLicences).isEqualTo(expectedLicences);
  }


  private Set<TaxiPhvVehicleLicence> createLicencesForEachLicensingAuthority() {
    TaxiPhvVehicleLicence licenceFromLaOne = Licences.toBeRegistered().toBuilder().vrm("8839GF")
        .licensingAuthority(createLicensingAuthority("la-1")).build();
    TaxiPhvVehicleLicence licenceFromLaTwo = Licences.toBeRegistered().toBuilder().vrm("8839GG")
        .licensingAuthority(createLicensingAuthority("la-2")).build();
    TaxiPhvVehicleLicence licenceFromLaThree = Licences.toBeRegistered().toBuilder().vrm("8839GI")
        .licensingAuthority(createLicensingAuthority("la-3")).build();

    return ImmutableSet.of(licenceFromLaOne, licenceFromLaTwo,
        licenceFromLaThree);
  }

  private Map<Integer, Set<TaxiPhvVehicleLicence>> mockCurrentLicences(Map<String, LicensingAuthority> licensingAuthorities) {
    Map<Integer, Set<TaxiPhvVehicleLicence>> currentLicences = licensingAuthorities.values()
        .stream()
        .collect(Collectors.toMap(LicensingAuthority::getId,
            licensingAuthority -> IntStream.rangeClosed(1, licensingAuthority.getId())
                .mapToObj(index -> TestObjects.Licences.existing()
                    .toBuilder()
                    .id(index)
                    .vrm(index + "839GF")
                    .licensingAuthority(licensingAuthority)
                    .build())
                .collect(Collectors.toSet())));
    given(vehicleRepository.findByLicensingAuthority(anyInt())).willAnswer(answer -> {
      Integer id = answer.getArgument(0);
      return currentLicences.get(id);
    });
    return currentLicences;
  }

  private Set<TaxiPhvVehicleLicence> getNewLicenceByLaName(
      Map<String, LicensingAuthority> licensingAuthorities, RegisterContext context, String name) {
    return context.getNewLicencesByLicensingAuthority().get(licensingAuthorities.get(name));
  }

  private Map<String, LicensingAuthority> mockValidLicensingAuthoritiesInDatabase() {
    Map<String, LicensingAuthority> licensingAuthorities = ImmutableMap.of(
        "la-1", new LicensingAuthority(1, "la-1"),
        "la-2", new LicensingAuthority(2, "la-2"),
        "la-3", new LicensingAuthority(3, "la-3")
    );
    given(licensingAuthorityRepository.findAll()).willReturn(licensingAuthorities);
    return licensingAuthorities;
  }

  private TaxiPhvVehicleLicence licenceWithMismatchedLicensingAuthority() {
    return TestObjects.Licences.existing()
        .toBuilder()
        .licensingAuthority(createLicensingAuthority("not-existing"))
        .build();
  }

  private static LicensingAuthority createLicensingAuthority(String name) {
    return LicensingAuthority.withNameOnly(name);
  }
}