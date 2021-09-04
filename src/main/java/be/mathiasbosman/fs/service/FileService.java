package be.mathiasbosman.fs.service;

import be.mathiasbosman.fs.domain.FileSystemNode;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;

/**
 * Simple interface for handling file operations on a file system such as any NIO system or S3.
 *
 * @author mathiasbosman
 * @since 0.0.1
 */
public interface FileService {

  /**
   * Copy from given path to target path.
   *
   * @param source The path to copy from
   * @param target The target path
   */
  void copy(String source, String target);

  /**
   * Copy a {@link FileSystemNode} to a given target path.
   *
   * @param source The {@link FileSystemNode} to copy
   * @param target The target path
   */
  void copy(FileSystemNode source, String target);

  /**
   * Count all files in a given {@link FileSystemNode}. Directories are not counted.
   *
   * @param fileSystemNode The {@link FileSystemNode} to count all files
   * @return Amount of files in the given {@link FileSystemNode}
   */
  long countFiles(FileSystemNode fileSystemNode);

  /**
   * Deletes the given {@link FileSystemNode}.
   *
   * @param node The {@link FileSystemNode} to delete
   */
  void delete(FileSystemNode node);

  /**
   * Recursively delete a {@link FileSystemNode}.
   *
   * @param node      {@link FileSystemNode} to delete
   * @param recursive Flag to delete recursively or not
   */
  void delete(FileSystemNode node, boolean recursive);

  /**
   * Deletes the given path.
   *
   * @param path Path parts
   */
  void delete(String... path);

  /**
   * Checks if given path exists.
   *
   * @param parts Path parts
   * @return True or false if none-existing
   */
  boolean exists(String... parts);

  /**
   * Get the content as byte array from a given {@link FileSystemNode}.
   *
   * @param node The {@link FileSystemNode} to get byte array from
   * @return content
   */
  byte[] getBytes(FileSystemNode node);

  /**
   * Get the content as byte array from a given path.
   *
   * @param parts Path parts
   * @return content
   */
  byte[] getBytes(String... parts);


  /**
   * Get the creation time.
   *
   * @param node   The node to check
   * @param zoneId The zone id to convert to
   * @return the timestamp of creation
   */
  LocalDateTime getCreationTime(FileSystemNode node, ZoneId zoneId);

  /**
   * Get the {@link FileSystemNode} on the given path. If not found an exception is thrown.
   *
   * @param parts Path parts
   * @return The node found
   */
  FileSystemNode getFileNode(String... parts);

  /**
   * Get the last modified time.
   *
   * @param node   The node to check
   * @param zoneId The zone id to convert to
   * @return the timestamp of modification
   */
  LocalDateTime getLastModifiedTime(FileSystemNode node, ZoneId zoneId);

  /**
   * Get the Mime type of a given {@link FileSystemNode}.
   *
   * @param fileSystemNode The {@link FileSystemNode} to query
   * @return The Mime type
   */
  String getMimeType(FileSystemNode fileSystemNode);

  /**
   * Get the {@link FileSystemNode} on the given path. If not found null is returned.
   *
   * @param parts Path parts
   * @return The node found or null
   */
  FileSystemNode getOptionalFileNode(String... parts);

  /**
   * Returns the parent node of a given {@link FileSystemNode}.
   *
   * @param node {@link FileSystemNode} to query
   * @return The parent node or null if none found
   */
  FileSystemNode getParent(FileSystemNode node);

  /**
   * Returns the parent {@link FileSystemNode} of a given path.
   *
   * @param path Path parts
   * @return The parent node or null if none found
   */
  FileSystemNode getParent(String... path);

  /**
   * Returns the size (content length) of the given {@link FileSystemNode}.
   *
   * @param node {@link FileSystemNode} to query
   * @return The size of the content
   */
  long getSize(FileSystemNode node);

  /**
   * Checks a given path for being a directory.
   *
   * @param parts Path parts
   * @return True or false if not a directory
   */
  boolean isDirectory(String... parts);

  /**
   * List all file nodes that are children of the given {@link FileSystemNode}.
   *
   * @param root The {@link FileSystemNode} that is the root
   * @return the children nodes
   */
  List<FileSystemNode> list(FileSystemNode root);

  /**
   * List all {@link FileSystemNode}s on the given path.
   *
   * @param parts Path parts
   * @return {@link List} of {@link FileSystemNode}
   */
  List<FileSystemNode> list(String... parts);

  /**
   * Create all directories for the given path.
   *
   * @param path Path parts
   */
  void mkDirectories(String... path);

  /**
   * Move node from source to target.
   *
   * @param source The source path
   * @param target The target path
   */
  void move(String source, String target);

  /**
   * Open a given {@link FileSystemNode}.
   *
   * @param node The {@link FileSystemNode} to open
   * @return content as stream
   */
  InputStream open(FileSystemNode node);

  /**
   * Open a given path.
   *
   * @param parts Path parts
   * @return content as stream
   */
  InputStream open(String... parts);

  /**
   * Read a {@link FileSystemNode} as text.
   *
   * @param node The {@link FileSystemNode} to read
   * @return The content of given node as text
   */
  String read(FileSystemNode node);

  /**
   * Read the contents of a given path as text.
   *
   * @param parts Path parts
   * @return The content of the given path as text
   */
  String read(String... parts);

  /**
   * Save an {@link InputStream} to the given path.
   *
   * @param is    {@link InputStream} to save
   * @param parts Path parts
   */
  void save(InputStream is, String... parts);

  /**
   * Save a {@link Byte} array to the given path.
   *
   * @param bytes The array of {@link Byte}s to save
   * @param parts Path parts
   */
  void save(byte[] bytes, String... parts);

  /**
   * Save plain text to a given path.
   *
   * @param content Content to save
   * @param parts   Path parts
   */
  void saveText(String content, String... parts);

  /**
   * Get a {@link Stream} from a given {@link FileSystemNode}.
   *
   * @param root The {@link FileSystemNode} directory to stream
   * @return stream of the directory contents
   */
  Stream<FileSystemNode> streamDirectory(FileSystemNode root);

  /**
   * Walk all objects in given {@link FileSystemNode}.
   *
   * @param root    The {@link FileSystemNode} to walk
   * @param visitor The {@link FileNodeVisitor} to use
   */
  void walk(FileSystemNode root, FileNodeVisitor visitor);

}
