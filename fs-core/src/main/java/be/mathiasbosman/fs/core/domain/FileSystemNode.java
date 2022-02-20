package be.mathiasbosman.fs.core.domain;

/**
 * Interface that represents a node on the file system. This node can be a directory, a file or
 * none-existent.
 */
public interface FileSystemNode {

  /**
   * Returns the name of the node.
   *
   * @return Name of the node
   */
  String getName();

  /**
   * Returns the full path of the node.
   *
   * @return Full path
   */
  String getPath();

  /**
   * Get the parent path.
   *
   * @return The parent path
   */
  String getParentPath();

  /**
   * Get size.
   *
   * @return The size of the node (content length)
   */
  long getSize();

  /**
   * Determines if the node is a directory or not.
   *
   * @return True if directory
   */
  boolean isDirectory();
}