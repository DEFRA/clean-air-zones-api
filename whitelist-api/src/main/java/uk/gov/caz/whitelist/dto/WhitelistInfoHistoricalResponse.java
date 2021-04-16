package uk.gov.caz.whitelist.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.whitelist.model.CategoryType;
import uk.gov.caz.whitelist.model.WhitelistVehicleHistory;

/**
 * Value object that represents whitelist info historical response.
 */
@Value
@Builder
public class WhitelistInfoHistoricalResponse {

  /**
   * Page that has been retrieved.
   */
  int page;

  /**
   * Total number of pages available (with current page size).
   */
  int pageCount;

  /**
   * The current page size.
   */
  int perPage;

  /**
   * The total number of changes associated with this vehicle.
   */
  int totalChangesCount;

  /**
   * A list of changes associated with this vehicle.
   */
  List<Change> changes;

  @Value
  @Builder
  public static class Change {

    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /**
     * Date of modification.
     */
    @JsonFormat(pattern = DATE_FORMAT)
    LocalDate modifyDate;

    /**
     * Status of current VRM for a specific date range.
     */
    String action;

    /**
     * Category type.
     */
    String category;

    /**
     * Manufacturer name.
     */
    String manufacturer;

    /**
     * Update reason.
     */
    String reasonUpdated;

    /**
     * User's sub.
     */
    UUID uploaderId;

    /**
     * User's email.
     */
    String uploaderEmail;

    /**
     * Maps {@link WhitelistVehicleHistory} to {@link Change}.
     *
     * @param history An instance of {@link WhitelistVehicleHistory} to be mapped
     * @return An instance of {@link Change} mapped from {@link WhitelistVehicleHistory}
     */
    public static Change from(WhitelistVehicleHistory history) {
      Optional<CategoryType> category = CategoryType.fromCategory(history.getCategory());
      String categoryString = category.isPresent() ? history.getCategory() : null;
      return Change.builder()
          .modifyDate(history.getModifyDate())
          .action(history.getAction())
          .category(categoryString)
          .manufacturer(history.getManufacturer())
          .reasonUpdated(history.getReasonUpdated())
          .uploaderId(
              Optional.ofNullable(history.getModifierId()).map(UUID::fromString).orElse(null))
          .uploaderEmail(history.getModifierEmail())
          .build();
    }
  }
}
