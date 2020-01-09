package uk.gov.caz.taxiregister.service;


import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.taxiregister.model.LicensingAuthority;
import uk.gov.caz.taxiregister.repository.LicensingAuthorityPostgresRepository;

@ExtendWith(MockitoExtension.class)
class LicencesRegistrationSecuritySentinelTest {

  @Mock
  private LicensingAuthorityPostgresRepository repository;

  @InjectMocks
  private LicencesRegistrationSecuritySentinel securitySentinel;

  @ParameterizedTest
  @MethodSource("testDataProvider")
  public void testCheckUploaderPermissionsToModifyLicensingAuthoritiesScenarios(UUID uploaderId,
      List<LicensingAuthority> hasPermissionsToModify, Set<LicensingAuthority> wantsToModify,
      Optional<String> expectedSecurityCheckResult) {
    // given
    if (!wantsToModify.isEmpty()) {
      given(repository.findAllowedToBeModifiedBy(uploaderId)).willReturn(hasPermissionsToModify);
    }

    // when
    Optional<String> result = securitySentinel
        .checkUploaderPermissionsToModifyLicensingAuthorities(uploaderId, wantsToModify);

    // then
    assertThat(result).isEqualTo(expectedSecurityCheckResult);
  }

  private static Stream<Arguments> testDataProvider() {
    LicensingAuthority leeds = makeLicensingAuthority("Leeds");
    LicensingAuthority birmingham = makeLicensingAuthority("Birmingham");
    LicensingAuthority london = makeLicensingAuthority("London");

    return Stream.of(
        Arguments.arguments(
            TYPICAL_REGISTER_JOB_UPLOADER_ID,
            newArrayList(leeds, birmingham),         // Is allowed to modify
            newHashSet(leeds),                       // Wants to modify
            Optional.empty()                         // Should be fine
        ),
        Arguments.arguments(
            TYPICAL_REGISTER_JOB_UPLOADER_ID,
            newArrayList(leeds, birmingham),         // Is allowed to modify
            newHashSet(birmingham),                  // Wants to modify
            Optional.empty()                         // Should be fine
        ),
        Arguments.arguments(
            TYPICAL_REGISTER_JOB_UPLOADER_ID,
            newArrayList(leeds, birmingham),         // Is allowed to modify
            newHashSet(birmingham, leeds),           // Wants to modify
            Optional.empty()                         // Should be fine
        ),
        Arguments.arguments(
            TYPICAL_REGISTER_JOB_UPLOADER_ID,
            newArrayList(leeds, birmingham),         // Is allowed to modify
            newHashSet(),                            // Wants to modify
            Optional.empty()                         // Should be fine
        ),
        Arguments.arguments(
            TYPICAL_REGISTER_JOB_UPLOADER_ID,
            newArrayList(),                          // Is allowed to modify
            newHashSet(),                            // Wants to modify
            Optional.empty()                         // Should be fine
        ),
        Arguments.arguments(
            TYPICAL_REGISTER_JOB_UPLOADER_ID,
            newArrayList(leeds, birmingham),         // Is allowed to modify
            newHashSet(london),                      // Wants to modify
            Optional.of(error("London"))             // Should produce error with proper message
        ),
        Arguments.arguments(
            TYPICAL_REGISTER_JOB_UPLOADER_ID,
            newArrayList(),                          // Is allowed to modify
            newHashSet(leeds),                       // Wants to modify
            Optional.of(error("Leeds"))              // Should produce error with proper message
        ),
        Arguments.arguments(
            TYPICAL_REGISTER_JOB_UPLOADER_ID,
            newArrayList(leeds, birmingham),         // Is allowed to modify
            newHashSet(leeds, london),               // Wants to modify
            Optional.of(error("London"))             // Should produce error with proper message
        ),
        Arguments.arguments(
            TYPICAL_REGISTER_JOB_UPLOADER_ID,
            newArrayList(leeds, birmingham),         // Is allowed to modify
            newHashSet(birmingham, leeds, london),   // Wants to modify
            Optional.of(error("London"))             // Should produce error with proper message
        ),
        Arguments.arguments(
            TYPICAL_REGISTER_JOB_UPLOADER_ID,
            newArrayList(birmingham),                // Is allowed to modify
            newHashSet(leeds, london),               // Wants to modify
            Optional.of(error("Leeds, London"))      // Should produce error with proper message
        )
    );
  }

  private static LicensingAuthority makeLicensingAuthority(String name) {
    return LicensingAuthority.withNameOnly(name);
  }

  private static String error(String expected) {
    return "You are not authorised to submit data for " + expected;
  }
}