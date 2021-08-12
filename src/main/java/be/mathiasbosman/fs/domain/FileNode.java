package be.mathiasbosman.fs.domain;

public interface FileNode {

  /**
   * Returns the name of the node
   *
   * @return Name of the node
   */
  String getName();

  /**
   * Returns the full path of the node
   *
   * @return Full path
   */
  String getPath();

  /**
   * Get the parent path
   *
   * @return The parent path
   */
  String getParentPath();

  /**
   * Get size
   *
   * @return The size of the node (content length)
   */
  long getSize();

  /**
   * Determines if the node is a directory or not
   *
   * @return True if directory
   */
  boolean isDirectory();
}