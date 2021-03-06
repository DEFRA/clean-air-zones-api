package uk.gov.caz.vcc.util;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.GitProperties;
import org.springframework.stereotype.Component;

/**
 * Utility class to log the commit hash associated to a build.
 *
 */
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