package uk.gov.caz.vcc.dto;

import com.google.common.base.Preconditions;
import java.util.Map;
import lombok.Value;
import org.springframework.http.HttpStatus;

/**
 * Helper class that stores results from bulk-check NTR search call.
 */
@Value
public class LicencesInformation {
  Map<String, TaxiPhvLicenseInformationResponse> byVrn;

  // present in case of error
  HttpStatus httpStatus;
  String errorMessage;

  public static LicencesInformation success(
      Map<String, TaxiPhvLicenseInformationResponse> licencesInformation) {
    Preconditions.checkNotNull(licencesInformation, "'licencesInformation' cannot be null");
    return new LicencesInformation(licencesInformation, null, null);
  }

  public static LicencesInformation failure(HttpStatus httpStatus, String errorMessage) {
    return new LicencesInformation(null, httpStatus, errorMessage);
  }

  public boolean hasFailed() {
    return byVrn == null;
  }

  public TaxiPhvLicenseInformationResponse getLicenceInfoFor(String vrn) {
    return hasFailed() ? null : byVrn.get(vrn);
  }
}
