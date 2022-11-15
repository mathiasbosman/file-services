package be.mathiasbosman.fs.core;

import be.mathiasbosman.fs.core.service.AbstractFileServiceTest;
import java.io.File;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Abstract container test that uses test containers.
 *
 * @see DockerComposeContainer
 */
@Slf4j
@TestInstance(Lifecycle.PER_CLASS)
public abstract class AbstractContainerTest extends AbstractFileServiceTest {

  private DockerComposeContainer<?> dockerContainer;
  private final List<ContainerServiceDto> services;
  private final String composeFileSrc;

  public AbstractContainerTest(String composeFileSrc, List<ContainerServiceDto> services) {
    this.composeFileSrc = composeFileSrc;
    this.services = services;
  }

  public AbstractContainerTest(String composeFileSrc, ContainerServiceDto dto) {
    this(composeFileSrc, Collections.singletonList(dto));
  }

  /**
   * Creates the container.
   */
  public void createContainer() {
    this.dockerContainer = new DockerComposeContainer<>(
        new File(composeFileSrc))
        .withLocalCompose(true)
        .withPull(false);
    services.forEach(this::exposeService);
  }

  private void exposeService(ContainerServiceDto dto) {
    log.info("Exposing {}", dto);
    this.dockerContainer.withExposedService(
        dto.serviceName(),
        dto.port(),
        Wait.forListeningPort()
    );
  }

  @BeforeAll
  public void startContainer() {
    createContainer();
    this.dockerContainer.start();
  }

  @AfterAll
  public void stopContainer() {
    this.dockerContainer.stop();
  }
}
