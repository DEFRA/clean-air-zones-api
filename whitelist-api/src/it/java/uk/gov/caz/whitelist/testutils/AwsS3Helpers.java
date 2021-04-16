package uk.gov.caz.whitelist.testutils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;
import java.util.Iterator;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AwsS3Helpers {

  public static void deleteBucketWithObjects(String s3Bucket, AmazonS3 amazonS3) {
    try {
      deleteAllObjectsInS3Bucket(s3Bucket, amazonS3);
      deleteAllObjectVersionsInS3Bucket(s3Bucket, amazonS3);
      amazonS3.deleteBucket(s3Bucket);
    } catch (AmazonServiceException e) {
      throw new RuntimeException(e);
    }
  }

  private static void deleteAllObjectsInS3Bucket(String s3Bucket, AmazonS3 amazonS3) {
    ObjectListing objectListing = amazonS3.listObjects(s3Bucket);
    while (true) {
      for (Iterator<?> iterator = objectListing.getObjectSummaries().iterator();
          iterator.hasNext(); ) {
        S3ObjectSummary summary = (S3ObjectSummary) iterator.next();
        amazonS3.deleteObject(s3Bucket, summary.getKey());
      }

      if (objectListing.isTruncated()) {
        objectListing = amazonS3.listNextBatchOfObjects(objectListing);
      } else {
        break;
      }
    }
  }

  private static void deleteAllObjectVersionsInS3Bucket(String s3Bucket, AmazonS3 amazonS3) {
    VersionListing versionListing = amazonS3
        .listVersions(new ListVersionsRequest().withBucketName(s3Bucket));
    while (true) {
      for (Iterator<?> iterator = versionListing.getVersionSummaries().iterator();
          iterator.hasNext(); ) {
        S3VersionSummary vs = (S3VersionSummary) iterator.next();
        amazonS3.deleteVersion(s3Bucket, vs.getKey(), vs.getVersionId());
      }

      if (versionListing.isTruncated()) {
        versionListing = amazonS3.listNextBatchOfVersions(versionListing);
      } else {
        break;
      }
    }
  }
}
