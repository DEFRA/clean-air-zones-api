package uk.gov.caz.vcc.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.vcc.annotation.IntegrationTest;
import uk.gov.caz.vcc.domain.LocalVehicle;

@IntegrationTest
public class LocalVehicleDetailsRepositoryIT {

  @Autowired
  private LocalVehicleDetailsRepository localVehicleDetailsRepository;

  private static final String ANY_VRN = "CAS321";

  @Test
  public void shouldReturnTheExistingVehicleWithStrippedDownLeadingZeros() {
    // given
    createSampleVehicle();
    String vrnWithLeadingZeros = "000" + ANY_VRN;

    // when
    Optional<Vehicle> vehicle = localVehicleDetailsRepository
        .findByRegistrationNumber(vrnWithLeadingZeros);

    // then
    assertThat(vehicle.get().getRegistrationNumber()).isEqualTo(ANY_VRN);
    assertThat(vehicle.get().getDateOfFirstRegistration().equals(getDateForSampleVehicle()));
  }

  private void createSampleVehicle() {
    LocalVehicle vehicle = new LocalVehicle();
    vehicle.setRegistrationNumber(ANY_VRN);
    vehicle.setColour("red");
    Calendar cal = Calendar.getInstance();
    cal.set(2012, 12 - 1, 17, 0, 0);
    vehicle.setDateOfFirstRegistration(cal.getTime());
    localVehicleDetailsRepository.save(vehicle);
  }
  
  private Date getDateForSampleVehicle() {
    Calendar cal = Calendar.getInstance();
    cal.set(2012, 12 - 1, 1, 0, 0);
    return cal.getTime();
  }
}
