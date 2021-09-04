package be.mathiasbosman.fs;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ContainerServiceDto {

  private final String serviceName;
  private final int port;
}
