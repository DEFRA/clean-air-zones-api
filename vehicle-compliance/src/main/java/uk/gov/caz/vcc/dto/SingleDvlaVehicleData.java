package uk.gov.caz.vcc.dto;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.HttpStatus;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.vcc.repository.LicenseAndVehicleRemoteRepository;

/**
 * Helper class to carry results of {@link LicenseAndVehicleRemoteRepository#findDvlaVehicle(
 *java.lang.String, java.lang.String)} call.
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SingleDvlaVehicleData {

  Vehicle vehicle;

  // only present in case of failure
  HttpStatus httpStatus;
  String errorMessage;

  public static SingleDvlaVehicleData success(Vehicle vehicle) {
    return new SingleDvlaVehicleData(vehicle, null, null);
  }

  public static SingleDvlaVehicleData failure(HttpStatus httpStatus, String errorMessage) {
    return new SingleDvlaVehicleData(null, httpStatus, errorMessage);
  }

  public boolean hasFailed() {
    return vehicle == null;
  }
}
