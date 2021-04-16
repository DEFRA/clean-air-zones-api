package uk.gov.caz.vcc.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.vcc.domain.GeneralWhitelistVehicle;
import uk.gov.caz.vcc.dto.RegisterDetailsDto;
import uk.gov.caz.vcc.repository.RetrofitRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterDetailsService {

  private final GeneralWhitelistService generalWhitelistService;

  private final MilitaryVehicleService militaryVehicleService;

  private final RetrofitRepository retrofitRepository;

  private final NationalTaxiRegisterService nationalTaxiRegisterService;

  /**
   * Method that preparing response for register details endpoint.
   * @param vrn vehicle registration number of a vehicle
   * @return {@link RegisterDetailsDto}
   */
  public RegisterDetailsDto prepareRegisterDetails(String vrn) {

    boolean registeredRetrofit = registeredInRetrofit(vrn);

    Optional<GeneralWhitelistVehicle> generalWhitelistVehicle = tryToFindInGwp(vrn);

    boolean compliantOnGeneralWhitelist = generalWhitelistVehicle
        .map(GeneralWhitelistVehicle::isCompliant)
        .orElse(false);

    boolean exemptOnGeneralWhitelist = generalWhitelistVehicle
        .map(GeneralWhitelistVehicle::isExempt)
        .orElse(false);

    boolean registeredGpw = generalWhitelistVehicle.isPresent();

    boolean registeredMod = registeredInMod(vrn);

    boolean registeredNtr = registeredInNtr(vrn);

    boolean registerCompliant = registeredRetrofit || compliantOnGeneralWhitelist;

    boolean registerExempt = registeredMod || exemptOnGeneralWhitelist;

    return RegisterDetailsDto.builder()
        .registerCompliant(registerCompliant)
        .registerExempt(registerExempt)
        .registeredGpw(registeredGpw)
        .registeredMod(registeredMod)
        .registeredRetrofit(registeredRetrofit)
        .registeredNtr(registeredNtr)
        .build();
  }

  /**
   * Helper method to check if vehicle is in NTR.
   */
  private boolean registeredInNtr(String vrn) {
    return nationalTaxiRegisterService.getLicenseInformation(vrn).isPresent();
  }

  /**
   * Helper method to check if vehicle is in MOD.
   */
  private boolean registeredInMod(String vrn) {
    return militaryVehicleService.isMilitaryVehicle(vrn);
  }

  /**
   * Helper method to check if vehicle is in GWP.
   */
  private Optional<GeneralWhitelistVehicle> tryToFindInGwp(String vrn) {
    return generalWhitelistService
        .tryToFindFor(vrn);
  }

  /**
   * Helper method to check if vehicle is in Retrofit.
   */
  private boolean registeredInRetrofit(String vrn) {
    return retrofitRepository.existsByVrnIgnoreCase(vrn);
  }
}