package be.mathiasbosman.fs.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.zip.ZipInputStream;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

class FileServiceUtilsUnitTest {

  @Test
  void combine() {
    assertThat(FileServiceUtils.combine("x", " y", " ", "z")).isEqualTo("x/y/z");
    assertThat(FileServiceUtils.combine("x", "y", "z")).isEqualTo("x/y/z");
    assertThat(FileServiceUtils.combine("x/y", "/z")).isEqualTo("x/y/z");
  }

  @Test
  void getExtension() {
    assertThat(FileServiceUtils.getExtension((String) null)).isNull();
    assertThat(FileServiceUtils.getExtension("")).isNull();
    assertThat(FileServiceUtils.getExtension("a")).isNull();
    assertThat(FileServiceUtils.getExtension("a.jpeg")).isEqualTo("jpeg");
    assertThat(FileServiceUtils.getExtension("a", "b", "c.xml")).isEqualTo("xml");
  }

  @Test
  void getParentPath() {
    assertThat(FileServiceUtils.getParentPath("a/b/c.txt")).isEqualTo("a/b");
  }

  @Test
  void strip() {
    assertThat(FileServiceUtils.strip("a/b/c")).isEqualTo("a/b/c");
    assertThat(FileServiceUtils.strip("/a/b/c")).isEqualTo("a/b/c");
    assertThat(FileServiceUtils.strip("a/b/c/")).isEqualTo("a/b/c");
    assertThat(FileServiceUtils.strip("/a/b/c/")).isEqualTo("a/b/c");
  }

  @Test
  void split() {
    assertThat(FileServiceUtils.split(null)).isEqualTo(Pair.of(null, null));
    assertThat(FileServiceUtils.split("a")).isEqualTo(Pair.of(null, "a"));
    assertThat(FileServiceUtils.split("a/b")).isEqualTo(Pair.of("a", "b"));
    assertThat(FileServiceUtils.split("a/b/c")).isEqualTo(Pair.of("a/b", "c"));
  }

  @Test
  void getResourceAsStreamFails() {
    assertThatThrownBy(() -> new ZipInputStream(
        FileServiceUtils.getResourceAsStream(WalkZipInputStreamTest.class, "foo/bar")))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void appendSeparator() {
    assertThat(FileServiceUtils.appendSeparator("a/")).isEqualTo("a/");
    assertThat(FileServiceUtils.appendSeparator("a")).isEqualTo("a/");
  }

  @Test
  void getContentType() throws IOException {
    assertThat(FileServiceUtils.getContentType("test.txt")).isEqualTo("text/plain");
    assertThat(FileServiceUtils.getContentType("a/b/test.txt")).isEqualTo("text/plain");
    assertThat(FileServiceUtils.getContentType("foo.bar")).isNull();
  }

}
