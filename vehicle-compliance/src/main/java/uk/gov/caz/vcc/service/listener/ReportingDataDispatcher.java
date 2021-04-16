package uk.gov.caz.vcc.service.listener;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import uk.gov.caz.vcc.domain.events.VehicleEntrantListPersistedEvent;
import uk.gov.caz.vcc.messaging.MessagingClient;

/**
 * A listener class for the @{Link {@link VehicleEntrantPersistedEvent} event.
 */
@Component
@RequiredArgsConstructor
public class ReportingDataDispatcher
    implements ApplicationListener<VehicleEntrantListPersistedEvent> {

  private final MessagingClient messagingClient;

  @Override
  public void onApplicationEvent(VehicleEntrantListPersistedEvent event) {
    messagingClient.publishMessage(event.getVehicleEntrants());
  }
}
