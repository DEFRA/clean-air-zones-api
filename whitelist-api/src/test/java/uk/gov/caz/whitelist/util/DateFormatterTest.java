package uk.gov.caz.whitelist.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class DateFormatterTest {

  private DateFormatter dateFormatter = new DateFormatter();

  @Test
  public void shouldParseLocalDateTime() {
    // given
    LocalDateTime localDateTime = LocalDateTime.of(2020, 1, 23, 15, 32, 44);

    // when
    String formattedDate = dateFormatter.parse(localDateTime);

    // then
    assertThat(formattedDate).isEqualTo("2020-01-23 15:32:44");
  }
}