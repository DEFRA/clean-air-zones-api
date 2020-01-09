package uk.gov.caz.correlationid;

import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "uk.gov.caz.correlationid")
public class CorrelationIdProperties {

  private List<String> includedPathPatterns = Collections.singletonList("/v1/**");
}
