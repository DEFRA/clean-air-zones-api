package uk.gov.caz.vcc.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.caz.vcc.annotation.IntegrationTest;
import uk.gov.caz.vcc.domain.ChargeValidity;

@IntegrationTest
class ChargeValidityRepositoryIT {

  private static final String SAMPLE_CHARGE_VALIDITY_CODE = "SOC01";

  private static final String SAMPLE_VALIDITY_CODE_DESC = "THIS IS SAMPLE VALIDITY CODE DESC";

  @Autowired
  private ChargeValidityRepository chargeValidityRepository;

  @AfterEach
  public void cleanup() {
    chargeValidityRepository.deleteById(SAMPLE_CHARGE_VALIDITY_CODE);
  }

  @Test
  public void shouldPersistChargeValidity() {
    //given
    ChargeValidity chargeValidity = new ChargeValidity(SAMPLE_CHARGE_VALIDITY_CODE);
    chargeValidity.setValidityCodeDesc(SAMPLE_VALIDITY_CODE_DESC);

    //when
    chargeValidity = chargeValidityRepository.save(chargeValidity);

    //then
    ChargeValidity chargeValidityFromDb = chargeValidityRepository
        .findById(SAMPLE_CHARGE_VALIDITY_CODE).get();

    assertThat(chargeValidityFromDb.getChargeValidityCode())
        .isEqualTo(chargeValidity.getChargeValidityCode());
    assertThat(chargeValidityFromDb.getValidityCodeDesc())
        .isEqualTo(chargeValidity.getValidityCodeDesc());
    assertThat(chargeValidityFromDb.getInsertTimestamp())
        .isEqualTo(chargeValidity.getInsertTimestamp());

    //check generated insert timestamp
    assertThat(chargeValidityFromDb.getInsertTimestamp())
        .isAfter(LocalDateTime.now().minusMinutes(1));
  }
}