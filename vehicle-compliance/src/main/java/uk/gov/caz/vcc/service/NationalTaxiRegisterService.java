package uk.gov.caz.vcc.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;
import uk.gov.caz.vcc.repository.NationalTaxiRegisterRepository;

@Service
public class NationalTaxiRegisterService {

  private final NationalTaxiRegisterRepository nationalTaxiRegisterRepository;

  public NationalTaxiRegisterService(
      NationalTaxiRegisterRepository nationalTaxiRegisterRepository) {
    this.nationalTaxiRegisterRepository = nationalTaxiRegisterRepository;
  }

  /**
   * Get license info for a vehicle from the National Taxi Register.
   *
   * @param vrn The vehicle registration number.
   * @return A LicenseInfoResponse (optional).
   */
  public Optional<TaxiPhvLicenseInformationResponse> getLicenseInformation(String vrn) {
    return nationalTaxiRegisterRepository.getLicenseInfo(vrn);
  }

  /**
   * Method for evicting cached licenseInfo.
   */
  public void cacheEvictLicenses(List<String> vrms) {
    if (vrms.stream().count() == 0) {
      return;
    }
    
    nationalTaxiRegisterRepository.cacheEvictLicenseInfo();
  }
}