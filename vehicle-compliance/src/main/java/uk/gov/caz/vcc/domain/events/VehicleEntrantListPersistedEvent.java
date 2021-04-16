package uk.gov.caz.vcc.domain.events;

import java.util.List;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import uk.gov.caz.vcc.dto.VehicleEntrantReportingRequest;

/**
 * A wrapper used to signify a series of Vehicle Entrants that have
 * been saved to local data stores. 
 *
 */
@Getter
public class VehicleEntrantListPersistedEvent extends ApplicationEvent {
  
  private static final long serialVersionUID = 1L;
  private List<VehicleEntrantReportingRequest> vehicleEntrants;

  /**
   * Public constructor for @{link VehicleEntrantListPersistedEvent}.
   * @param source the class the event was created from
   * @param vehicleEntrants is a list of @{link VehicleEntrantReportingRequest}
   */
  public VehicleEntrantListPersistedEvent(Object source, 
      List<VehicleEntrantReportingRequest> vehicleEntrants) {
    super(source);
    this.vehicleEntrants = vehicleEntrants;
  }
}
