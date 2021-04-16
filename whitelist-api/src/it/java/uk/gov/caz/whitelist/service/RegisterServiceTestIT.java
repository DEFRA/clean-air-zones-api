package uk.gov.caz.whitelist.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.caz.whitelist.annotation.IntegrationTest;
import uk.gov.caz.whitelist.model.Actions;
import uk.gov.caz.whitelist.model.ConversionResult;
import uk.gov.caz.whitelist.model.ConversionResults;
import uk.gov.caz.whitelist.model.WhitelistVehicle;
import uk.gov.caz.whitelist.model.WhitelistVehicleCommand;
import uk.gov.caz.whitelist.model.WhitelistVehicleHistory;
import uk.gov.caz.whitelist.repository.WhitelistVehicleHistoryPostgresRepository;

@IntegrationTest
public class RegisterServiceTestIT {

  @Autowired
  private RegisterService registerService;

  @Autowired
  private WhitelistVehicleHistoryPostgresRepository repository;

  @Test
  public void registerAddsUploaderEmailToAuditTable() {
    // given
    String uploaderEmail = RandomStringUtils.randomAlphabetic(10);
    String vrn = RandomStringUtils.randomAlphabetic(10);
    ConversionResults conversionResults = fullCrudConversionForVrn(vrn);

    // when
    registerService.register(conversionResults, UUID.randomUUID(), uploaderEmail);

    // then
    List<WhitelistVehicleHistory> historyItems = repository
        .findByVrnInRange(vrn, LocalDateTime.MIN, LocalDateTime.MAX, 10, 0);
    assertThat(historyItems).hasSize(2);
    assertThat(historyItems.get(0).getModifierEmail()).isEqualTo(uploaderEmail);
    assertThat(historyItems.get(1).getModifierEmail()).isEqualTo(uploaderEmail);
  }

  private ConversionResults fullCrudConversionForVrn(String vrn) {
    WhitelistVehicle vehicle = WhitelistVehicle.builder()
        .vrn(vrn)
        .reasonUpdated("because_I_can")
        .uploaderId(UUID.randomUUID())
        .category("category")
        .build();
    WhitelistVehicleCommand insertCommand = WhitelistVehicleCommand.builder()
        .whitelistVehicle(vehicle)
        .action(Actions.CREATE.getActionCharacter())
        .build();
    WhitelistVehicleCommand deleteCommand = WhitelistVehicleCommand.builder()
        .whitelistVehicle(vehicle)
        .action(Actions.DELETE.getActionCharacter())
        .build();
    return ConversionResults.from(Arrays.asList(
        ConversionResult.success(insertCommand),
        ConversionResult.success(deleteCommand)
    ));
  }

}
