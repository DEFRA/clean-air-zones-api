package uk.gov.caz.csv.model;

import java.util.List;
import lombok.Value;

/**
 * Value class that represents parse results.
 */
@Value
public class CsvParseResult<T> {
  List<T> objects;
  List<CsvValidationError> validationErrors;
}
