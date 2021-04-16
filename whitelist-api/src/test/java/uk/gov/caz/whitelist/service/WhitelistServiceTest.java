package uk.gov.caz.whitelist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.caz.whitelist.testutils.TestObjects.whitelistedVehicle;

import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.whitelist.controller.exception.VrnAlreadyWhitelistedException;
import uk.gov.caz.whitelist.model.ConversionResults;
import uk.gov.caz.whitelist.model.WhitelistVehicle;
import uk.gov.caz.whitelist.repository.WhitelistVehiclePostgresRepository;
import uk.gov.caz.whitelist.service.exception.VehicleNotFoundException;

@ExtendWith(MockitoExtension.class)
class WhitelistServiceTest {

  private static final UUID X_MODIFIER_ID = UUID.randomUUID();
  private static final String MODIFIER_EMAIL = RandomStringUtils.randomAlphabetic(10);

  @Mock
  private WhitelistVehiclePostgresRepository whitelistVehiclePostgresRepository;

  @Mock
  private RegisterService registerService;

  @InjectMocks
  private WhitelistService whitelistService;

  @Captor
  private ArgumentCaptor<ConversionResults> conversionResultsArgumentCaptor;

  @Nested
  class FindOneByVrn {

    @Test
    void shouldReturnWhitelistedVehicle() {
      // given
      String sampleVrn = "CAS310";
      given(whitelistVehiclePostgresRepository.findOneByVrn(sampleVrn))
          .willReturn(whitelistedVehicle());

      // when
      Optional<WhitelistVehicle> whitelistVehicle = whitelistService.findBy(sampleVrn);

      // then
      assertThat(whitelistVehicle).isPresent();
      assertThat(whitelistVehicle.get().getVrn()).isEqualTo(sampleVrn);
      verify(whitelistVehiclePostgresRepository, times(1)).findOneByVrn(sampleVrn);
    }

    @Test
    void shouldReturnEmpty() {
      // when
      String sampleVrn = "CAS310";
      Optional<WhitelistVehicle> whitelistVehicle = whitelistService.findBy(sampleVrn);

      // then
      assertThat(whitelistVehicle).isEmpty();
      verify(whitelistVehiclePostgresRepository, times(1)).findOneByVrn(sampleVrn);
    }
  }

  @Nested
  class Save {

    @Test
    void shouldReturnWhitelistedVehicleWhenItIsNotPersisted() {
      // given
      String sampleVrn = "CAS310";
      WhitelistVehicle vehicle = getWhitelistVehicleSampleObject();
      when(whitelistVehiclePostgresRepository.findOneByVrn(vehicle.getVrn()))
          .thenReturn(Optional.of(vehicle));

      // when
      WhitelistVehicle whitelistVehicle = whitelistService.save(vehicle);

      // then
      assertThat(whitelistVehicle.getVrn()).isEqualTo(sampleVrn);
      verify(whitelistVehiclePostgresRepository, times(1)).exists(sampleVrn);
      verify(whitelistVehiclePostgresRepository, times(1)).findOneByVrn(sampleVrn);
      verify(registerService)
          .register(conversionResultsArgumentCaptor.capture(), eq(X_MODIFIER_ID), eq(MODIFIER_EMAIL));
      ConversionResults conversionResults = conversionResultsArgumentCaptor.getValue();
      assertThat(conversionResults.getWhitelistVehiclesToSaveOrUpdate()).hasSize(1);
      assertThat(conversionResults.getVrnToDelete()).isEmpty();
      assertThat(conversionResults.getValidationErrors()).isEmpty();
    }

    @Test
    void shouldThrowVrnAlreadyWhitelistedExceptionWhenItIsAlreadyStoredInTheDatabase() {
      WhitelistVehicle vehicle = getWhitelistVehicleSampleObject();
      when(whitelistVehiclePostgresRepository.exists(vehicle.getVrn()))
          .thenReturn(true);

      Throwable throwable = catchThrowable(() -> whitelistService.save(vehicle));

      assertThat(throwable).isInstanceOf(VrnAlreadyWhitelistedException.class)
          .hasMessage("VRN is already whitelisted.");
    }

    private WhitelistVehicle getWhitelistVehicleSampleObject() {
      return WhitelistVehicle.builder()
          .vrn("CAS310")
          .uploaderId(X_MODIFIER_ID)
          .uploaderEmail(MODIFIER_EMAIL)
          .build();
    }
  }

  @Nested
  class Delete {

    @Nested
    class WhenVehicleIsAbsentInDatabase {

      @Test
      public void shouldThrowVehicleNotFoundException() {
        // given
        String vrn = "ABC12DE";
        mockVehicleAbsenceInDatabase(vrn);

        // when
        Throwable throwable = catchThrowable(() -> whitelistService.deleteBy(vrn, X_MODIFIER_ID, MODIFIER_EMAIL));

        // then
        assertThat(throwable).isInstanceOf(VehicleNotFoundException.class)
            .hasMessage("Vehicle not found");
        verify(whitelistVehiclePostgresRepository, never()).deleteByVrnsIn(anySet());
      }

      private void mockVehicleAbsenceInDatabase(String vrn) {
        when(whitelistVehiclePostgresRepository.findOneByVrn(vrn)).thenReturn(Optional.empty());
      }
    }

    @Nested
    class WhenVehicleIsPresentInDatabase {

      @Test
      public void shouldCallRepositoryToDeleteVehicle() {
        // given
        String vrn = "ABC12DE";
        mockVehiclePresenceInDatabase(vrn);

        // when
        whitelistService.deleteBy(vrn, X_MODIFIER_ID, MODIFIER_EMAIL);

        // then
        verify(registerService)
            .register(conversionResultsArgumentCaptor.capture(), eq(X_MODIFIER_ID), eq(MODIFIER_EMAIL));
        ConversionResults conversionResults = conversionResultsArgumentCaptor.getValue();
        assertThat(conversionResults.getVrnToDelete()).hasSize(1);
        assertThat(conversionResults.getWhitelistVehiclesToSaveOrUpdate()).isEmpty();
        assertThat(conversionResults.getValidationErrors()).isEmpty();
      }

      private void mockVehiclePresenceInDatabase(String vrn) {
        when(whitelistVehiclePostgresRepository.findOneByVrn(vrn)).thenReturn(
            Optional.of(WhitelistVehicle.builder().vrn(vrn).build())
        );
      }
    }
  }
}