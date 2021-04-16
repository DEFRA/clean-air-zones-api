package uk.gov.caz.whitelist.service;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static uk.gov.caz.whitelist.model.Actions.CREATE;
import static uk.gov.caz.whitelist.model.Actions.DELETE;
import static uk.gov.caz.whitelist.model.Actions.UPDATE;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.whitelist.model.ConversionResult;
import uk.gov.caz.whitelist.model.ConversionResults;
import uk.gov.caz.whitelist.model.ValidationError;
import uk.gov.caz.whitelist.model.WhitelistVehicle;
import uk.gov.caz.whitelist.model.WhitelistVehicleCommand;
import uk.gov.caz.whitelist.repository.AuditingRepository;
import uk.gov.caz.whitelist.repository.WhitelistVehiclePostgresRepository;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

  private static final WhitelistVehicle ANY_WHITELIST_VEHICLE = WhitelistVehicle.builder()
      .vrn("8839GF")
      .build();

  private static final UUID ANY_UPLOADER_ID = UUID
      .fromString("c5052136-46b9-4a07-8051-7da01b5c84c5");
  private static final String ANY_UPLOADER_EMAIL = RandomStringUtils.randomAlphabetic(10);

  private static final WhitelistVehicleCommand INSERT_WHITELIST_VEHICLE_COMMAND = WhitelistVehicleCommand
      .builder()
      .whitelistVehicle(ANY_WHITELIST_VEHICLE)
      .action(CREATE.getActionCharacter())
      .build();

  private static final WhitelistVehicleCommand UPDATE_WHITELIST_VEHICLE_COMMAND = WhitelistVehicleCommand
      .builder()
      .whitelistVehicle(ANY_WHITELIST_VEHICLE)
      .action(UPDATE.getActionCharacter())
      .build();

  private static final WhitelistVehicleCommand DELETE_WHITELIST_VEHICLE_COMMAND = WhitelistVehicleCommand
      .builder()
      .whitelistVehicle(ANY_WHITELIST_VEHICLE)
      .action(DELETE.getActionCharacter())
      .build();

  private static final ConversionResult INSERT_RESULT = ConversionResult
      .success(INSERT_WHITELIST_VEHICLE_COMMAND);
  private static final ConversionResult UPDATE_RESULT = ConversionResult
      .success(UPDATE_WHITELIST_VEHICLE_COMMAND);
  private static final ConversionResult DELETE_RESULT = ConversionResult
      .success(DELETE_WHITELIST_VEHICLE_COMMAND);
  private static final ConversionResult IGNORED_FAILURE_RESULT = ConversionResult
      .failure(Collections.singletonList(ValidationError.valueError("ee", "aa")));
  private static final List<ConversionResult> ALL_RESULTS = Arrays.asList(
      IGNORED_FAILURE_RESULT,
      INSERT_RESULT,
      UPDATE_RESULT,
      DELETE_RESULT
  );


  @Mock
  private WhitelistVehiclePostgresRepository whitelistRepository;

  @Mock
  private AuditingRepository auditingRepository;

  private RegisterService registerService;

  @BeforeEach
  void setup() {
    registerService = new RegisterService(whitelistRepository, auditingRepository);
  }

  @Test
  void shouldRejectUploadingNullConversionResults() {
    //given
    ConversionResults conversionResults = null;

    //then
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> registerService.register(conversionResults, ANY_UPLOADER_ID,
              ANY_UPLOADER_EMAIL))
        .withMessage("ConversionResults cannot be null");
  }

  @Test
  void shouldRegisterWhitelistVehicles() {
    //given
    ConversionResults conversionResults = ConversionResults.from(ALL_RESULTS);

    //when
    registerService.register(conversionResults, ANY_UPLOADER_ID, ANY_UPLOADER_EMAIL);

    //then
    Mockito.verify(whitelistRepository, Mockito.times(1)).saveOrUpdate(any());
    Mockito.verify(whitelistRepository, Mockito.times(1)).deleteByVrnsIn(any());
  }

  @Test
  void shouldOnlyInsert() {
    //given
    ConversionResults conversionResults = ConversionResults.from(Collections.singletonList(
        INSERT_RESULT));

    //when
    registerService.register(conversionResults, ANY_UPLOADER_ID, ANY_UPLOADER_EMAIL);

    //then
    Mockito.verify(whitelistRepository, Mockito.times(1)).saveOrUpdate(any());
    Mockito.verify(whitelistRepository, Mockito.never()).deleteByVrnsIn(any());
  }

  @Test
  void shouldOnlyUpdate() {
    //given
    ConversionResults conversionResults = ConversionResults.from(Collections.singletonList(
        UPDATE_RESULT));

    //when
    registerService.register(conversionResults, ANY_UPLOADER_ID, ANY_UPLOADER_EMAIL);

    //then
    Mockito.verify(whitelistRepository, Mockito.times(1)).saveOrUpdate(any());
    Mockito.verify(whitelistRepository, Mockito.never()).deleteByVrnsIn(any());
  }


  @Test
  void shouldOnlyDelete() {
    //given
    ConversionResults conversionResults = ConversionResults.from(Collections.singletonList(
        DELETE_RESULT));

    //when
    registerService.register(conversionResults, ANY_UPLOADER_ID, ANY_UPLOADER_EMAIL);

    //then
    Mockito.verify(whitelistRepository, Mockito.never()).saveOrUpdate(any());
    Mockito.verify(whitelistRepository, Mockito.times(1)).deleteByVrnsIn(any());
  }
}