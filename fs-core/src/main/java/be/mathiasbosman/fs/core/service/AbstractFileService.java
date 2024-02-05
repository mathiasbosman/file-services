package be.mathiasbosman.fs.core.service;

import be.mathiasbosman.fs.core.domain.FileServiceException;
import be.mathiasbosman.fs.core.domain.FileSystemNode;
import be.mathiasbosman.fs.core.domain.FileSystemNodeType;
import be.mathiasbosman.fs.core.domain.NodeMetadata;
import be.mathiasbosman.fs.core.util.FileServiceUtils;
import be.mathiasbosman.fs.core.util.ZipEntryInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Abstract implementation of {@link FileService} for path manipulation.
 */
public abstract class AbstractFileService implements FileService {

  protected abstract FileSystemNodeType getFileNodeType(String path);

  protected abstract void copyContent(FileSystemNode source, String to);

  protected abstract NodeMetadata getNodeMetadata(String path);

  @Override
  public void copy(FileSystemNode source, String target) {
    String targetPath = FileServiceUtils.strip(target);
    if (!exists(source.path())) {
      throw new IllegalArgumentException("File " + source.path() + " does not exist.");
    }
    if (source.isDirectory()) {
      List<FileSystemNode> list = list(source);
      if (CollectionUtils.isEmpty(list)) {
        mkDirectories(targetPath);
      } else {
        list.forEach(node -> copy(node, FileServiceUtils.combine(targetPath, node.name())));
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
    return getForPath(FileServiceUtils.combine(parts), true);
  }

  @Override
  public FileSystemNode getOptionalFileNode(String... parts) {
    return getForPath(FileServiceUtils.combine(parts), false);
  }

  @Override
  public FileSystemNode getParent(FileSystemNode node) {
    return StringUtils.isEmpty(node.path()) ? null : getFileNode(node.parentPath());
  }

  @Override
  public FileSystemNode getParent(String... path) {
    return getForPath(FileServiceUtils.getParentPath(path), false);
  }

  @Override
  public void mkDirectories(String... path) {
    checkPath(path);
    mkDirectories(FileServiceUtils.combine(path));
  }

  protected abstract void mkDirectories(String path);

  static void checkPath(@NonNull String[] parts) {
    if (parts.length == 0) {
      throw new IllegalArgumentException("Operation only possible with path in second argument.");
    }
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
    save(is, FileServiceUtils.combine(parts), -1);
  }

  protected abstract void save(InputStream is, String path, long size);

  @Override
  public void save(byte[] bytes, String... parts) {
    checkPath(parts);
    save(new ByteArrayInputStream(bytes), FileServiceUtils.combine(parts), bytes.length);
  }

  @Override
  public void saveText(String content, String... parts) {
    save(content.getBytes(Charset.defaultCharset()), parts);
  }

  @Override
  public String read(FileSystemNode node) {
    try (InputStream inputStream = open(node)) {
      return IOUtils.toString(inputStream, Charset.defaultCharset());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public String read(String... parts) {
    return read(getFileNode(parts));
  }

  @Override
  public boolean isDirectory(String... parts) {
    return exists(parts) && isDirectory(FileServiceUtils.combine(parts));
  }

  protected abstract boolean isDirectory(String path);

  @Override
  public boolean exists(String... parts) {
    return exists(FileServiceUtils.combine(parts));
  }

  protected abstract boolean exists(String path);

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
  public long getSize(FileSystemNode node) {
    if (node.isDirectory()) {
      return streamDirectory(node).mapToLong(FileSystemNode::size).sum();
    }
    return getSize(node.path());
  }

  protected abstract long getSize(String path);

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

  @Override
  public void zip(String path, OutputStream outputStream) {
    zip(path, outputStream, null);
  }

  @Override
  public void zip(String path, OutputStream outputStream, String prefix) {
    try (final ZipOutputStream zipStream = new ZipOutputStream(outputStream)) {
      walk(getFileNode(path), new FileNodeVisitor() {
        @Override
        public void on(FileSystemNode node) {
          add(node, true);
        }

        private void add(FileSystemNode node, boolean file) {
          final String nodePath = node.path();
          try {
            String inZipPath = FileServiceUtils.combine(prefix,
                StringUtils.substringAfter(nodePath, path));
            if (StringUtils.isEmpty(inZipPath)) {
              return;
            }
            String path = file ? inZipPath : FileServiceUtils.appendSeparator(inZipPath);
            zipStream.putNextEntry(new ZipEntry(path));
            if (file) {
              try (InputStream stream = open(node)) {
                IOUtils.copy(stream, zipStream);
              }
            }
          } catch (Exception e) {
            throw new FileServiceException("Problem while zipping node " + nodePath);
          }
        }

        @Override
        public void pre(FileSystemNode node) {
          add(node, false);
        }

        @Override
        public void post(FileSystemNode node) {
          // no op
        }
      });
    } catch (Exception e) {
      throw new FileServiceException(e);
    }
  }

  @Override
  public void unzip(ZipInputStream input, final String target) {
    unzip(input, target, FileServiceUtils.always);
  }

  @Override
  public void unzip(ZipInputStream input, String target, Predicate<ZipEntry> entryPredicate) {
    unzip(input, target, entryPredicate, FileServiceUtils.noConsumer);
  }

  @Override
  public void unzip(ZipInputStream input, final String target, Predicate<ZipEntry> predicate,
      Consumer<ZipEntry> consumer) {
    final Consumer<ZipEntryInputStream> fileConsumer = s -> save(s,
        FileServiceUtils.combine(target, s.getZipEntry().getName()));
    final Consumer<ZipEntry> folderConsumer = e -> mkDirectories(target, e.getName());
    FileServiceUtils.walk(input, predicate, consumer, fileConsumer, folderConsumer);
  }

  protected FileSystemNode createFileNode(String path, boolean isDirectory, long size,
      Date lastModified) {
    Pair<String, String> dirAndName = FileServiceUtils.split(path);
    String parentDir = dirAndName.getLeft();
    String name = dirAndName.getRight();
    return new FileSystemNode(parentDir, name, isDirectory, size, lastModified);
  }

  protected FileSystemNode createDirectoryNode(String path, Date lastModified) {
    return createFileNode(path, true, 0L, lastModified);
  }

  /**
   * Counts the files in a given node. Implements {@link AbstractFileService#defaultFileCount} by
   * default.
   *
   * @param node The node to count files in
   * @return amount of files
   */
  public long countFiles(FileSystemNode node) {
    return defaultFileCount(node);
  }

  protected long defaultFileCount(FileSystemNode node) {
    return list(node).stream().filter(fileNode -> !fileNode.isDirectory()).count();
  }

  private FileSystemNode getForPath(String parts, boolean shouldExist) {
    if (StringUtils.isBlank(parts)) {
      return createFileNode("", true, 0, null);
    }
    String path = FileServiceUtils.strip(parts);
    NodeMetadata nodeMetadata = getNodeMetadata(path);
    if (nodeMetadata == null) {
      if (!shouldExist) {
        return null;
      }
      throw new IllegalArgumentException("Path does not exist on filesystem: " + path);
    }
    boolean directory = nodeMetadata.isDirectory();
    return createFileNode(path, directory, directory ? 0 : getSize(path),
        nodeMetadata.lastModified());
  }

}
