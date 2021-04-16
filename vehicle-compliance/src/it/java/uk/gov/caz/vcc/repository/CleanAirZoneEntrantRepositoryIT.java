package uk.gov.caz.vcc.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static uk.gov.caz.vcc.util.CleanAirZoneEntrantAssert.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionSystemException;
import uk.gov.caz.vcc.annotation.IntegrationTest;
import uk.gov.caz.vcc.domain.ChargeValidity;
import uk.gov.caz.vcc.domain.CleanAirZoneEntrant;

@IntegrationTest
public class CleanAirZoneEntrantRepositoryIT {

  private ChargeValidity persistedChargeValidity;

  @Autowired
  private CleanAirZoneEntrantRepository cleanAirZoneEntrantRepository;

  @Autowired
  private ChargeValidityRepository chargeValidityRepository;

  @BeforeEach
  public void setup() {
    ChargeValidity chargeValidity = new ChargeValidity("SOC01");
    chargeValidity.setValidityCodeDesc("THIS IS SAMPLE VALIDITY CODE DESC");

    persistedChargeValidity = chargeValidityRepository.save(chargeValidity);
  }

  @AfterEach
  public void cleanup() {
    cleanAirZoneEntrantRepository.deleteAll();
    chargeValidityRepository.delete(persistedChargeValidity);
  }

  @Test
  public void shouldPersistCleanAirZoneEntrant() {
    //given
    ChargeValidity chargeValidity = getChargeValidity();
    CleanAirZoneEntrant cleanAirZoneEntrant = buildCleanAirZoneEntrant(chargeValidity, "vrn123");

    //when
    CleanAirZoneEntrant persistedCleanAirZoneEntrant = cleanAirZoneEntrantRepository
        .save(cleanAirZoneEntrant);

    //then
    Iterable<CleanAirZoneEntrant> all = cleanAirZoneEntrantRepository.findAll();
    CleanAirZoneEntrant entrantFromDb = all.iterator().next();

    assertThat(all).hasSize(1);

    //check fields equality
    assertThat(persistedCleanAirZoneEntrant.getEntrantId()).isNotNull();

    assertThat(entrantFromDb)
        .hasChargeValidityCode(chargeValidity.getChargeValidityCode())
        .hasCleanAirZoneId(cleanAirZoneEntrant.getCleanAirZoneId())
        .hasCorrelationId(cleanAirZoneEntrant.getCorrelationId())
        .hasVrn(cleanAirZoneEntrant.getVrn())
        .hasEntrantPaymentId(cleanAirZoneEntrant.getEntrantPaymentId())
        .hasEntrantTimestamp(cleanAirZoneEntrant.getEntrantTimestamp())
        .hasInsertTimestamp(cleanAirZoneEntrant.getInsertTimestamp())
        .insertTimestampIsAfter(LocalDateTime.now().minusMinutes(1));
  }

  @Test
  public void shouldPersistVrnWithFifteenCharacters() {
    // given
    CleanAirZoneEntrant cleanAirZoneEntrant = buildCleanAirZoneEntrant(getChargeValidity(),
        "vrn123456789012");

    // when
    CleanAirZoneEntrant persistedCleanAirZoneEntrant = cleanAirZoneEntrantRepository
        .save(cleanAirZoneEntrant);

    // then
    assertThat(persistedCleanAirZoneEntrant.getVrn()).isEqualTo("vrn123456789012");
  }

  @Test
  public void shouldThrowExceptionWhenVrnIsLongerThanNineCharacters() {
    // given
    CleanAirZoneEntrant cleanAirZoneEntrant = buildCleanAirZoneEntrant(getChargeValidity(),
        "vrn1234567890123");

    // when
    Throwable thrown = catchThrowable(
        () -> cleanAirZoneEntrantRepository.save(cleanAirZoneEntrant));
    //then
    assertThat(thrown).isInstanceOf(TransactionSystemException.class);
    assertThat(thrown).hasStackTraceContaining(ConstraintViolationException.class.getName());
  }

  private CleanAirZoneEntrant buildCleanAirZoneEntrant(ChargeValidity chargeValidity, String vrn) {
    UUID cleanAirZoneId = UUID.randomUUID();
    String correlationId = UUID.randomUUID().toString();
    LocalDateTime entrantTimestamp = LocalDateTime.now();

    CleanAirZoneEntrant cleanAirZoneEntrant = new CleanAirZoneEntrant(
        cleanAirZoneId, correlationId, entrantTimestamp
    );

    cleanAirZoneEntrant.setChargeValidityCode(chargeValidity);
    cleanAirZoneEntrant.setVrn(vrn);
    cleanAirZoneEntrant.setEntrantPaymentId(UUID.randomUUID());
    return cleanAirZoneEntrant;
  }

  private ChargeValidity getChargeValidity() {
    return chargeValidityRepository.findAll().iterator().next();
  }
}