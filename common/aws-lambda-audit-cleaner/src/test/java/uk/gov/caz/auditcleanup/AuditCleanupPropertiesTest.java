package uk.gov.caz.auditcleanup;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuditCleanupPropertiesTest {

  @Test
  public void shouldCheckDefaultCleanupDays() {
    //given
    int expectedDefault = 2555;

    //when
    int cleanupDays = new AuditCleanupProperties().getDays();

    //then
    assertThat(cleanupDays).isEqualTo(expectedDefault);
  }

  @Test
  public void shouldSuccessfullySetCleanupDays() {
    //given
    int target = 2137;

    //when
    AuditCleanupProperties auditCleanupProperties = new AuditCleanupProperties();
    auditCleanupProperties.setDays(target);

    //then
    assertThat(auditCleanupProperties.getDays()).isEqualTo(target);
  }
}