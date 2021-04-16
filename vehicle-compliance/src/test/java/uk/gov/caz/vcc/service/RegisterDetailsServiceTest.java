package uk.gov.caz.vcc.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.caz.vcc.util.RegisterDetailsDtoAssert.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.vcc.domain.GeneralWhitelistVehicle;
import uk.gov.caz.vcc.dto.RegisterDetailsDto;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;
import uk.gov.caz.vcc.repository.RetrofitRepository;

@ExtendWith(MockitoExtension.class)
class RegisterDetailsServiceTest {

  @Mock
  private GeneralWhitelistService generalWhitelistService;

  @Mock
  private MilitaryVehicleService militaryVehicleService;

  @Mock
  private RetrofitRepository retrofitRepository;

  @Mock
  private NationalTaxiRegisterService nationalTaxiRegisterService;

  @InjectMocks
  private RegisterDetailsService registerDetailsService;


  @Nested
  class ShouldBeCompliant {

    @Test
    void shouldBeInRetrofitService() {
      // given
      when(retrofitRepository.existsByVrnIgnoreCase(any())).thenReturn(true);

      // when
      RegisterDetailsDto result = registerDetailsService.prepareRegisterDetails(any());

      // then
      assertThat(result)
          .isCompliant()
          .isInRetrofit()
          .isNotExempt()
          .isNotInMod()
          .isNotInNtr()
          .isNotInGpw();
    }

    @Test
    void shouldBeInGpwService() {
      // given
      GeneralWhitelistVehicle generalWhitelistVehicle = createWhitelistVehicle(true, false);
      when(generalWhitelistService.tryToFindFor(any())).thenReturn(Optional.of(generalWhitelistVehicle));

      // when
      RegisterDetailsDto result = registerDetailsService.prepareRegisterDetails(any());

      // then
      assertThat(result)
          .isCompliant()
          .isNotInRetrofit()
          .isNotExempt()
          .isNotInMod()
          .isNotInNtr()
          .isInGpw();
    }
  }

  @Nested
  class ShouldBeExempt {

    @Test
    void shouldBeInMod() {
      // given
      when(militaryVehicleService.isMilitaryVehicle(any())).thenReturn(true);

      // when
      RegisterDetailsDto result = registerDetailsService.prepareRegisterDetails(any());

      // then
      assertThat(result)
          .isNotCompliant()
          .isNotInRetrofit()
          .isExempt()
          .isInMod()
          .isNotInNtr()
          .isNotInGpw();
    }

    @Test
    void shouldBeInGpwService() {
      // given
      GeneralWhitelistVehicle generalWhitelistVehicle = createWhitelistVehicle(false, true);
      when(generalWhitelistService.tryToFindFor(any())).thenReturn(Optional.of(generalWhitelistVehicle));

      // when
      RegisterDetailsDto result = registerDetailsService.prepareRegisterDetails(any());

      // then
      assertThat(result)
          .isNotCompliant()
          .isNotInRetrofit()
          .isExempt()
          .isNotInMod()
          .isNotInNtr()
          .isInGpw();
    }
  }

  @Test
  void shouldBeInNtr() {
    // given
    when(nationalTaxiRegisterService.getLicenseInformation(any()))
        .thenReturn(Optional.of(TaxiPhvLicenseInformationResponse.builder().build()));

    // when
    RegisterDetailsDto result = registerDetailsService.prepareRegisterDetails(any());

    // then
    assertThat(result)
        .isNotCompliant()
        .isNotInRetrofit()
        .isNotExempt()
        .isNotInMod()
        .isInNtr()
        .isNotInGpw();
  }

  @Test
  void shouldBeExemptAndCompliantWhenIsInGpw() {
    // given
    GeneralWhitelistVehicle generalWhitelistVehicle = createWhitelistVehicle(true, true);
    when(generalWhitelistService.tryToFindFor(any())).thenReturn(Optional.of(generalWhitelistVehicle));

    // when
    RegisterDetailsDto result = registerDetailsService.prepareRegisterDetails(any());

    // then
    assertThat(result)
        .isCompliant()
        .isNotInRetrofit()
        .isExempt()
        .isNotInMod()
        .isNotInNtr()
        .isInGpw();
  }

  @Test
  void shouldBeExemptAndCompliantAndInEveryServiceExceptGpw() {
    // given
    when(nationalTaxiRegisterService.getLicenseInformation(any()))
        .thenReturn(Optional.of(TaxiPhvLicenseInformationResponse.builder().build()));
    when(nationalTaxiRegisterService.getLicenseInformation(any()))
        .thenReturn(Optional.of(TaxiPhvLicenseInformationResponse.builder().build()));
    when(militaryVehicleService.isMilitaryVehicle(any())).thenReturn(true);
    when(retrofitRepository.existsByVrnIgnoreCase(any())).thenReturn(true);

    // when
    RegisterDetailsDto result = registerDetailsService.prepareRegisterDetails(any());

    // then
    assertThat(result)
        .isCompliant()
        .isInRetrofit()
        .isExempt()
        .isInMod()
        .isInNtr()
        .isNotInGpw();
  }

  private GeneralWhitelistVehicle createWhitelistVehicle(boolean compliant, boolean exempt) {
    return GeneralWhitelistVehicle.builder().compliant(compliant).exempt(exempt).build();
  }
}