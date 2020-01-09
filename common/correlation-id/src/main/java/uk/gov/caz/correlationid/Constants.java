package uk.gov.caz.correlationid;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

  //logback-spring.xml relies on the value of this field. DO NOT TOUCH.
  public static final String X_CORRELATION_ID_HEADER = "X-Correlation-ID";
}
