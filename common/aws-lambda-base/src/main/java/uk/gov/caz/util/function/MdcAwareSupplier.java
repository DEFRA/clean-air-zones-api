package uk.gov.caz.util.function;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.MDC;

/**
 * A delegate for {@link Supplier} which sets the calling thread's MDC context once the wrapped
 * supplier's {@link MdcAwareSupplier#get()} method is invoked. The context is cleared once the
 * delegate has finished its job.
 */
public class MdcAwareSupplier<T> implements Supplier<T> {

  private final Map<String, String> copyOfContextMap;
  private final Supplier<T> delegate;

  private MdcAwareSupplier(Supplier<T> delegate) {
    this.copyOfContextMap = MDC.getCopyOfContextMap();
    this.delegate = delegate;
  }

  /**
   * Static factory method for {@link MdcAwareSupplier}.
   */
  public static <T> MdcAwareSupplier<T> from(Supplier<T> delegate) {
    return new MdcAwareSupplier<>(delegate);
  }

  @Override
  public T get() {
    MDC.setContextMap(copyOfContextMap == null ? Collections.emptyMap() : copyOfContextMap);
    try {
      return delegate.get();
    } finally {
      MDC.clear();
    }
  }
}
