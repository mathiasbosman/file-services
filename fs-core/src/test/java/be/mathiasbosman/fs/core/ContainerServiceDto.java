package be.mathiasbosman.fs.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Dto to hold container information.
 */
@Getter
@AllArgsConstructor
public class ContainerServiceDto {

  private final String serviceName;
  private final int port;
}
