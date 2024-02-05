package be.mathiasbosman.fs.core.domain;

import java.util.Date;

public record NodeMetadata(FileSystemNodeType type, long size, Date lastModified) {

  public NodeMetadata(FileSystemNodeType type) {
    this(type, 0L, null);
  }

  public boolean isDirectory() {
    return type == FileSystemNodeType.DIRECTORY;
  }
}
