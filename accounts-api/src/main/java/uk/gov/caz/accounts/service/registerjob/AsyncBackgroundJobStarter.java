package uk.gov.caz.accounts.service.registerjob;

/**
 * Starts arbitrary background tasks.
 */
public interface AsyncBackgroundJobStarter {

  /**
   * Starts arbitrary background task which registers vrns from CSV file located at S3.
   * <p>
   * Runs task in 'fire and forget' mode - so returns void and allows task to continue in the
   * background unsupervised.
   * </p>
   *
   * @param registerJobId ID of register job that is being supervised by {@link
   *     RegisterJobSupervisor}. Running job internals must use this ID to tell supervisor about
   *     progress and status changes.
   * @param s3Bucket Name of S3 bucket that holds CSV file.
   * @param fileName Name of CSV file.
   * @param correlationId UUID formatted string to track the request through the enquiries
   *     stack.
   * @param shouldSendEmailsUponSuccessfulChargeCalculation Flag indicating whether to send
   *     email(s) upon successful charge-calculation-job completion.
   */
  void fireAndForgetRegisterCsvFromS3Job(int registerJobId, String s3Bucket, String fileName,
      String correlationId, boolean shouldSendEmailsUponSuccessfulChargeCalculation);
}
