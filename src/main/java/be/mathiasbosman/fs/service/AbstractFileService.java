package be.mathiasbosman.fs.service;

import be.mathiasbosman.fs.domain.FileNode;
import be.mathiasbosman.fs.domain.FileNodeImpl;
import be.mathiasbosman.fs.domain.FileNodeType;
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

public abstract class AbstractFileService implements FileService {

  public static final char extensionSeparator = '.';

  /**
   * Combine multiple strings to a path using the {@link File} separator
   *
   * @param parts Path parts
   * @return The combined path {@link String}
   */
  public static String combine(String... parts) {
    if (parts == null) {
      return null;
    }

    return Joiner.on(File.separator).skipNulls().join(
        Arrays.stream(parts).map(input -> {
          String stripped = strip(input, File.separatorChar);
          return StringUtils.isEmpty(stripped) ? null : stripped;
        }).collect(Collectors.toList())
    );
  }

  /**
   * Returns the extension (determined by checking the last ".") of a path
   *
   * @param parts Pah parts
   * @return The extension as {@link String} excluding the "." character
   */
  public static String getExtension(String... parts) {
    String combined = combine(parts);
    return Optional.ofNullable(combined)
        .filter(f -> f.contains(String.valueOf(extensionSeparator)))
        .map(f -> f.substring(combined.lastIndexOf(extensionSeparator) + 1))
        .orElseThrow(() -> new IllegalArgumentException(
            "No '" + extensionSeparator + "' found in path: " + combined));
  }

  /**
   * Gets the parent path of a given path
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

  private static Pair<String, String> split(String path) {
    int i = path == null ? -1 : path.lastIndexOf(File.separator);
    return 0 < i
        ? Pair.of(path.substring(0, i), path.substring(i + 1))
        : Pair.of((String) null, path);
  }

  @Override
  public void copy(FileNode source, String target) {
    String targetPath = strip(target, File.separatorChar);
    if (!exists(source.getPath())) {
      throw new RuntimeException("File " + source.getPath() + " does not exist.");
    }
    if (source.isDirectory()) {
      List<FileNode> list = list(source);
      if (CollectionUtils.isEmpty(list)) {
        mkFolders(targetPath);
      } else {
        for (FileNode node : list) {
          copy(node, combine(targetPath, node.getName()));
        }
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
  public byte[] getBytes(FileNode node) {
    try (InputStream inputStream = open(node)) {
      return IOUtils.toByteArray(inputStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public byte[] getBytes(String... parts) {
    return getBytes(getFileNode(parts));
  }

  @Override
  public FileNode getFileNode(String... parts) {
    return getForPath(combine(parts), true);
  }

  @Override
  public FileNode getOptionalFileNode(String... parts) {
    return getForPath(combine(parts), false);
  }

  @Override
  public FileNode getParent(FileNode node) {
    return StringUtils.isEmpty(node.getPath()) ? null : getFileNode(node.getParentPath());
  }

  @Override
  public FileNode getParent(String... path) {
    return getForPath(getParentPath(path), false);
  }

  @Override
  public void mkFolders(String... path) {
    checkPath(path);
    mkFolders(combine(path));
  }

  @Override
  public List<FileNode> list(String... parts) {
    FileNode node = getOptionalFileNode(parts);
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
  public String read(FileNode node) {
    try (InputStream inputStream = open(node)) {
      return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
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

  protected abstract void mkFolders(String path);

  protected abstract void copyContent(FileNode source, String to);

  protected abstract FileNodeType getFileNodeType(String path);

  protected abstract boolean isFolder(String path);

  protected abstract boolean exists(String path);

  protected abstract long getSize(String path);


  private FileNode getForPath(String parts, boolean shouldExist) {
    if (StringUtils.isBlank(parts)) {
      return new FileNodeImpl(null, "", true, 0);
    }
    String path = strip(parts, File.separatorChar);
    FileNodeType fileNodeType = getFileNodeType(path);
    if (FileNodeType.NONE_EXISTENT == fileNodeType) {
      if (!shouldExist) {
        return null;
      }
      throw new IllegalArgumentException("Path does not exist on filesystem: " + path);
    }
    boolean folder = FileNodeType.FOLDER == fileNodeType;
    return createFileNode(path, folder, folder ? 0 : getSize(path));
  }

  protected FileNode createFileNode(String path, boolean isFolder, long size) {
    Pair<String, String> folderAndName = split(path);
    String parentFolder = folderAndName.getLeft();
    String name = folderAndName.getRight();
    return new FileNodeImpl(parentFolder, name, isFolder, size);
  }

  @Override
  public boolean isDirectory(String... parts) {
    return isFolder(combine(parts));
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
  public void delete(FileNode node) {
    delete(node, false);
  }

  @Override
  public void move(String from, String to) {
    copy(from, to);
    final FileNode fromNode = getFileNode(from);
    delete(fromNode, fromNode.isDirectory());
  }

  @Override
  public final long getSize(FileNode node) {
    if (node.isDirectory()) {
      throw new IllegalArgumentException("Size of folder not determined. " + node.getPath());
    }
    return getSize(node.getPath());
  }

  public long countFiles(FileNode node) {
    return defaultFileCount(node);
  }

  protected long defaultFileCount(FileNode node) {
    return list(node).stream().filter(fileNode -> !fileNode.isDirectory()).count();
  }

  @Override
  public void walk(FileNode node, FileNodeVisitor visitor) {
    if (node.isDirectory()) {
      visitor.pre(node);
      for (FileNode child : list(node)) {
        walk(child, visitor);
      }
      visitor.post(node);
    } else {
      visitor.on(node);
    }
  }
}
