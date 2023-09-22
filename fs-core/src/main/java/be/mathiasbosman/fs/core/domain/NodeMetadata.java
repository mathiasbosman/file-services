package be.mathiasbosman.fs.core.domain;

import java.util.Date;
import lombok.Getter;

@Getter
public class NodeMetadata {

  private final FileSystemNodeType type;
  private final long size;
  private final Date lastModified;

  public NodeMetadata(FileSystemNodeType type) {
    this(type, 0L, null);
  }

  public NodeMetadata(FileSystemNodeType type, long size, Date lastModified) {
    this.type = type;
    this.size = size;
    this.lastModified = lastModified;
  }

  public boolean isDirectory() {
    return type == FileSystemNodeType.DIRECTORY;
  }
}
