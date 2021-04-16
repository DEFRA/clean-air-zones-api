package uk.gov.caz.vcc.dto;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.HttpStatus;

/**
 * Value class that represents the result of a call to NTR.
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SingleLicenceData {

  // nullable
  TaxiPhvLicenseInformationResponse licence;

  // only present in case of failure
  HttpStatus httpStatus;
  String errorMessage;

  /**
   * Creates an instance of {@link SingleLicenceData} which contains the obtained licence
   * information. Additionally {@link SingleLicenceData#hasFailed()} returns false.
   */
  public static SingleLicenceData success(TaxiPhvLicenseInformationResponse licence) {
    return new SingleLicenceData(licence, null, null);
  }

  /**
   * Creates an instance of {@link SingleLicenceData} which {@link SingleLicenceData#hasFailed()}
   * returns true.
   */
  public static SingleLicenceData failure(HttpStatus httpStatus, String errorMessage) {
    Preconditions.checkNotNull(httpStatus);
    return new SingleLicenceData(null, httpStatus, Strings.nullToEmpty(errorMessage));
  }

  /**
   * Returns true if the operation to fetch the licence data failed, false otherwise.
   */
  public boolean hasFailed() {
    return httpStatus != null && errorMessage != null;
  }

  /**
   * Returns licence information wrapped in {@link Optional}.
   */
  public Optional<TaxiPhvLicenseInformationResponse> getLicence() {
    return Optional.ofNullable(licence);
  }
}
