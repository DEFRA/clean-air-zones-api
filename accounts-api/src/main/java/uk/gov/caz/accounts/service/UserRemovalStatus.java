package uk.gov.caz.accounts.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum UserRemovalStatus {
  SUCCESSFULLY_DELETED(204),
  ALREADY_DELETED(204),
  USER_DOESNT_EXIST(422),
  USER_IS_AN_OWNER(403);

  @Getter
  private final int httpStatus;
}
