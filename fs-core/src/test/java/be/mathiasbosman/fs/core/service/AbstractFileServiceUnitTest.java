package be.mathiasbosman.fs.core.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AbstractFileServiceUnitTest {

  @Test
  void checkPath() {
    String[] nullString = new String[0];
    assertThatThrownBy(() -> AbstractFileService.checkPath(nullString))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
