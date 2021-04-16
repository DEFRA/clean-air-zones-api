package uk.gov.caz.whitelist.repository;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.caz.whitelist.testutils.TestObjects.createRandomWhitelistVehicle;
import static uk.gov.caz.whitelist.testutils.TestObjects.createWhitelistVehicle;

import com.google.common.collect.Sets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.caz.whitelist.annotation.IntegrationTest;
import uk.gov.caz.whitelist.model.WhitelistVehicle;
import uk.gov.caz.whitelist.testutils.WhitelistVehicleTestRepository;

@IntegrationTest
class WhitelistVehiclePostgresRepositoryIT {

  public static final String VRN = "8839GF123";

  @Autowired
  private WhitelistVehiclePostgresRepository whitelistRepository;

  @Autowired
  private WhitelistVehicleTestRepository testRepository;

  @BeforeEach
  void setUp() {
    testRepository.deleteAll();
  }

  @Test
  void shouldInsertWhitelistVehicle() {
    //given
    WhitelistVehicle whitelistVehicle = createRandomWhitelistVehicle("8839GF123");
    //when
    whitelistRepository.saveOrUpdate(singleton(whitelistVehicle));
    //then
    List<WhitelistVehicle> allWhitelistVehicles = testRepository.findAll();
    WhitelistVehicle foundVehicle = allWhitelistVehicles.iterator().next();
    assertThat(allWhitelistVehicles).hasSize(1);
    assertThat(foundVehicle.getInsertTimestamp()).isNotNull();
  }

  @Test
  void shouldUpdateWhitelistVehicle() {
    //given
    WhitelistVehicle whitelistVehicle = createRandomWhitelistVehicle(VRN);
    whitelistRepository.saveOrUpdate(singleton(whitelistVehicle));

    String reasonUpdated = "reasonUpdated2";
    String manufacturer = "manufacturer2";
    UUID uploaderId = UUID.randomUUID();
    WhitelistVehicle expectedUpdatedVehicle = createWhitelistVehicle(reasonUpdated, manufacturer,
        uploaderId, "8839GF123", "Other", "test@gov.uk");

    //when
    whitelistRepository.saveOrUpdate(singleton(expectedUpdatedVehicle));

    //then
    WhitelistVehicle updatedVehicle = testRepository.findAll().iterator().next();
    assertThat(updatedVehicle.getVrn()).isEqualTo(whitelistVehicle.getVrn());
    assertThat(updatedVehicle.getUploaderEmail()).isEqualTo("test@gov.uk");
    assertWhitelistVehicle(updatedVehicle, expectedUpdatedVehicle);
  }

  @Test
  void updateShouldAffectUpdateTimestamp() {
    //given
    WhitelistVehicle whitelistVehicle = createRandomWhitelistVehicle(VRN);
    whitelistRepository.saveOrUpdate(singleton(whitelistVehicle));
    LocalDateTime previousUpdateTime = testRepository.findAll().iterator().next()
        .getUpdateTimestamp();

    String reasonUpdated = "reasonUpdated2";
    String manufacturer = "manufacturer2";
    UUID uploaderId = UUID.randomUUID();
    WhitelistVehicle expectedUpdatedVehicle = createWhitelistVehicle(reasonUpdated, manufacturer,
        uploaderId, "8839GF123", "Other", "test@gov.uk");

    //when
    whitelistRepository.saveOrUpdate(singleton(expectedUpdatedVehicle));

    //then
    WhitelistVehicle updatedVehicle = testRepository.findAll().iterator().next();
    assertThat(updatedVehicle.getUpdateTimestamp()).isAfter(previousUpdateTime);
  }

  @Test
  void updateShouldNotAffectInsertTimestamp() {
    //given
    WhitelistVehicle whitelistVehicle = createRandomWhitelistVehicle(VRN);
    whitelistRepository.saveOrUpdate(singleton(whitelistVehicle));
    LocalDateTime previousInsertTime = testRepository.findAll().iterator().next()
        .getInsertTimestamp();

    String reasonUpdated = "reasonUpdated2";
    String manufacturer = "manufacturer2";
    UUID uploaderId = UUID.randomUUID();
    WhitelistVehicle expectedUpdatedVehicle = createWhitelistVehicle(reasonUpdated, manufacturer,
        uploaderId, "8839GF123", "Other", "test@gov.uk");

    //when
    whitelistRepository.saveOrUpdate(singleton(expectedUpdatedVehicle));

    //then
    WhitelistVehicle updatedVehicle = testRepository.findAll().iterator().next();
    assertThat(updatedVehicle.getInsertTimestamp()).isEqualTo(previousInsertTime);
  }

  @Test
  void reCreateAfterDeleteShouldAffectInsertTimestamp() {
    //given
    //create whitelist vehicle
    WhitelistVehicle whitelistVehicle = createRandomWhitelistVehicle(VRN);
    whitelistRepository.saveOrUpdate(singleton(whitelistVehicle));
    LocalDateTime firstVehicleInsertTime = testRepository.findAll().iterator().next()
        .getInsertTimestamp();
    //delete first vehicle
    testRepository.deleteAll();

    //when
    whitelistRepository.saveOrUpdate(singleton(whitelistVehicle));

    //then
    LocalDateTime secondVehicleInsertTime = testRepository.findAll().iterator().next()
        .getInsertTimestamp();
    assertThat(firstVehicleInsertTime).isBefore(secondVehicleInsertTime);
  }

  @Test
  void shouldFindByVrn() {
    //given
    WhitelistVehicle whitelistVehicle = createRandomWhitelistVehicle(VRN);
    whitelistRepository.saveOrUpdate(singleton(whitelistVehicle));

    //when
    Optional<WhitelistVehicle> whitelistVehicleFromDb = whitelistRepository.findOneByVrn(VRN);

    //then
    assertThat(whitelistVehicleFromDb).isPresent();

    WhitelistVehicle foundVehicle = whitelistVehicleFromDb.get();
    assertWhitelistVehicle(foundVehicle, whitelistVehicle);
  }

  @Test
  void shouldFindByVrnReturnEmptyResultIfVrnNotFound() {
    //given
    //when
    Optional<WhitelistVehicle> whitelistVehicleFromDb = whitelistRepository
        .findOneByVrn("not existing vrn");

    //then
    assertThat(whitelistVehicleFromDb).isNotPresent();

  }

  @Test
  void shouldDeleteByVrns() {
    //given
    String vrn1 = "VRN1";
    String vrn2 = "VRN2";
    whitelistRepository.saveOrUpdate(
        Sets.newHashSet(createRandomWhitelistVehicle(vrn1), createRandomWhitelistVehicle(vrn2)));

    //when
    whitelistRepository.deleteByVrnsIn(Sets.newHashSet(vrn1, vrn2));

    //then
    assertThat(whitelistRepository.findOneByVrn(vrn1)).isNotPresent();
    assertThat(whitelistRepository.findOneByVrn(vrn2)).isNotPresent();

  }

  private void assertWhitelistVehicle(WhitelistVehicle actual,
      WhitelistVehicle expected) {
    assertThat(actual.getVrn()).isEqualTo(expected.getVrn());
    assertThat(actual.getReasonUpdated()).isEqualTo(expected.getReasonUpdated());
    assertThat(actual.getUploaderId()).isEqualTo(expected.getUploaderId());
    assertThat(actual.getUploaderEmail()).isEqualTo(expected.getUploaderEmail());
    assertThat(actual.getManufacturer()).isPresent().hasValue(expected.getManufacturer().get());
    assertThat(actual.getUpdateTimestamp()).isBefore(LocalDateTime.now().plusSeconds(1));
    assertThat(actual.getInsertTimestamp()).isBefore(LocalDateTime.now().plusSeconds(1));
  }
}