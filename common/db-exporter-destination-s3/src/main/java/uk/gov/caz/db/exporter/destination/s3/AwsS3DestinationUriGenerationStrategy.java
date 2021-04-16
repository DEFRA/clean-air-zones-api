package uk.gov.caz.db.exporter.destination.s3;

import com.amazonaws.services.s3.AmazonS3;
import java.net.URI;

/**
 * Defines algorithm to generate target destination URI on AWS S3.
 */
public interface AwsS3DestinationUriGenerationStrategy {

  /**
   * Generates URI for object put in AWS S3.
   *
   * @param s3Bucket AWS S3 bucket that stores the object.
   * @param s3ObjectName Object key/name.
   * @param amazonS3 V1 AWS S3 client.
   * @return URI for object on AWS S3.
   */
  URI getUri(String s3Bucket, String s3ObjectName, AmazonS3 amazonS3);
}
