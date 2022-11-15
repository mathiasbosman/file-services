package be.mathiasbosman.fs.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipEntryInputStream extends InputStream {

  private final InputStream delegate;
  private final ZipEntry zipEntry;

  public ZipEntryInputStream(ZipInputStream inputStream, ZipEntry entry) {
    this.delegate = inputStream;
    this.zipEntry = entry;
  }

  @Override
  public int read() throws IOException {
    return delegate.read();
  }

  public ZipEntry getZipEntry() {
    return zipEntry;
  }
}