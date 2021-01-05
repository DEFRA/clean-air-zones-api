package uk.gov.caz.accounts.dto;

import uk.gov.caz.accounts.model.registerjob.RegisterJobStatus;

/**
 * If you change anything in this class remember to update API documentation in {@link
 * StatusOfRegisterCsvFromS3JobQueryResult} if necessary.
 */
public enum RegisterJobStatusDto {
  RUNNING,
  CHARGEABILITY_CALCULATION_IN_PROGRESS,
  SUCCESS,
  FAILURE;

  /**
   * Create {@link RegisterJobStatusDto} enum from {@link RegisterJobStatus} model status.
   *
   * @param registerJobStatus Current status of model related enum (much more detailed).
   * @return {@link RegisterJobStatusDto} value.
   */
  public static RegisterJobStatusDto from(RegisterJobStatus registerJobStatus) {
    switch (registerJobStatus) {
      case STARTING:
      case RUNNING:
        return RUNNING;
      case FINISHED_SUCCESS:
        return SUCCESS;
      case CHARGEABILITY_CALCULATION_IN_PROGRESS:
        return CHARGEABILITY_CALCULATION_IN_PROGRESS;
      default:
        return FAILURE;
    }
  }
}
