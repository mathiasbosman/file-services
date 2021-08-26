package be.mathiasbosman.fs;

import be.mathiasbosman.fs.service.AbstractFileServiceTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.testcontainers.containers.DockerComposeContainer;

@Slf4j
@TestInstance(Lifecycle.PER_CLASS)
public abstract class AbstractContainerTest extends AbstractFileServiceTest implements
    DockerComposeContainerTest {

  private DockerComposeContainer<?> dockerContainer;

  public abstract DockerComposeContainer<?> createContainer();

  @BeforeAll
  public void startContainer() {
    this.dockerContainer = createContainer();
    this.dockerContainer.start();
  }

  @AfterAll
  public void stopContainer() {
    this.dockerContainer.stop();
  }
}
