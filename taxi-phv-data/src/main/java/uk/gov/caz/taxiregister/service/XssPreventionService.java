package uk.gov.caz.taxiregister.service;

import java.util.Arrays;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;
import uk.gov.caz.taxiregister.controller.exception.SecurityThreatException;
import uk.gov.caz.taxiregister.dto.Vehicles;

/**
 * Service responsible for checking if there are XSS vulnerabilities found in fields.
 */
@Component
public class XssPreventionService {

  public static final String OPEN_HTML_TAG_ELEMENT = "<";

  /**
   * Checks Vehicle DTO against XSS values.
   */
  public void checkVehicles(Vehicles vehicles) {
    vehicles.getVehicleDetails().stream()
        .forEach(vehicleDto -> checkTextProperties(vehicleDto.getDescription(),
            vehicleDto.getLicensePlateNumber()));
  }

  /**
   * Checks single text property against XSS values.
   */
  private void checkTextProperties(String... textProperties) {
    Arrays.stream(textProperties)
        .forEach(this::checkForHtmlScript);
  }

  /**
   * Checks whether text contains some element in its normal or escaped version.
   * @param input checked text
   * @param element element being searched for
   * @return if element was found
   */
  private boolean containsTextOrItsEscapedVersion(String input, String element) {
    if (StringUtils.isEmpty(input)) {
      return false;
    }
    return input.contains(element) || input.contains(HtmlUtils.htmlEscape(element));
  }

  /**
   * Checks input for html and throws an {@link SecurityThreatException} exception if found.
   * @param input text being searched for
   */
  private void checkForHtmlScript(String input) {
    if (containsTextOrItsEscapedVersion(input, OPEN_HTML_TAG_ELEMENT)) {
      throw new SecurityThreatException("Discovered XSS entry. Rejecting request.");
    }
  }
}