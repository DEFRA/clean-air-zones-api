package uk.gov.caz;

/**
 * Class that is used to distinguish between our custom exceptions and JVM related exceptions.
 */
public class ApplicationRuntimeException extends RuntimeException {
  private final String message;

  /**
   * Constructs instance of {@link ApplicationRuntimeException} with given message.
   */
  public ApplicationRuntimeException(String message) {
    super(message);
    this.message = message;
  }

  /**
   * Provides message that was used when constructing instance of this class.
   */
  public String message() {
    return message;
  }
}
