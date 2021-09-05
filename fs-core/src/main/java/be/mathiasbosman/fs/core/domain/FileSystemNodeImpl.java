package be.mathiasbosman.fs.core.domain;

import be.mathiasbosman.fs.core.service.AbstractFileService;
import lombok.Getter;

/**
 * Simple implementation of {@link FileSystemNode}.
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

  /**
   * Create a File node.
   *
   * @param parentPath  The parent path (optional)
   * @param name        Name of the node
   * @param isDirectory Indicator of directory
   * @param size        Size of the node
   */
  public FileSystemNodeImpl(String parentPath, String name, boolean isDirectory, long size) {
    this.parentPath = parentPath;
    this.path = AbstractFileService.combine(parentPath, name);
    this.name = name;
    this.isDirectory = isDirectory;
    this.size = size;
  }
}
