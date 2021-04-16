package uk.gov.caz.whitelist.model.registerjob;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;
import uk.gov.caz.whitelist.model.CsvContentType;

class RegisterJobTriggerTest {

  @Test
  public void testMappingOfAllCsvContentTypeValues() {
    // this is to quickly catch case when we add new CsvContentType but forget to update
    // RegisterJobTrigger.from method
    for (CsvContentType csvContentType : CsvContentType.values()) {
      RegisterJobTrigger.from(csvContentType);
    }
  }

  @Test
  public void testThatCorrectlyMapsFromCsvContentTypes() {
    assertThat(RegisterJobTrigger.from(CsvContentType.WHITELIST_LIST))
        .isEqualByComparingTo(RegisterJobTrigger.WHITELIST_CSV_FROM_S3);
  }

  @Test
  public void testNullCsvContentType() {
    // given
    CsvContentType csvContentType = null;

    // when
    Throwable throwable = catchThrowable(() -> RegisterJobTrigger.from(csvContentType));

    // then
    BDDAssertions.then(throwable).isInstanceOf(UnsupportedOperationException.class);
  }
}