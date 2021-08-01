package be.mathiasbosman.fs.domain;

import be.mathiasbosman.fs.service.AbstractFileService;

public class FileNodeImpl implements FileNode {

  private final String parentPath;
  private final String path;
  private final String name;
  private final boolean isFolder;
  private final long size;

  public FileNodeImpl(String parentPath, String name, boolean isFolder, long size) {
    this.parentPath = parentPath;
    this.path = AbstractFileService.combine(parentPath, name);
    this.name = name;
    this.isFolder = isFolder;
    this.size = size;
  }

  @Override
  public String getParentPath() {
    return parentPath;
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean isFolder() {
    return isFolder;
  }

  @Override
  public long getSize() {
    return size;
  }

}
