package uk.gov.caz.vcc.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.vcc.dto.LicencesInformation;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;
import uk.gov.caz.vcc.repository.NationalTaxiRegisterRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class NationalTaxiRegisterService {

  private final NationalTaxiRegisterRepository nationalTaxiRegisterRepository;

  /**
   * Get license info for a vehicle from the National Taxi Register.
   *
   * @param vrn The vehicle registration number.
   * @return A LicenseInfoResponse (optional).
   */
  public Optional<TaxiPhvLicenseInformationResponse> getLicenseInformation(String vrn) {
    return nationalTaxiRegisterRepository.getLicenseInfo(vrn);
  }

  public LicencesInformation getLicensesInformation(Collection<String> vrns) {
    return nationalTaxiRegisterRepository.getLicensesInformation(vrns);
  }

  /**
   * Method for evicting cached licenseInfo.
   */
  public void cacheEvictLicenses(List<String> vrms) {
    if (vrms.isEmpty()) {
      return;
    }
    
    nationalTaxiRegisterRepository.cacheEvictLicenseInfo();
  }
}