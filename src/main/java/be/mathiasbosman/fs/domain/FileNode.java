package be.mathiasbosman.fs.domain;

public interface FileNode {

  String getParentPath();

  String getPath();

  String getName();

  boolean isFolder();

  long getSize();
}