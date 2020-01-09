package uk.gov.caz;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Little helper class that wraps HTTP error code with body and can easily be returned as JSON or
 * XML.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement
class ErrorResponse {

  /**
   * HTTP error code.
   */
  @XmlElement
  private int status;

  /**
   * HTTP error body.
   */
  @XmlElement
  private String message;

  /**
   * Creates new instance of {@link ErrorResponse}.
   *
   * @param status HTTP error status code to return.
   * @param message HTTP error body to return.
   * @return New instance of {@link ErrorResponse} with code and body.
   */
  public static ErrorResponse from(int status, String message) {
    return new ErrorResponse(status, message);
  }
}
