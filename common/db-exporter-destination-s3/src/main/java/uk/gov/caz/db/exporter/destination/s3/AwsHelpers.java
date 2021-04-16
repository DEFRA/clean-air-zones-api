package uk.gov.caz.db.exporter.destination.s3;

import lombok.experimental.UtilityClass;

/**
 * Some little helpers for AWS deployed runtime.
 */
@UtilityClass
class AwsHelpers {

  /**
   * Determines if code is running locally using AWS SAM Local tool.
   *
   * @return true if code is running in Lambda environment simulated locally by AWS SAM Local tool
   *     (docker).
   */
  static boolean areWeRunningLocallyUsingSam() {
    return System.getenv("AWS_SAM_LOCAL") != null;
  }

  /**
   * Returns value of AWS_ACCESS_KEY_ID environment variable (if set).
   *
   * @return value of AWS_ACCESS_KEY_ID environment variable if set or null otherwise.
   */
  static String getAwsAccessKeyFromEnvVar() {
    return System.getenv("AWS_ACCESS_KEY_ID");
  }

  /**
   * Returns value of AWS_REGION environment variable (if set).
   *
   * @return value of AWS_REGION environment variable if set or null otherwise.
   */
  static String getAwsRegionFromEnvVar() {
    return System.getenv("AWS_REGION");
  }

  /**
   * Returns value of AWS_PROFILE environment variable (if set).
   *
   * @return value of AWS_PROFILE environment variable if set or null otherwise.
   */
  static String getAwsProfileFromEnvVar() {
    return System.getenv("AWS_PROFILE");
  }
}
