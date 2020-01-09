package uk.gov.caz.util.function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
class MdcAwareSupplierTest {

  @Mock
  private Supplier<String> delegate;

  @InjectMocks
  private MdcAwareSupplier<String> mdcAwareSupplier;

  @BeforeEach
  public void setUp() {
    MDC.clear();
    mdcAwareSupplier = MdcAwareSupplier.from(delegate);
  }

  @AfterEach
  public void tearDown() {
    MDC.clear();
  }

  @Test
  public void shouldCallDelegate() {
    // given

    // when
    mdcAwareSupplier.get();

    // then
    verify(delegate).get();
  }

  @Test
  public void shouldSetCallingThreadMdcContext() throws ExecutionException, InterruptedException {
    // given
    Supplier<String> verifyingSupplier = () -> {
      String aValue = MDC.get("a");
      return aValue;
    };
    Map<String, String> context = ImmutableMap.of("a", "b");
    MDC.setContextMap(context);
    mdcAwareSupplier = MdcAwareSupplier.from(verifyingSupplier);

    // when
    CompletableFuture<String> withCopiedMdcContext = CompletableFuture
        .supplyAsync(mdcAwareSupplier);
    CompletableFuture<String> withEmptyMdcContext = CompletableFuture
        .supplyAsync(verifyingSupplier);

    // then
    Awaitility
        .await()
        .atMost(1, TimeUnit.SECONDS)
        .until(() -> withCopiedMdcContext.isDone() && withEmptyMdcContext.isDone());

    assertThat(withCopiedMdcContext.get()).isEqualTo(context.get("a"));
    assertThat(withEmptyMdcContext.get()).isNull();
  }
}