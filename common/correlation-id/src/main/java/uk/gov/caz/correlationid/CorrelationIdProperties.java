package uk.gov.caz.correlationid;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "uk.gov.caz.correlationid")
public class CorrelationIdProperties {

  private List<String> includedPathPatterns = ImmutableList.of("/v1/**", "/v2/**");
}