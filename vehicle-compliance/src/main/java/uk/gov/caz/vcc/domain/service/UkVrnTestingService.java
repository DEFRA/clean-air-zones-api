package uk.gov.caz.vcc.domain.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;


/**
 * Helper class to determine whether a VRN is a potential UK number plate.
 *
 */
@Slf4j
public class UkVrnTestingService {
  
  /**
   * Private constructor for static class methods.
   */
  private UkVrnTestingService() {
  }
  
  @VisibleForTesting
  static final int MAX_LENGTH = 7;

  public static final String REGEX = "^"
      + "([A-Za-z]{3}[0-9]{1,4})"
      + "|([A-Za-z][0-9]{1,3}[A-Za-z]{3})"
      + "|([A-Za-z]{3}[0-9]{1,3}[A-Za-z])"
      + "|([A-Za-z]{2}[0-9]{2}[A-Za-z]{3})"
      + "|([A-Za-z]{1,3}[0-9]{1,3})"
      + "|([0-9]{1,4}[A-Za-z]{1,3})"
      + "|([A-Za-z]{1,2}[0-9]{1,4})"
      + "$";

  private static final Pattern vrmPattern = Pattern.compile(REGEX);

  /**
   * Method to check whether a vrn may be a UK number plate.
   * @param vrn the VRN to query against.
   * @return true/false indicator for whether a vrn may be a UK number plate.
   */
  public static boolean isPotentialUkVrn(String vrn) {
    
    // If empty yield early return
    if (Strings.isNullOrEmpty(vrn)) {
      log.debug("VRN provided for UK plate testing was empty");
      return false;
    }
    
    // Create a delimiter stripped VRN representation (i.e. no whitespace etc.)
    String delimiterStrippedVrn = vrn.replaceAll("\\s","");
    
    // If greater than 7 digits - immediately exclude
    if (delimiterStrippedVrn.length() > MAX_LENGTH) {
      log.debug("VRN {} too long to be a candidate UK plate", vrn);
      return false;
    }
    
    // Check whether VRN matches UK plate regex pattern
    boolean regexMatch = vrmPattern.matcher(delimiterStrippedVrn).matches();
    
    log.debug("VRN {} was deemed non-UK based on pattern", vrn);
    
    return regexMatch;
  }
}
