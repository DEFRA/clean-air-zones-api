package uk.gov.caz.taxiregister.util;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Custom filter which enforces the content type of a request to always be deemed application/json.
 */
@Component
public class ContentTypeFilter implements Filter {

  /**
   * Filter invocation method for request handling.
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException {
    chain.doFilter(new ContentTypeRequestWrapper((HttpServletRequest) request), response);
  }

  @Override
  public void init(FilterConfig filterConfig) {
  }

}
