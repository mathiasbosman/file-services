package be.mathiasbosman.fs.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.zip.ZipInputStream;
import org.junit.Test;

public class WalkZipInputStreamTest {

  @Test
  public void walk() {
    StringBuilder sb = new StringBuilder();
    ZipInputStream zipInputStream = new ZipInputStream(
        FileServiceUtils.getResourceAsStream(WalkZipInputStreamTest.class, "zip/test.zip"));
    FileServiceUtils.walk(zipInputStream,
        f -> sb.append("f: ").append(f.getZipEntry().getName()).append(", "),
        d -> sb.append("d: ").append(d.getName()).append(", "));
    assertThat(sb).hasToString("d: a/, f: a/a.txt, d: b/, f: b/b.txt, f: c.txt, ");
  }
}