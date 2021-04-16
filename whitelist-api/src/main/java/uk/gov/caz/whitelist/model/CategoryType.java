package uk.gov.caz.whitelist.model;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Class that contains all available Categories and specific exempt and compliant mapping.
 */
@AllArgsConstructor
@Getter
public enum CategoryType {
  EARLY_ADOPTER("Early Adopter", false, true),
  NON_UK_VEHICLE("Non-UK Vehicle", false, true),
  PROBLEMATIC_VRN("Problematic VRN", true, false),
  EXEMPTION("Exemption", true, false),
  OTHER("Other", true, false);

  private String category;
  private boolean exempt;
  private boolean compliant;

  /**
   * Helper method to get all available categories.
   *
   * @return {@link Set} of available categories.
   */
  public static Set<String> availableCategories() {
    return Stream.of(values()).map(CategoryType::getCategory).collect(Collectors.toSet());
  }

  /**
   * Helper method to find CategoryType by string category value.
   *
   * @param category string value.
   * @return {@link CategoryType}.
   */
  public static Optional<CategoryType> fromCategory(String category) {
    return Stream.of(values())
        .filter(categoryType -> categoryType.getCategory()
        .equalsIgnoreCase(category))
        .findFirst();
  }
}
