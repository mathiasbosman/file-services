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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public abstract class AbstractFileService implements FileService {

  public static String strip(String path, String separator) {
    return StringUtils.strip(path, separator + " ");
  }

  private static Pair<String, String> split(String path, String separator) {
    int i = path == null ? -1 : path.lastIndexOf(separator);
    return 0 < i
        ? Pair.of(path.substring(0, i), path.substring(i + 1))
        : Pair.of((String) null, path);
  }

  public static String nodeName(String path, String separator) {
    return split(path, separator).getRight();
  }

  public static String combine(String... parts) {
    if (parts == null) {
      return null;
    }

    return Joiner.on("/").skipNulls().join(
        Arrays.stream(parts).map(input -> {
          String stripped = strip(input, "/" + " ");
          return StringUtils.isEmpty(stripped) ? null : stripped;
        }).collect(Collectors.toList())
    );
  }

  protected abstract void mkFolders(String path);

  protected abstract void copyContent(FileNode source, String to);

  protected abstract FileNodeType getFileNodeType(String path);

  protected abstract boolean isFolder(String path);

  protected abstract boolean exists(String path);

  protected abstract long getSize(String path);

  private String createFileNodePath(String parentPath, String name) {
    return combine(parentPath, name);
  }

  protected String strip(String path) {
    return strip(path, File.separator);
  }

  @Override
  public List<FileNode> list(String... parts) {
    FileNode node = getOptionalFileNode(parts);
    return node != null ? list(node) : Collections.emptyList();
  }

  @Override
  public String read(String... parts) {
    return read(get(parts));
  }

  @Override
  @SuppressWarnings("ConstantConditions")
  public void copy(String source, String target) {
    copy(getForPath(source, true), target);
  }

  @Override
  public byte[] getBytes(String... parts) {
    return getBytes(get(parts));
  }

  @Override
  public String read(FileNode node) {
    try (InputStream inputStream = open(node)) {
      return IOUtils.toString(inputStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
  public InputStream open(String... parts) {
    checkPath(parts);
    return open(get(parts));
  }

  @Override
  public void copy(FileNode source, String target) {
    String targetPath = strip(target, File.separator);
    if (!exists(source.getPath())) {
      throw new RuntimeException("File " + source.getPath() + " does not exist.");
    }
    if (source.isFolder()) {
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
  public void mkFolders(String... path) {
    checkPath(path);
    mkFolders(combine(path));
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

  private void checkPath(String[] parts) {
    if (parts == null || parts.length == 0) {
      throw new IllegalArgumentException("Operation only possible with path in second argument.");
    }
  }

  @Override
  public FileNode get(String... parts) {
    return getForPath(combine(parts), true);
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
    return StringUtils.isEmpty(node.getPath()) ? null : get(node.getParentPath());
  }

  @Override
  public FileNode getParent(String... path) {
    return getForPath(getParentPath(path), false);
  }

  @Override
  public String getParentPath(String... path) {
    return split(combine(path), File.separator).getRight();
  }

  private FileNode getForPath(String parts, boolean shouldExist) {
    if (StringUtils.isBlank(parts)) {
      return new FileNodeImpl(null, "", true, 0);
    }
    String path = strip(parts, File.separator);
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
    Pair<String, String> folderAndName = split(path, File.separator);
    String parentFolder = folderAndName.getLeft();
    String name = folderAndName.getRight();
    return new FileNodeImpl(parentFolder, name, isFolder, size);
  }

  @Override
  public boolean isFolder(String... parts) {
    return isFolder(combine(parts));
  }

  @Override
  public boolean exists(String... parts) {
    return exists(combine(parts));
  }

  @Override
  public void delete(String... path) {
    checkPath(path);
    delete(get(path));
  }

  @Override
  public void delete(FileNode node) {
    delete(node, false);
  }

  @Override
  public void move(String from, String to) {
    copy(from, to);
    delete(get(from), true);
  }

  @Override
  public void mv(String from, String to) {
    copy(from, to);
    final FileNode fromNode = get(from);
    delete(fromNode, fromNode.isFolder());
  }

  @Override
  public final long getSize(FileNode node) {
    if (node.isFolder()) {
      throw new IllegalArgumentException("Size of folder not determined. " + node.getPath());
    }
    return getSize(node.getPath());
  }

  public long countFiles(FileNode node) {
    return defaultFileCount(node);
  }

  protected long defaultFileCount(FileNode node) {
    return list(node).stream().filter(fileNode -> !fileNode.isFolder()).count();
  }

  @Override
  public void walk(FileNode node, FileNodeVisitor visitor) {
    if (node.isFolder()) {
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
