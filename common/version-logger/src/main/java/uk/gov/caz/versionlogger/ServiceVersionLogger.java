package uk.gov.caz.versionlogger;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.GitProperties;

@Slf4j
public class ServiceVersionLogger {

  private final GitProperties gitProperties;

  public ServiceVersionLogger(GitProperties gitProperties) {
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
