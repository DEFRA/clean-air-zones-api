package uk.gov.caz.accounts.service.registerjob;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import uk.gov.caz.accounts.model.AccountVehicle;
import uk.gov.caz.accounts.repository.AccountVehicleRepository;

@ExtendWith(MockitoExtension.class)
public class RegisterServiceTest {
  private static UUID ANY_ACCOUNT_ID = UUID.randomUUID();
  private static final AccountVehicle ANY_VALID_VEHICLE = AccountVehicle.builder()
      .accountId(ANY_ACCOUNT_ID)
      .vrn("8839GF")
      .build();
  private static final AccountVehicle ANY_INVALID_VEHICLE = AccountVehicle.builder()
      .accountId(UUID.randomUUID())
      .vrn("8839GF")
      .build();

  @Mock
  private AccountVehicleRepository accountVehicleRepository;

  @Captor
  private ArgumentCaptor<Set<AccountVehicle>> vehiclesCaptured;

  @Captor
  private ArgumentCaptor<List<AccountVehicle>> toBeDeletedVehiclesCaptor;

  @InjectMocks
  private RegisterService registerService;


  private void mockVehiclesInDb(Set<AccountVehicle> accountVehicles) {
    when(accountVehicleRepository.findAllByAccountId(any(), any())).thenReturn(
        new PageImpl<>(Lists.newArrayList(accountVehicles)));
  }

  @Test
  void shouldRejectUploadingWhenAccountVehiclesIsNull() {
    //given
    Set<AccountVehicle> accountVehicles = null;

    //then
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> registerService.register(accountVehicles, ANY_ACCOUNT_ID))
        .withMessage("'newAccountVehicles' cannot be null");
  }
  
  @Test
  void shouldRejectUploadingWhenAccountVehiclesIsEmpty() {
    // given
    Set<AccountVehicle> accountVehicles = Collections.emptySet();
    
    //then
    assertThatExceptionOfType(IllegalArgumentException.class)
    .isThrownBy(() -> registerService.register(accountVehicles, ANY_ACCOUNT_ID))
    .withMessage("'newAccountVehicles' cannot be empty");
    
  }

  @Test
  void shouldRejectUploadingWhenAccountIdIsNotTheSameForEachAccountVehicle() {
    //given
    Set<AccountVehicle> accountVehicles = ImmutableSet.of(ANY_VALID_VEHICLE, ANY_INVALID_VEHICLE);

    //then
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> registerService.register(accountVehicles, ANY_ACCOUNT_ID))
        .withMessage("AccountIds are not the same for each AccountVehicle");
  }

  @Test
  void shouldAddOnlyVehiclesWhichDoNotExistInDatabase() {
    // given
    Set<AccountVehicle> dbVehicles = vehiclesOfVrns(Sets.newHashSet("B", "C"));
    mockVehiclesInDb(dbVehicles);
    Set<AccountVehicle> csvVehicles = vehiclesOfVrns(Sets.newHashSet("A", "B", "C"));

    // when
    registerService.register(csvVehicles, ANY_ACCOUNT_ID);

    // then
    verify(accountVehicleRepository).saveAll(vehiclesCaptured.capture());
    Set<String> addedVrns = vehiclesCaptured.getValue().stream().map(AccountVehicle::getVrn)
        .collect(Collectors.toSet());
    assertThat(addedVrns).containsExactlyInAnyOrder("A");
  }

  @Test
  void shouldAddNothingIfAllVehiclesAreAlreadyInDatabase() {
    // given
    Set<AccountVehicle> dbVehicles = vehiclesOfVrns(Sets.newHashSet("A", "B", "C"));
    mockVehiclesInDb(dbVehicles);
    Set<AccountVehicle> csvVehicles = vehiclesOfVrns(Sets.newHashSet("A", "B", "C"));

    // when
    registerService.register(csvVehicles, ANY_ACCOUNT_ID);

    // then
    verify(accountVehicleRepository, times(0)).saveAll(any());
  }

  @Test
  void shouldRemoveOnlyVehiclesMissingInCsv() {
    // given
    Set<AccountVehicle> dbVehicles = vehiclesOfVrns(Sets.newHashSet("B", "C", "D", "E", "F"));
    mockVehiclesInDb(dbVehicles);
    Set<AccountVehicle> csvVehicles = vehiclesOfVrns(Sets.newHashSet("A", "C"));

    // when
    registerService.register(csvVehicles, ANY_ACCOUNT_ID);

    // then
    verify(accountVehicleRepository, times(1))
        .deleteAll(toBeDeletedVehiclesCaptor.capture());
    assertThat(extractVrnsFrom()).containsExactlyInAnyOrder("B", "D", "E", "F");
  }

  private Set<String> extractVrnsFrom() {
    return toBeDeletedVehiclesCaptor.getValue()
        .stream()
        .map(AccountVehicle::getVrn)
        .collect(Collectors.toSet());
  }

  @Test
  void shouldRemoveNothingOnlyNewVehiclesAdded() {
    // given
    Set<AccountVehicle> dbVehicles = vehiclesOfVrns(Sets.newHashSet("A", "B"));
    mockVehiclesInDb(dbVehicles);
    Set<AccountVehicle> csvVehicles = vehiclesOfVrns(Sets.newHashSet("A", "B", "C"));

    // when
    registerService.register(csvVehicles, ANY_ACCOUNT_ID);

    // then
    verify(accountVehicleRepository, never()).deleteAll(any());
  }

  private Set<AccountVehicle> vehiclesOfVrns(HashSet<String> vrns) {
    return vrns.stream()
        .map(vrn -> AccountVehicle.builder()
            .accountId(ANY_ACCOUNT_ID)
            .vrn(vrn)
            .build())
        .collect(Collectors.toSet());
  }
}
