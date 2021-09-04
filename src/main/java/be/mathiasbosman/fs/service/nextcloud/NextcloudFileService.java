package be.mathiasbosman.fs.service.nextcloud;

import be.mathiasbosman.fs.domain.FileSystemNode;
import be.mathiasbosman.fs.domain.FileSystemNodeType;
import be.mathiasbosman.fs.service.AbstractFileService;
import com.github.sardine.DavResource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.aarboard.nextcloud.api.NextcloudConnector;
import org.aarboard.nextcloud.api.webdav.ResourceProperties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
public class NextcloudFileService extends AbstractFileService {

  // as the connector requires File inputs we need to create them locally
  private final FileSystem fileSystem = FileSystems.getDefault();
  private final Path workdir = fileSystem.getPath("/tmp/" + System.identityHashCode(this) + "/");


  private final NextcloudConnector connector;

  public NextcloudFileService(NextcloudConnector connector) {
    this.connector = connector;
  }

  @Override
  protected void mkDirectories(String path) {
    Pair<String, String> split = AbstractFileService.split(path);
    if (split.getLeft() != null) {
      mkDirectories(split.getLeft());
    }
    if (!exists(path)) {
      log.debug("Creating remote folder on {}", path);
      connector.createFolder(path);
    }
  }

  @Override
  protected void copyContent(FileSystemNode source, String to) {
    save(open(source), to);
  }

  @Override
  protected FileSystemNodeType getFileNodeType(String path) {
    if (!exists(path)) {
      return FileSystemNodeType.NONE_EXISTENT;
    }
    return isFile(path) ? FileSystemNodeType.FILE : FileSystemNodeType.DIRECTORY;
  }

  @Override
  protected boolean isDirectory(String path) {
    return DavResource.HTTPD_UNIX_DIRECTORY_CONTENT_TYPE
        .equals(getProperties(path).getContentType());
  }

  private boolean isFile(String path) {
    return !isDirectory(path);
  }

  @Override
  protected boolean exists(String path) {
    return connector.fileExists(path) || connector.folderExists(path);
  }

  @Override
  protected long getSize(String path) {
    return getProperties(path).getSize();
  }

  @Override
  public void delete(FileSystemNode node, boolean recursive) {
    String path = node.getPath();
    if (node.isDirectory()) {
      if (!recursive && !connector.listFolderContent(path).isEmpty()) {
        throw new IllegalStateException("Directory is not empty for deletion.");
      }
      log.debug("Deleting remote directory {}", path);
      connector.deleteFolder(path);
    } else {
      log.debug("Removing remote file {}", path);
      connector.removeFile(path);
    }
  }

  @Override
  public LocalDateTime getCreationTime(FileSystemNode node, ZoneId zoneId) {
    return LocalDateTime.ofInstant(getProperties(node.getPath()).getCreation().toInstant(), zoneId);
  }

  @Override
  public LocalDateTime getLastModifiedTime(FileSystemNode node, ZoneId zoneId) {
    return LocalDateTime.ofInstant(getProperties(node.getPath()).getModified().toInstant(), zoneId);
  }

  @Override
  public String getMimeType(FileSystemNode node) {
    return getProperties(node.getPath()).getContentType();
  }

  @Override
  public List<FileSystemNode> list(FileSystemNode root) {
    if (!root.isDirectory()) {
      throw new IllegalArgumentException("Cannot list contents of a file node: " + root);
    }
    return streamDirectory(root).collect(Collectors.toList());
  }

  @Override
  public InputStream open(FileSystemNode node) {
    try {
      String path = node.getPath();
      log.debug("Downloading file {}", path);
      return connector.downloadFile(path);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void save(InputStream is, String path, long size) {
    try {
      Path tmpPath = workdir.resolve(path);
      String directory = split(path).getLeft();
      if (directory != null) {
        mkDirectories(directory);
        Files.createDirectories(workdir.resolve(tmpPath.getParent().toString()));
      }
      File tmpFile = tmpPath.toFile();
      FileUtils.copyInputStreamToFile(is, tmpFile);
      log.debug("Uploading file {} to remote {}", tmpFile, path);
      connector.uploadFile(tmpFile, path);
      Files.delete(tmpPath);
      Files.deleteIfExists(workdir.resolve(tmpPath.getParent().toString()));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  // if root = x/c @ file = c
  public Stream<FileSystemNode> streamDirectory(FileSystemNode root) {
    if (!root.isDirectory()) {
      throw new IllegalArgumentException("Cannot list contents of a file node: " + root);
    }
    String dirName = split(root.getPath()).getRight();
    String rootPath = root.getPath();
    log.debug("Listing directory content of {}", rootPath);
    return connector.listFolderContent(rootPath).stream()
        .filter(p -> !p.equals(dirName)) // exclude the root itself
        .map(p -> getFileNode(combine(rootPath, p)));
  }

  private ResourceProperties getProperties(String path) {
    try {
      return connector.getProperties(path, true);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
