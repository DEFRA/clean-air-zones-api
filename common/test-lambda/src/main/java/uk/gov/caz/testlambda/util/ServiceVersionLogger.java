package uk.gov.caz.testlambda.util;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.GitProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ServiceVersionLogger {

  private final GitProperties gitProperties;

  public ServiceVersionLogger(@Autowired(required = false) GitProperties gitProperties) {
    this.gitProperties = gitProperties;
  }

  /**
   * Logs information of the service version (git commit sha).
   */
  @PostConstruct
  public void logProjectVersion() {
    log.info("Service version: {}", gitProperties == null
        ? "unknown"
        : gitProperties.getCommitId()
    );
  }
}
