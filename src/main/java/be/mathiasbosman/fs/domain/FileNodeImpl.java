package be.mathiasbosman.fs.domain;

import be.mathiasbosman.fs.service.AbstractFileService;
import lombok.Getter;

@Getter
public class FileNodeImpl implements FileNode {

  private final String parentPath;
  private final String path;
  private final String name;
  private final boolean isDirectory;
  private final long size;

  public FileNodeImpl(String parentPath, String name, boolean isDirectory, long size) {
    this.parentPath = parentPath;
    this.path = AbstractFileService.combine(parentPath, name);
    this.name = name;
    this.isDirectory = isDirectory;
    this.size = size;
  }
}
