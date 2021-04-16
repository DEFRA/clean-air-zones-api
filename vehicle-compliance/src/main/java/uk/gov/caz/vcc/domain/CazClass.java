package uk.gov.caz.vcc.domain;

/**
 * Enumeration of CAZ tariffs/classes that can be implemented by 
 * a Local Authority against the national framework.
 *
 */
public enum CazClass {
  A,
  B,
  C,
  D;
  
  /**
   * Helper method for converting a char to a CAZ Class enum value.
   * @param cazClass the char identifier for a CAZ.
   * @return an enumeration value matching the supplied char.
   */
  public static CazClass fromChar(char cazClass) {
    return CazClass.valueOf(Character.toString(cazClass));
  }
}
