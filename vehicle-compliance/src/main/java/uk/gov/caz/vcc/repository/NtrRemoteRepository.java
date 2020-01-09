package uk.gov.caz.vcc.repository;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;

/**
 * Retrofit2 repository to create a ntr call.
 */
@Service
@AllArgsConstructor
public class NtrRemoteRepository {

  private final NtrRepository ntrRepository;

  /**
   * Method to creat retrofit2 ntr call.
   *
   * @param vrn to call
   * @return {@link Call} of{@link TaxiPhvLicenseInformationResponse}
   */
  public Call<TaxiPhvLicenseInformationResponse> getLicenseInfo(String correlationId, String vrn) {
    return ntrRepository.findByRegistrationNumber(correlationId, vrn);
  }
}
