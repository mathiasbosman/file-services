package be.mathiasbosman.fs.core.domain;

import be.mathiasbosman.fs.core.util.FileServiceUtils;
import java.util.Date;

public record FileSystemNode(String parentPath, String path, String name, boolean isDirectory,
                             long size, Date lastModified) {

  public FileSystemNode(String parentPath, String name, boolean isDirectory, long size) {
    this(parentPath, name, isDirectory, size, new Date());
  }

  public FileSystemNode(String parentPath, String name, boolean isDirectory, long size,
      Date lastModified) {
    this(parentPath, FileServiceUtils.combine(parentPath, name), name, isDirectory, size,
        lastModified);
  }
}
