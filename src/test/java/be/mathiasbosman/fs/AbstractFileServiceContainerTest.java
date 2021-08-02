package be.mathiasbosman.fs;

import be.mathiasbosman.fs.service.AbstractFileServiceTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

@Slf4j
@Testcontainers
@TestInstance(Lifecycle.PER_CLASS)
public abstract class AbstractFileServiceContainerTest extends AbstractFileServiceTest {

  private DockerComposeContainer<?> dockerContainer;

  protected static Slf4jLogConsumer getContainerLogConsumer(String serviceName) {
    return new Slf4jLogConsumer(LoggerFactory.getLogger("container." + serviceName));
  }

  public abstract DockerComposeContainer<?> createContainer();

  @BeforeAll
  protected void startContainer() {
    log.warn("Creating test container");
    this.dockerContainer = createContainer();
    log.warn("Starting test container");
    this.dockerContainer.start();
  }

  @AfterAll
  void stopContainer() {
    log.warn("Stopping test container");
    this.dockerContainer.stop();
  }
}
