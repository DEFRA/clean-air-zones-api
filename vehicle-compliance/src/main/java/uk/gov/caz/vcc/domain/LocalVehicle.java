package uk.gov.caz.vcc.domain;

import java.util.Calendar;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.NoArgsConstructor;
import uk.gov.caz.definitions.domain.Vehicle;
import uk.gov.caz.vcc.repository.LocalVehicleDetailsRepository;

/**
 * Spring JPA decorated definition of a vehicle (sourced from local SQL stores). 
 *
 */
@Entity
@Table(name = "t_vehicle", schema = "caz_test_harness")
@NoArgsConstructor
public class LocalVehicle extends Vehicle {

  private static final long serialVersionUID = -1818496912478156273L;


  /**
   * Constructor required by the {@link LocalVehicleDetailsRepository} to simulate response from the
   * remote vehicle API.
   */
  public LocalVehicle(String registrationNumber, String colour, Date dateOfFirstRegistration,
      String euroStatus, String typeApproval, Integer massInService, String bodyType, String make,
      String model, Integer revenueWeight, Integer seatingCapacity, Integer standingCapacity,
      String taxClass, String fuelType) {
    this.setRegistrationNumber(registrationNumber);
    this.setColour(colour);
    this.setDateOfFirstRegistration(setDateDayToOne(dateOfFirstRegistration));
    this.setEuroStatus(euroStatus);
    this.setTypeApproval(typeApproval);
    this.setMassInService(massInService);
    this.setBodyType(bodyType);
    this.setMake(make);
    this.setModel(model);
    this.setRevenueWeight(revenueWeight);
    this.setSeatingCapacity(seatingCapacity);
    this.setStandingCapacity(standingCapacity);
    this.setTaxClass(taxClass);
    this.setFuelType(fuelType);
  }
  
  private static Date setDateDayToOne(Date dateOfFirstRegistration) {
    if (dateOfFirstRegistration != null) {
      Calendar cal = Calendar.getInstance();
      cal.setTime(dateOfFirstRegistration);
      cal.set(Calendar.DATE, 1);
      return cal.getTime();
    }
    return dateOfFirstRegistration;
  }
}
