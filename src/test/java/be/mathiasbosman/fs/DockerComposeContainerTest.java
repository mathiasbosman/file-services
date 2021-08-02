package be.mathiasbosman.fs;

import org.testcontainers.containers.DockerComposeContainer;

public interface DockerComposeContainerTest {
  DockerComposeContainer<?> createContainer();

  void startContainer();

  void stopContainer();
}
