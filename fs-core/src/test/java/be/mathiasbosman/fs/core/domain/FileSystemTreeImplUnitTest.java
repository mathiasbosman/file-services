package be.mathiasbosman.fs.core.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

class FileSystemTreeImplUnitTest {

  private final static String ROOT = "root";

  @Test
  void add() {
    FileSystemTree<String> tree = new FileSystemTreeImpl<>(ROOT);
    tree.add("child", () -> RandomStringUtils.random(5));

    assertThat(tree.getChildren()).hasSize(1);
  }

  @Test
  void getChildrenIsEmpty() {
    FileSystemTree<String> tree = new FileSystemTreeImpl<>(ROOT);

    assertThat(tree.getChildren()).isEmpty();
  }

  @Test
  void getNode() {
    FileSystemTree<String> tree = new FileSystemTreeImpl<>(ROOT);

    assertThat(tree.getNode()).isEqualTo(ROOT);
  }

}
