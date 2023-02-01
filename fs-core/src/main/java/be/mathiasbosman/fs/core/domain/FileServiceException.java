package be.mathiasbosman.fs.core.domain;

import be.mathiasbosman.fs.core.config.Generated;

@Generated //ignores test coverage
public class FileServiceException extends RuntimeException {

  public FileServiceException(String message) {
    super(message);
  }

  public FileServiceException(Throwable cause) {
    super(cause);
  }
}
