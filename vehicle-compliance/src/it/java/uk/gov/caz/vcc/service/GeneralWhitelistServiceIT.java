package uk.gov.caz.vcc.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.caz.vcc.annotation.IntegrationTest;
import uk.gov.caz.vcc.domain.GeneralWhitelistVehicle;
import uk.gov.caz.vcc.repository.GeneralWhitelistRepository;

@IntegrationTest
public class GeneralWhitelistServiceIT {

  @Autowired
  private GeneralWhitelistRepository generalWhitelistRepository;

  @Autowired
  private GeneralWhitelistService generalWhitelistService;

  @AfterEach
  void post() {
    generalWhitelistRepository.deleteAll();
  }

  @Test
  public void exemptOnGeneralWhitelistTrueIfPresent() {
    generalWhitelistRepository.save(buildGeneralWhitelistVehicle(false, true));

    assertTrue(generalWhitelistService.exemptOnGeneralWhitelist("CAS300"));
  }

  @Test
  public void exemptOnGeneralWhitelistFalseIfCompliant() {
    generalWhitelistRepository.save(buildGeneralWhitelistVehicle(true, false));

    assertFalse(generalWhitelistService.exemptOnGeneralWhitelist("CAS300"));
  }

  @Test
  public void exemptOnGeneralWhitelistFalseIfAbsent() {
    assertFalse(generalWhitelistService.exemptOnGeneralWhitelist("CAS300"));
  }

  @Test
  public void compliantOnGeneralWhitelistTrueIfPresent() {
    generalWhitelistRepository.save(buildGeneralWhitelistVehicle(true, false));

    assertTrue(generalWhitelistService.compliantOnGeneralWhitelist("CAS300"));
  }

  @Test
  public void compliantOnGeneralWhitelistFalseIfExempt() {
    generalWhitelistRepository.save(buildGeneralWhitelistVehicle(false, true));

    assertFalse(generalWhitelistService.compliantOnGeneralWhitelist("CAS300"));
  }

  @Test
  public void compliantOnGeneralWhitelistFalseIfAbsent() {
    assertFalse(generalWhitelistService.compliantOnGeneralWhitelist("CAS300"));
  }

  @Test
  public void isOnGeneralPurposeWhitelistAndHasCategoryOtherWhenItsTrue() {
    List<GeneralWhitelistVehicle> gpwVehicles = Arrays
        .asList(buildGeneralWhitelistVehicle(false, true));

    assertTrue(generalWhitelistService
        .isOnGeneralPurposedWhitelistAndHasCategoryOther("CAS300", gpwVehicles));
  }

  @Test
  public void isNotOnGeneralPurposeWhitelistAndHasCategoryOtherWhenItsFalse() {
    List<GeneralWhitelistVehicle> gpwVehicles = Collections.emptyList();

    assertFalse(generalWhitelistService
        .isOnGeneralPurposedWhitelistAndHasCategoryOther("CAS300", gpwVehicles));
  }

  private GeneralWhitelistVehicle buildGeneralWhitelistVehicle(boolean isCompliant,
      boolean isExempt) {
    return GeneralWhitelistVehicle.builder()
        .vrn("CAS300")
        .reasonUpdated("For testing")
        .uploaderId(UUID.randomUUID())
        .updateTimestamp(LocalDateTime.now())
        .category("OTHER")
        .compliant(isCompliant)
        .exempt(isExempt)
        .build();
  }
}
