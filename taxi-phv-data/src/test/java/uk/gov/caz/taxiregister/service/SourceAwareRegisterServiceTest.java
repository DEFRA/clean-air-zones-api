package uk.gov.caz.taxiregister.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.testutils.TestObjects;
import uk.gov.caz.testutils.TestObjects.LicensingAuthorities;
import uk.gov.caz.testutils.TestObjects.Registration;

@ExtendWith(MockitoExtension.class)
class SourceAwareRegisterServiceTest {

  @Mock
  private RegisterCommandFactory registerCommandFactory;

  @InjectMocks
  private SourceAwareRegisterService sourceAwareRegisterService;

  @Mock
  private RegisterFromRestApiCommand restApiCommand;

  @Mock
  private RegisterFromCsvCommand csvCommand;

  @Nested
  class RestApi {

    @Test
    public void shouldGetAndExecuteCommandFromFactory() {
      // given
      given(registerCommandFactory.createRegisterFromRestApiCommand(
          anyList(), anyInt(), anyString(), any())).willReturn(restApiCommand);
      given(restApiCommand.execute()).willReturn(
          SuccessRegisterResult.with(LicensingAuthorities.existingAsSingleton()));

      // when
      sourceAwareRegisterService.register(
          Registration.VehicleDtos.toBeRegistered(),
          TestObjects.TYPICAL_UPLOADER_ID,
          TestObjects.API_CALL_REGISTER_JOB_ID,
          TestObjects.TYPICAL_CORRELATION_ID
      );

      // then
      verify(restApiCommand).execute();
      verifyNoMoreInteractions(restApiCommand);
    }
  }

  @Nested
  class Csv {

    @Test
    public void shouldGetAndExecuteCommandFromFactory() {
      // given
      given(registerCommandFactory.createRegisterFromCsvCommand(
          anyString(), anyString(), anyInt(), anyString())).willReturn(csvCommand);
      given(csvCommand.execute()).willReturn(
          SuccessRegisterResult.with(LicensingAuthorities.existingAsSingleton()));

      // when
      sourceAwareRegisterService.register(
          TestObjects.Registration.Csv.bucket(),
          TestObjects.Registration.Csv.filename(),
          TestObjects.Registration.Csv.registerJobId(),
          TestObjects.Registration.correlationId()
      );

      // then
      verify(csvCommand).execute();
      verifyNoMoreInteractions(csvCommand);
    }
  }

}