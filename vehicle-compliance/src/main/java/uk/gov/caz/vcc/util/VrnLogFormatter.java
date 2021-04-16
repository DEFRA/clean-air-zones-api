package uk.gov.caz.vcc.util;

import com.amazonaws.serverless.proxy.LogFormatter;
import com.amazonaws.serverless.proxy.internal.servlet.ApacheCombinedServletLogFormatter;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.SecurityContext;

/**
 * Custom LogFormatter used to remove URLs with VRN from the app logs.
 */
public class VrnLogFormatter<T1 extends HttpServletRequest,
    T2 extends HttpServletResponse> implements LogFormatter<T1, T2> {

  private static final List<String> LOGGING_BLACKLIST = Arrays.asList(
      "(.*)\\/v1\\/compliance-checker\\/vehicles\\/.*\\/details(.*)",
      "(.*)\\/v1\\/compliance-checker\\/vehicles\\/.*\\/compliance(.*)",
      "(.*)\\/v1\\/compliance-checker\\/vehicles\\/.*\\/external-details(.*)",
      "(.*)\\/v1\\/compliance-checker\\/vehicles\\/.*\\/register-details(.*)",
      "(.*)\\/v1\\/vehicles\\/.*\\/licence-info(.*)"
  );

  private final LogFormatter<T1, T2> delegate;

  public VrnLogFormatter() {
    this.delegate = new ApacheCombinedServletLogFormatter<>();
  }

  /**
   * Method which filters request with urls form the {@code BLACK_LIST} based on the {@link
   * ApacheCombinedServletLogFormatter} formatted message.
   */
  @Override
  public String format(T1 containerRequestType,
      T2 containerResponseType, SecurityContext securityContext) {
    String message = delegate
        .format(containerRequestType, containerResponseType, securityContext);
    return LOGGING_BLACKLIST.stream()
        .filter(message::matches)
        .findFirst()
        .map(match -> "Filtered Request " + containerRequestType.getMethod() + " url matches: "
            + match)
        .orElse(message);
  }
}