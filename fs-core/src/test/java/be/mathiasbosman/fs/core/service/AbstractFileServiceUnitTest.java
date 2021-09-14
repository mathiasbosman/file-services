package be.mathiasbosman.fs.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

class AbstractFileServiceUnitTest {

  @Test
  void checkPath() {
    String[] nullString = new String[0];
    assertThatThrownBy(() -> AbstractFileService.checkPath(nullString))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void combine() {
    assertThat(AbstractFileService.combine("x", " y", " ", "z")).isEqualTo("x/y/z");
    assertThat(AbstractFileService.combine("x", "y", "z")).isEqualTo("x/y/z");
    assertThat(AbstractFileService.combine("x/y", "/z")).isEqualTo("x/y/z");
    assertThat(AbstractFileService.combine(false, "x/y", "/z"))
        .isEqualTo("x/y/z");
    assertThat(AbstractFileService.combine(true, "x", "y", "z"))
        .isEqualTo("/x/y/z");
  }

  @Test
  void getExtension() {
    assertThat(AbstractFileService.getExtension((String) null)).isNull();
    assertThat(AbstractFileService.getExtension("")).isNull();
    assertThat(AbstractFileService.getExtension("a")).isNull();
    assertThat(AbstractFileService.getExtension("a.jpeg")).isEqualTo("jpeg");
    assertThat(AbstractFileService.getExtension("a", "b", "c.xml")).isEqualTo("xml");
  }

  @Test
  void getParentPath() {
    assertThat(AbstractFileService.getParentPath("a/b/c.txt")).isEqualTo("a/b");
  }

  @Test
  void strip() {
    assertThat(AbstractFileService.strip("a/b/c", '/')).isEqualTo("a/b/c");
    assertThat(AbstractFileService.strip("/a/b/c", '/')).isEqualTo("a/b/c");
    assertThat(AbstractFileService.strip("a/b/c/", '/')).isEqualTo("a/b/c");
    assertThat(AbstractFileService.strip("/a/b/c/", '/')).isEqualTo("a/b/c");
  }

  @Test
  void split() {
    assertThat(AbstractFileService.split(null)).isEqualTo(Pair.of(null, null));
    assertThat(AbstractFileService.split("a")).isEqualTo(Pair.of(null, "a"));
    assertThat(AbstractFileService.split("a/b")).isEqualTo(Pair.of("a", "b"));
    assertThat(AbstractFileService.split("a/b/c")).isEqualTo(Pair.of("a/b", "c"));
  }
}