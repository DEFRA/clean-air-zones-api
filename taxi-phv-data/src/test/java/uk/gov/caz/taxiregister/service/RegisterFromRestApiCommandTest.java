package uk.gov.caz.taxiregister.service;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.caz.testutils.TestObjects.S3_REGISTER_JOB_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_CORRELATION_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.testutils.TestObjects;

class RegisterFromRestApiCommandTest {

  private RegisterFromRestApiCommand registerFromRestApiCommand;

  @Test
  public void shouldNotReturnAnyParseErrors() {
    // when
    registerFromRestApiCommand = new RegisterFromRestApiCommand(
        Collections.emptyList(), TYPICAL_REGISTER_JOB_UPLOADER_ID, S3_REGISTER_JOB_ID,
        new RegisterServicesContext(null, null, null, null, null, null, null, 0, 0, 0, null),
        TYPICAL_CORRELATION_ID
    );

    // then
    assertThat(registerFromRestApiCommand.getLicencesParseValidationErrors()).isEmpty();
  }

  @Test
  public void shouldReturnPassedUploaderId() {
    // given
    UUID uploaderId = TYPICAL_REGISTER_JOB_UPLOADER_ID;

    // when
    registerFromRestApiCommand = new RegisterFromRestApiCommand(
        Collections.emptyList(), uploaderId, S3_REGISTER_JOB_ID,
        new RegisterServicesContext(null, null, null, null, null, null, null, 0, 0, 0, null),
        TYPICAL_CORRELATION_ID
    );

    // then
    assertThat(registerFromRestApiCommand.getUploaderId()).isEqualTo(uploaderId);
  }

  @Test
  public void shouldReturnPassedLicencesToRegister() {
    // given
    List<VehicleDto> licences = TestObjects.Registration.VehicleDtos.toBeRegistered();

    // when
    registerFromRestApiCommand = new RegisterFromRestApiCommand(
        licences, TYPICAL_REGISTER_JOB_UPLOADER_ID, S3_REGISTER_JOB_ID,
        new RegisterServicesContext(null, null, null, null, null, null, null, 0, 0, 0, null),
        TYPICAL_CORRELATION_ID
    );

    // then
    assertThat(registerFromRestApiCommand.getLicencesToRegister()).isEqualTo(licences);
  }
}