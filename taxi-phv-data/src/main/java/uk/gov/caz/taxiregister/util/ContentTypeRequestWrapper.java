package uk.gov.caz.taxiregister.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * A request wrapper to force application/json as the content type for a
 * request.
 *
 */
public class ContentTypeRequestWrapper extends HttpServletRequestWrapper {

  /**
   * Default constructor.
   * 
   * @param request in-bound servlet request.
   */
  public ContentTypeRequestWrapper(HttpServletRequest request) {
    super(request);
  }

  /**
   * A forced override of the content-type such that json is always returned.
   */
  @Override
  public String getContentType() {
    return "application/json";
  }

  /**
   * Override for fetching a named header from a request and forcing
   * application/json as the content type.
   * 
   */
  @Override
  public String getHeader(String name) {
    if (name.equalsIgnoreCase("content-type")) {
      return "application/json";
    }

    return super.getHeader(name);
  }

}
