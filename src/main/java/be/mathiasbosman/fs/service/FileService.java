package be.mathiasbosman.fs.service;

import be.mathiasbosman.fs.domain.FileNode;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

public interface FileService {

  /**
   * Copy from given path to target path
   *
   * @param source The path to copy from
   * @param target The target path
   */
  void copy(String source, String target);

  /**
   * Copy a {@link FileNode} to a given target path
   *
   * @param source The {@link FileNode} to copy
   * @param target The target path
   */
  void copy(FileNode source, String target);

  /**
   * Count all files in a given {@link FileNode}. Directories are not counted.
   *
   * @param fileNode The {@link FileNode} to count all files
   * @return Amount of files in the given {@link FileNode}
   */
  long countFiles(FileNode fileNode);

  /**
   * Deletes the given {@link FileNode}
   *
   * @param node The {@link FileNode} to delete
   */
  void delete(FileNode node);

  /**
   * Recursively delete a {@link FileNode}
   *
   * @param node      {@link FileNode} to delete
   * @param recursive Flag to delete recursively or not
   */
  void delete(FileNode node, boolean recursive);

  /**
   * Deletes the given path
   *
   * @param path Path parts
   */
  void delete(String... path);

  /**
   * Checks if given path exists
   *
   * @param parts Path parts
   * @return True or false if none-existing
   */
  boolean exists(String... parts);

  /**
   * Get byte array from a given {@link FileNode}
   *
   * @param node The {@link FileNode} to get byte array from
   * @return {@link Byte} array
   */
  byte[] getBytes(FileNode node);

  /**
   * Get byte array from a given path
   *
   * @param parts Path parts
   * @return {@link Byte} array
   */
  byte[] getBytes(String... parts);

  /**
   * Get the {@link FileNode} on the given path. If not found an exception is thrown
   *
   * @param parts Path parts
   * @return {@link FileNode}
   */
  FileNode getFileNode(String... parts);

  /**
   * Get the Mime type of a given {@link FileNode}
   *
   * @param fileNode The {@link FileNode} to query
   * @return The Mime type
   */
  String getMimeType(FileNode fileNode);

  /**
   * Get the {@link FileNode} on the given path. If not found null is returned.
   *
   * @param parts Path parts
   * @return {@link FileNode}
   */
  FileNode getOptionalFileNode(String... parts);

  /**
   * Returns the parent node of a given {@link FileNode}
   *
   * @param node {@link FileNode} to query
   * @return {@link FileNode}
   */
  FileNode getParent(FileNode node);

  /**
   * Gets the parent path of a given path
   *
   * @param path Path part
   * @return Path of the parent
   */
  String getParentPath(String... path);

  /**
   * Returns the size (content length) of the given {@link FileNode}
   *
   * @param node {@link FileNode} to query
   * @return The size of the content
   */
  long getSize(FileNode node);

  /**
   * Returns the parent {@link FileNode} of a given path
   *
   * @param path Path parts
   * @return {@link FileNode}
   */
  FileNode getParent(String... path);

  /**
   * Checks a given path for being a directory
   *
   * @param parts Path parts
   * @return True or false if not a directory
   */
  boolean isDirectory(String... parts);

  /**
   * List all file nodes that are children of the given {@link FileNode}
   *
   * @param root The {@link FileNode} that is the root
   * @return {@link List} of {@link FileNode}
   */
  List<FileNode> list(FileNode root);

  /**
   * List all {@link FileNode}s on the given path
   *
   * @param parts Path parts
   * @return {@link List} of {@link FileNode}
   */
  List<FileNode> list(String... parts);

  /**
   * Create all folders for the given path
   *
   * @param path Path parts
   */
  void mkFolders(String... path);

  /**
   * Move node from source to target
   *
   * @param source The source path
   * @param target The target path
   */
  void move(String source, String target);

  /**
   * Open a given {@link FileNode}
   *
   * @param node The {@link FileNode} to open
   * @return {@link InputStream}
   */
  InputStream open(FileNode node);

  /**
   * Open a given {@link FileNode}
   *
   * @param parts Path parts
   * @return {@link InputStream}
   */
  InputStream open(String... parts);

  /**
   * Read a {@link FileNode} as text
   *
   * @param node The {@link FileNode} to read
   * @return The content of given {@link FileNode} as text
   */
  String read(FileNode node);

  /**
   * Read the contents of a given path as text
   *
   * @param parts Path parts
   * @return The content of the given path as text
   */
  String read(String... parts);

  /**
   * Save an {@link InputStream} to the given path with given size
   *
   * @param is   {@link InputStream} to save
   * @param path Path to save as
   * @param size Size of the given stream (the content lenght)
   */
  void save(InputStream is, String path, long size);

  /**
   * Save an {@link InputStream} to the given path
   *
   * @param is    {@link InputStream} to save
   * @param parts Path parts
   */
  void save(InputStream is, String... parts);

  /**
   * Save a {@link Byte} array to the given path
   *
   * @param bytes The array of {@link Byte}s to save
   * @param parts Path parts
   */
  void save(byte[] bytes, String... parts);

  /**
   * Save plain text to a given path
   *
   * @param content Content to save
   * @param parts   Path parts
   */
  void saveText(String content, String... parts);

  /**
   * Get a {@link Stream<FileNode>} from a given {@link FileNode}
   *
   * @param root The {@link FileNode} directory to stream
   * @return {@link Stream} of {@link FileNode}
   */
  Stream<FileNode> streamDirectory(FileNode root);

  /**
   * Walk all objects in given {@link FileNode}
   *
   * @param root    The {@link FileNode} to walk
   * @param visitor The {@link FileNodeVisitor} to use
   */
  void walk(FileNode root, FileNodeVisitor visitor);

}
