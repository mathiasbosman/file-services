package be.mathiasbosman.fs.service;

import be.mathiasbosman.fs.domain.FileSystemNode;
import be.mathiasbosman.fs.domain.FileSystemNodeImpl;
import be.mathiasbosman.fs.domain.FileSystemNodeType;
import com.google.common.base.Joiner;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Abstract implementation of {@link FileService}.
 * Also holding some static methods that can be used for path manipulation.
 *
 * @author mathiasbosman
 * @since 0.0.1
 */
public abstract class AbstractFileService implements FileService {

  /**
   * The character used for separating the file extension (defaults to '.').
   */
  public static final char EXTENSION_SEPARATOR = '.';

  /**
   * Combine multiple strings to a path using the {@link File} separator. Paths are also stripped
   * from excessive {@link File#separatorChar}.
   *
   * @param parts Path parts
   * @return The combined path {@link String}
   */
  public static String combine(String... parts) {
    if (parts == null) {
      return null;
    }

    return Joiner.on(File.separatorChar).skipNulls().join(
        Arrays.stream(parts).map(input -> {
          String stripped = strip(input, File.separatorChar);
          return StringUtils.isEmpty(stripped) ? null : stripped;
        }).collect(Collectors.toList())
    );
  }

  /**
   * Combine multiple strings to a path using the {@link File} separator with leading separator or
   * without.
   *
   * @param leadingSeparator Either state to include the File.separator or not
   * @param parts            Path parts
   * @return The combined path {@link String}
   */
  public static String combine(boolean leadingSeparator, String... parts) {
    String combined = combine(parts);
    return !leadingSeparator ? combined : File.separatorChar + combined;
  }

  /**
   * Returns the extension (determined by checking the last ".") of a path.
   *
   * @param parts Pah parts
   * @return The extension as {@link String} excluding the "." character
   */
  public static String getExtension(String... parts) {
    String combined = combine(parts);
    return Optional.ofNullable(combined)
        .filter(f -> f.contains(String.valueOf(EXTENSION_SEPARATOR)))
        .map(f -> f.substring(combined.lastIndexOf(EXTENSION_SEPARATOR) + 1))
        .orElse(null);
  }

  /**
   * Gets the parent path of a given path.
   *
   * @param path Path part
   * @return Path of the parent
   */
  public static String getParentPath(String... path) {
    return split(combine(path)).getLeft();
  }

  /**
   * Strips all given separators from a given {@link String}. Spaces are always stripped.
   *
   * @param input     The input {@link String}
   * @param separator The separator char to strip
   * @return The stripped {@link String}
   */
  public static String strip(String input, char separator) {
    return StringUtils.strip(input, separator + " ");
  }

  /**
   * Split a given path using the file separator at the last separator. If no separator is present
   * the left part will be null.
   *
   * @param path The path to split
   * @return the split string as tuple
   */
  public static Pair<String, String> split(String path) {
    if (path == null) {
      return Pair.of(null, null);
    }
    int i = path.lastIndexOf(File.separator);
    return 0 < i
        ? Pair.of(path.substring(0, i), path.substring(i + 1))
        : Pair.of((String) null, path);
  }

  @Override
  public void copy(FileSystemNode source, String target) {
    String targetPath = strip(target, File.separatorChar);
    if (!exists(source.getPath())) {
      throw new IllegalArgumentException("File " + source.getPath() + " does not exist.");
    }
    if (source.isDirectory()) {
      List<FileSystemNode> list = list(source);
      if (CollectionUtils.isEmpty(list)) {
        mkDirectories(targetPath);
      } else {
        list.forEach(node -> copy(node, combine(targetPath, node.getName())));
      }
      return;
    }
    if (exists(targetPath)) {
      return;
    }
    copyContent(source, targetPath);
  }

  @Override
  @SuppressWarnings("ConstantConditions")
  public void copy(String source, String target) {
    copy(getForPath(source, true), target);
  }

  @Override
  public byte[] getBytes(FileSystemNode node) {
    try (InputStream inputStream = open(node)) {
      return IOUtils.toByteArray(inputStream);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public byte[] getBytes(String... parts) {
    return getBytes(getFileNode(parts));
  }

  @Override
  public FileSystemNode getFileNode(String... parts) {
    return getForPath(combine(parts), true);
  }

  @Override
  public FileSystemNode getOptionalFileNode(String... parts) {
    return getForPath(combine(parts), false);
  }

  @Override
  public FileSystemNode getParent(FileSystemNode node) {
    return StringUtils.isEmpty(node.getPath()) ? null : getFileNode(node.getParentPath());
  }

  @Override
  public FileSystemNode getParent(String... path) {
    return getForPath(getParentPath(path), false);
  }

  @Override
  public void mkDirectories(String... path) {
    checkPath(path);
    mkDirectories(combine(path));
  }

  @Override
  public List<FileSystemNode> list(String... parts) {
    FileSystemNode node = getOptionalFileNode(parts);
    return node != null ? list(node) : Collections.emptyList();
  }

  @Override
  public InputStream open(String... parts) {
    checkPath(parts);
    return open(getFileNode(parts));
  }

  @Override
  public void save(InputStream is, String... parts) {
    checkPath(parts);
    save(is, combine(parts), -1);
  }

  @Override
  public void save(byte[] bytes, String... parts) {
    checkPath(parts);
    save(new ByteArrayInputStream(bytes), combine(parts), bytes.length);
  }

  @Override
  public void saveText(String content, String... parts) {
    save(content.getBytes(Charset.defaultCharset()), parts);
  }

  @Override
  public String read(FileSystemNode node) {
    try (InputStream inputStream = open(node)) {
      return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private void checkPath(String[] parts) {
    if (parts == null || parts.length == 0) {
      throw new IllegalArgumentException("Operation only possible with path in second argument.");
    }
  }

  @Override
  public String read(String... parts) {
    return read(getFileNode(parts));
  }

  protected abstract void mkDirectories(String path);

  protected abstract void copyContent(FileSystemNode source, String to);

  protected abstract FileSystemNodeType getFileNodeType(String path);

  protected abstract boolean isDirectory(String path);

  protected abstract boolean exists(String path);

  protected abstract long getSize(String path);

  private FileSystemNode getForPath(String parts, boolean shouldExist) {
    if (StringUtils.isBlank(parts)) {
      return new FileSystemNodeImpl(null, "", true, 0);
    }
    String path = strip(parts, File.separatorChar);
    FileSystemNodeType fileSystemNodeType = getFileNodeType(path);
    if (FileSystemNodeType.NONE_EXISTENT == fileSystemNodeType) {
      if (!shouldExist) {
        return null;
      }
      throw new IllegalArgumentException("Path does not exist on filesystem: " + path);
    }
    boolean directory = FileSystemNodeType.DIRECTORY == fileSystemNodeType;
    return createFileNode(path, directory, directory ? 0 : getSize(path));
  }

  protected FileSystemNode createFileNode(String path, boolean isDirectory, long size) {
    Pair<String, String> dirAndName = split(path);
    String parentDir = dirAndName.getLeft();
    String name = dirAndName.getRight();
    return new FileSystemNodeImpl(parentDir, name, isDirectory, size);
  }

  @Override
  public boolean isDirectory(String... parts) {
    return isDirectory(combine(parts));
  }

  @Override
  public boolean exists(String... parts) {
    return exists(combine(parts));
  }

  @Override
  public void delete(String... path) {
    checkPath(path);
    delete(getFileNode(path));
  }

  @Override
  public void delete(FileSystemNode node) {
    delete(node, false);
  }

  @Override
  public void move(String from, String to) {
    copy(from, to);
    final FileSystemNode fromNode = getFileNode(from);
    delete(fromNode, fromNode.isDirectory());
  }

  @Override
  public final long getSize(FileSystemNode node) {
    if (node.isDirectory()) {
      throw new IllegalArgumentException("Size of directory not determined. " + node.getPath());
    }
    return getSize(node.getPath());
  }

  public long countFiles(FileSystemNode node) {
    return defaultFileCount(node);
  }

  protected long defaultFileCount(FileSystemNode node) {
    return list(node).stream().filter(fileNode -> !fileNode.isDirectory()).count();
  }

  @Override
  public void walk(FileSystemNode node, FileNodeVisitor visitor) {
    if (node.isDirectory()) {
      visitor.pre(node);
      list(node).forEach(child -> walk(child, visitor));
      visitor.post(node);
    } else {
      visitor.on(node);
    }
  }
}
