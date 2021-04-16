package uk.gov.caz.taxiregister.tasks;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    value = "tasks.active-licences-in-reporting-window.enabled",
    havingValue = "true")
public class AnyTaskEnabled {

}
