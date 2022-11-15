package be.mathiasbosman.fs.core.domain;

public class FileServiceException extends RuntimeException {

  public FileServiceException(String message) {
    super(message);
  }

  public FileServiceException(Throwable cause) {
    super(cause);
  }
}
