package uk.gov.caz.whitelist.service.exception;

public class FatalErrorWithCsvFileMetadataException extends RuntimeException {

  public FatalErrorWithCsvFileMetadataException(String message) {
    super(message);
  }
}
