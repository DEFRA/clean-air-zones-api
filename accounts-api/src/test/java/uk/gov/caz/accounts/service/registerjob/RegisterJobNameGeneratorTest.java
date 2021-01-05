package uk.gov.caz.accounts.service.registerjob;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import uk.gov.caz.accounts.model.registerjob.RegisterJobName;
import uk.gov.caz.accounts.model.registerjob.RegisterJobTrigger;

class RegisterJobNameGeneratorTest {

  private final RegisterJobNameGenerator registerJobNameGenerator = new RegisterJobNameGenerator();

  @Test
  public void prefixesJobSuffixUsingTrigger() {
    // given
    String jobSuffix = "job";

    // when
    RegisterJobName registerJobName = registerJobNameGenerator
        .generate(jobSuffix, RegisterJobTrigger.CSV_FROM_S3);

    // then
    assertThat(registerJobName.getValue().matches("\\w{8}_\\w{9}_CSV_FROM_S3_job"))
        .isTrue();
  }

  @Test
  public void whenSuffixIsEmptyItDoesNotAppendTrailingUnderscore() {
    // given
    String jobSuffix = "";

    // when
    RegisterJobName registerJobName = registerJobNameGenerator
        .generate(jobSuffix, RegisterJobTrigger.CSV_FROM_S3);

    // then
    assertThat(registerJobName.getValue().matches("\\w{8}_\\w{9}_CSV_FROM_S3")).isTrue();
  }

  @Test
  public void whenSuffixIsNullItDoesNotAppendTrailingUnderscore() {
    // given
    String jobSuffix = null;

    // when
    RegisterJobName registerJobName = registerJobNameGenerator
        .generate(jobSuffix, RegisterJobTrigger.CSV_FROM_S3);

    // then
    assertThat(registerJobName.getValue().matches("\\w{8}_\\w{9}_CSV_FROM_S3")).isTrue();
  }
}
