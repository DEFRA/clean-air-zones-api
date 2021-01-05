package uk.gov.caz.accounts.controller.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.caz.accounts.controller.exception.VehicleRetrievalDtoValidationException;

class QueryStringValidatorTest {

  private QueryStringValidator queryStringValidator = new QueryStringValidator();

  private static final String PAGE_SIZE_QUERY_PARAM = "pageSize";
  private static final String PAGE_NUMBER_QUERY_PARAM = "pageNumber";
  private static final String CHARGEABLE_CAZ_ID = "chargeableCazId";

  @Test
  public void shouldThrowExceptionWhenPageSizeIsNotANumber() {
    // given
    List<String> requiredParams = Collections.singletonList(PAGE_SIZE_QUERY_PARAM);
    Map<String, String> params = Collections.singletonMap(PAGE_SIZE_QUERY_PARAM, "apple");

    // when
    Throwable throwable = catchThrowable(() ->
        queryStringValidator.validateRequest(params, requiredParams));

    // then
    assertThat(throwable).isInstanceOf(VehicleRetrievalDtoValidationException.class);
  }

  @Test
  public void shouldThrowExceptionWhenChargeableCazIdIsNotAUuid() {
    // given
    List<String> requiredParams = Collections.singletonList(CHARGEABLE_CAZ_ID);
    Map<String, String> params = Collections.singletonMap(CHARGEABLE_CAZ_ID, "apple");

    // when
    Throwable throwable = catchThrowable(
        () -> queryStringValidator.validateRequest(params, requiredParams));

    // then
    assertThat(throwable).isInstanceOf(VehicleRetrievalDtoValidationException.class);
  }

  @Test
  public void shouldThrowExceptionWhenChargeableCazIdKeyIsPresentButHasNoValue() {
    // given
    List<String> requiredParams = Collections.singletonList(CHARGEABLE_CAZ_ID);
    Map<String, String> params = Collections.singletonMap(CHARGEABLE_CAZ_ID, null);

    // when
    Throwable throwable = catchThrowable(
        () -> queryStringValidator.validateRequest(params, requiredParams));

    // then
    assertThat(throwable).isInstanceOf(VehicleRetrievalDtoValidationException.class);
  }

  @Test
  public void shouldThrowExceptionWhenAnyOtherRequiredParamIsNotANumber() {
    // given
    List<String> requiredParams = Arrays
        .asList(PAGE_SIZE_QUERY_PARAM, PAGE_NUMBER_QUERY_PARAM, CHARGEABLE_CAZ_ID);

    Map<String, String> params = new HashMap<String, String>() {{
      put(PAGE_SIZE_QUERY_PARAM, "32");
      put(PAGE_NUMBER_QUERY_PARAM, "apple");
      put(CHARGEABLE_CAZ_ID, UUID.randomUUID().toString());
    }};

    // when
    Throwable throwable = catchThrowable(
        () -> queryStringValidator.validateRequest(params, requiredParams));

    // then
    assertThat(throwable).isInstanceOf(VehicleRetrievalDtoValidationException.class);
  }

  @Test
  public void shouldNotThrowExceptionWhenValidationIsSuccessful() {
    // given
    List<String> requiredParams = Arrays
        .asList(PAGE_SIZE_QUERY_PARAM, PAGE_NUMBER_QUERY_PARAM, CHARGEABLE_CAZ_ID);

    Map<String, String> params = new HashMap<String, String>() {{
      put(PAGE_SIZE_QUERY_PARAM, "32");
      put(PAGE_NUMBER_QUERY_PARAM, "23");
      put(CHARGEABLE_CAZ_ID, UUID.randomUUID().toString());
    }};

    // when
    Throwable throwable = catchThrowable(
        () -> queryStringValidator.validateRequest(params, requiredParams));

    // then
    assertThat(throwable).isNull();
  }
}