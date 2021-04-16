package uk.gov.caz.whitelist.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import uk.gov.caz.whitelist.model.registerjob.RegisterJobName;
import uk.gov.caz.whitelist.model.registerjob.RegisterJobTrigger;

class RegisterJobNameGeneratorTest {

  @Test
  public void prefixesJobSuffixUsingTrigger() {
    // given
    String jobSuffix = "job";

    // when
    RegisterJobName registerJobName = new RegisterJobNameGenerator()
        .generate(jobSuffix, RegisterJobTrigger.WHITELIST_CSV_FROM_S3);

    // then
    assertThat(registerJobName.getValue().matches("\\w{8}_\\w{6}_WHITELIST_CSV_FROM_S3_job"))
        .isTrue();
  }

  @Test
  public void whenSuffixIsEmptyItDoesNotAppendTrailingUnderscore() {
    // given
    String jobSuffix = "";

    // when
    RegisterJobName registerJobName = new RegisterJobNameGenerator()
        .generate(jobSuffix, RegisterJobTrigger.WHITELIST_CSV_FROM_S3);

    // then
    assertThat(registerJobName.getValue().matches("\\w{8}_\\w{6}_WHITELIST_CSV_FROM_S3")).isTrue();
  }

  @Test
  public void whenSuffixIsNullItDoesNotAppendTrailingUnderscore() {
    // given
    String jobSuffix = null;

    // when
    RegisterJobName registerJobName = new RegisterJobNameGenerator()
        .generate(jobSuffix, RegisterJobTrigger.WHITELIST_CSV_FROM_S3);

    // then
    assertThat(registerJobName.getValue().matches("\\w{8}_\\w{6}_WHITELIST_CSV_FROM_S3")).isTrue();
  }
}
