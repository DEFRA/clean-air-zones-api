package uk.gov.caz.db.exporter.destination.s3;

import com.amazonaws.services.s3.AmazonS3;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * This provider can be used by clients to obtain new instances of {@link AwsS3Destination} class
 * needed to export database to AWS S3.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class AwsS3DestinationProvider {

  /**
   * Allows to do operations over S3.
   */
  private final AmazonS3 amazonS3;

  /**
   * Returns {@link Builder} instance that can be used to fully initialize and build {@link
   * AwsS3Destination} object.
   *
   * @return {@link Builder} instance that can be used to fully initialize and build {@link
   *     AwsS3Destination} object.
   */
  public Builder provide() {
    return new Builder(amazonS3);
  }

  /**
   * Can be used to fully initialize and build {@link AwsS3Destination} object.
   */
  public static class Builder {

    /**
     * S3 bucket that will be used to store exported data.
     */
    private String s3Bucket;

    /**
     * S3 Object Key that will be used to store exported data.
     */
    private String s3ObjectName;

    /**
     * Defines Mime Type of data stored in the S3 object.
     */
    private String mimeType;

    /**
     * Allows to do operations over S3.
     */
    private AmazonS3 amazonS3;

    /**
     * Implementation that generates target destination URI on AWS S3.
     */
    private AwsS3DestinationUriGenerationStrategy awsS3DestinationUriGenerationStrategy;

    /**
     * Creates new instance of {@link Builder}.
     *
     * @param amazonS3 Allows to do operations over S3.
     */
    public Builder(AmazonS3 amazonS3) {
      this.amazonS3 = amazonS3;
    }

    /**
     * Defines S3 bucket that will be used to store exported data.
     */
    public Builder inS3Bucket(String s3Bucket) {
      this.s3Bucket = s3Bucket;
      return this;
    }

    /**
     * Defines S3 Object Key that will be used to store exported data.
     */
    public Builder inS3Object(String s3ObjectName) {
      this.s3ObjectName = s3ObjectName;
      return this;
    }

    /**
     * Defines Mime Type of data stored in the S3 object.
     */
    public Builder withMimeType(String mimeType) {
      this.mimeType = mimeType;
      return this;
    }

    /**
     * Defines generation strategy for URI of target destination object on AWS S3.
     */
    public Builder usingUriGenerator(
        AwsS3DestinationUriGenerationStrategy awsS3DestinationUriGenerationStrategy) {
      this.awsS3DestinationUriGenerationStrategy = awsS3DestinationUriGenerationStrategy;
      return this;
    }

    /**
     * Builds final {@link AwsS3Destination} object.
     */
    public AwsS3Destination inDestination() {
      return new AwsS3Destination(s3Bucket, s3ObjectName, mimeType, amazonS3,
          awsS3DestinationUriGenerationStrategy);
    }
  }
}
