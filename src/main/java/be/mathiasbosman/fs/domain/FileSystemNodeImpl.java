package be.mathiasbosman.fs.domain;

import be.mathiasbosman.fs.service.AbstractFileService;
import lombok.Getter;

/**
 * Simple implementation of {@link FileSystemNode}
 *
 * @author mathiasbosman
 * @since 0.0.1
 */
@Getter
public class FileSystemNodeImpl implements FileSystemNode {

  private final String parentPath;
  private final String path;
  private final String name;
  private final boolean isDirectory;
  private final long size;

  public FileSystemNodeImpl(String parentPath, String name, boolean isDirectory, long size) {
    this.parentPath = parentPath;
    this.path = AbstractFileService.combine(parentPath, name);
    this.name = name;
    this.isDirectory = isDirectory;
    this.size = size;
  }
}
