package uk.gov.caz.whitelist.model;

import lombok.AllArgsConstructor;

/**
 * When modifying Whitelist Vehicles database, modifier can specify one of 3 actions: CREATE "C",
 * UPDATE "C" or DELETE "D".
 */
@AllArgsConstructor
public enum Actions {
  CREATE("C"),
  UPDATE("U"),
  DELETE("D");

  private String actionCharacter;

  /**
   * Gets character associated with this action.
   *
   * @return Character associated with this action.
   */
  public String getActionCharacter() {
    return actionCharacter;
  }
}
