package uk.gov.caz.correlationid;

import java.util.Map;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;

/**
 * A local helper class that exposes a non-static interface for {@link MDC} operations. This is
 * mainly used for tests so that there is no need to include PowerMock.
 */
class MdcAdapter implements MDCAdapter {

  @Override
  public void put(String key, String val) {
    MDC.put(key, val);
  }

  @Override
  public String get(String key) {
    return MDC.get(key);
  }

  @Override
  public void remove(String key) {
    MDC.remove(key);
  }

  @Override
  public void clear() {
    MDC.clear();
  }

  @Override
  public Map<String, String> getCopyOfContextMap() {
    return MDC.getCopyOfContextMap();
  }

  @Override
  public void setContextMap(Map<String, String> map) {
    MDC.setContextMap(map);
  }
}
