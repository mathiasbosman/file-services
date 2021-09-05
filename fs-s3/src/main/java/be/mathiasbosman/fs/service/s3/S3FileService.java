package be.mathiasbosman.fs.service.s3;

import be.mathiasbosman.fs.core.domain.FileSystemNode;
import be.mathiasbosman.fs.core.domain.FileSystemNodeType;
import be.mathiasbosman.fs.core.service.AbstractFileService;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * An implementation of the {@link be.mathiasbosman.fs.core.service.FileService} for AmazonS3 file
 * systems. A marker object name is used to mock directories.
 *
 * @author mathiasbosman
 * @see AmazonS3
 * @since 0.0.1
 */
@Slf4j
public class S3FileService extends AbstractFileService {

  public static final String CONTENT_ENCODING = "aws-chunked";
  public static final String CONTENT_TYPE = "application/octet-stream";
  public static final String DIRECTORY_MARKER_OBJECT_NAME = ".directory";

  private final String bucketName;
  private final String bucketPrefix;
  private final AmazonS3 s3;

  /**
   * Create the S3FileService.
   *
   * @param s3           The {@link AmazonS3} to use
   * @param bucketName   Name of the bucket used
   * @param bucketPrefix Prefix of the bucket (optional)
   */
  public S3FileService(AmazonS3 s3, String bucketName, String bucketPrefix) {
    this.s3 = s3;
    this.bucketName = bucketName;
    this.bucketPrefix = bucketPrefix;
  }

  public S3FileService(AmazonS3 s3, String bucketName) {
    this(s3, bucketName, "");
  }

  @Override
  public void delete(FileSystemNode node, boolean recursive) {
    if (recursive) {
      final List<S3ObjectSummary> objectSummaries = getObjectSummaries(node.getPath());
      objectSummaries.forEach(s3ObjectSummary -> delete(s3ObjectSummary.getKey()));
      return;
    }

    if (node.isDirectory()) {
      List<FileSystemNode> list = list(node, true);
      if (CollectionUtils.size(list) == 1) {
        FileSystemNode sub = list.get(0);
        if (DIRECTORY_MARKER_OBJECT_NAME.equals(sub.getName())) {
          delete(toObjectKey(sub.getPath()));
          return;
        }
      }
      if (CollectionUtils.isNotEmpty(list)) {
        throw new IllegalArgumentException("Directory not empty for deletion");
      }
      return;
    }
    delete(toObjectKey(node.getPath()));
  }

  private void delete(String key) {
    try {
      log.debug("Deleting {}/{}", bucketName, key);
      s3.deleteObject(bucketName, key);
    } catch (Exception e) {
      throw new IllegalStateException(
          "The S3 server threw an array deleting the object with key = " + key, e);
    }
  }

  @Override
  public LocalDateTime getCreationTime(FileSystemNode node, ZoneId zoneId) {
    throw new UnsupportedOperationException("Amazon S3 does not support creation times.");
  }

  @Override
  public String getMimeType(FileSystemNode node) {
    return getMetaData(node.getPath()).getContentType();
  }

  @Override
  public LocalDateTime getLastModifiedTime(FileSystemNode node, ZoneId zoneId) {
    ObjectMetadata metaData = getMetaData(node.getPath());
    return LocalDateTime.ofInstant(metaData.getLastModified().toInstant(), zoneId);
  }

  @Override
  public List<FileSystemNode> list(FileSystemNode root) {
    return list(root, false);
  }

  private List<FileSystemNode> list(FileSystemNode directory,
      boolean includeHiddenDirectoryMarkers) {
    List<FileSystemNode> result = new LinkedList<>();
    boolean root = StringUtils.isEmpty(directory.getPath());
    String prefix = root ? "" : directory.getPath() + File.separatorChar;
    Iterable<S3ObjectSummary> objectListing = getObjectSummaries(directory.getPath());
    Set<String> subDirs = new HashSet<>();
    for (S3ObjectSummary objectSummary : objectListing) {
      String location = getLocation(objectSummary);
      String subPad = location.substring(prefix.length());
      int firstSlash = subPad.indexOf(File.separatorChar);
      if (firstSlash < 0) {
        if (includeHiddenDirectoryMarkers || !DIRECTORY_MARKER_OBJECT_NAME.equals(subPad)) {
          result.add(createFileNode(location, false, objectSummary.getSize()));
        }
      } else {
        subDirs.add(prefix + subPad.substring(0, firstSlash));
      }
    }
    for (String subDir : subDirs) {
      result.add(createFileNode(subDir, true, 0));
    }
    result.sort(Comparator.comparing(FileSystemNode::getName));
    return result;
  }

  @Override
  public InputStream open(FileSystemNode node) {
    String key = toObjectKey(node.getPath());
    try {
      log.debug("Getting {}/{}", bucketName, key);
      return s3.getObject(bucketName, key).getObjectContent();
    } catch (Exception e) {
      throw new IllegalStateException("The S3 server threw an error opening " + key, e);
    }
  }

  @Override
  protected void save(InputStream is, String path, long size) {
    put(toObjectKey(path), is, toMetadata(size));
  }

  @Override
  public Stream<FileSystemNode> streamDirectory(FileSystemNode root) {
    List<S3ObjectSummary> objectSummaries = getObjectSummaries(root.getPath());
    return objectSummaries.stream().map(s3ObjectSummary -> {
      String location = getLocation(s3ObjectSummary);
      return getFileNode(location);
    });
  }

  @Override
  protected void copyContent(FileSystemNode source, String to) {
    String sourceKey = toObjectKey(source.getPath());
    String destinationKey = toObjectKey(to);

    try {
      log.debug("Copying object {}/{} to {}/{}", bucketName, source, bucketName, destinationKey);
      s3.copyObject(bucketName, sourceKey, bucketName, destinationKey);
    } catch (Exception e) {
      throw new IllegalStateException(
          "The S3 server threw an error when copying from "
              + sourceKey + " to " + destinationKey, e);
    }
  }

  @Override
  protected boolean exists(String path) {
    return isFile(path) || isDirectory(path);
  }

  @Override
  protected FileSystemNodeType getFileNodeType(String path) {
    if (isFile(path)) {
      return FileSystemNodeType.FILE;
    }
    return isDirectory(path) ? FileSystemNodeType.DIRECTORY : FileSystemNodeType.NONE_EXISTENT;
  }

  @Override
  protected long getSize(String path) {
    ObjectMetadata objectMetadata;
    objectMetadata = getMetaData(path);
    return objectMetadata.getContentLength();
  }

  @Override
  protected boolean isDirectory(String path) {
    try {
      final ListObjectsRequest objectList = new ListObjectsRequest(bucketName, toObjectKey(path),
          null, null, 1);
      return CollectionUtils.isNotEmpty(s3.listObjects(objectList).getObjectSummaries());
    } catch (Exception e) {
      throw new IllegalStateException("The S3 server threw an error listing objects on " + path, e);
    }
  }

  @Override
  protected void mkDirectories(String path) {
    put(combine(toObjectKey(path), DIRECTORY_MARKER_OBJECT_NAME),
        new ByteArrayInputStream(new byte[]{1}), toMetadata(1));
  }

  ObjectMetadata getMetaData(String path) {
    final String key = toObjectKey(path);
    return s3.getObjectMetadata(bucketName, key);
  }

  void put(String key, InputStream is, ObjectMetadata metadata) {
    try {
      log.debug("Putting object {}/{}", bucketName, key);
      s3.putObject(bucketName, key, is, metadata);
    } catch (Exception e) {
      throw new IllegalStateException(
          "The S3 server threw an array for the object with key = " + key,
          e);
    }
  }

  ObjectMetadata toMetadata(long size) {
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentEncoding(CONTENT_ENCODING);
    metadata.setContentType(CONTENT_TYPE);
    if (0 <= size) {
      metadata.setContentLength(size);
    }
    return metadata;
  }

  private List<S3ObjectSummary> getObjectSummaries(String path) {
    List<S3ObjectSummary> result = new ArrayList<>();
    AtomicReference<ObjectListing> objectListing = new AtomicReference<>(
        s3.listObjects(bucketName, toObjectKey(path) + File.separatorChar));
    while (true) {
      result.addAll(objectListing.get().getObjectSummaries());
      if (!objectListing.get().isTruncated()) {
        break;
      }
      objectListing.set(s3.listNextBatchOfObjects(objectListing.get()));
    }
    return result;
  }

  private String getLocation(S3ObjectSummary s3ObjectSummary) {
    return s3ObjectSummary.getKey().substring(bucketPrefix.length());
  }

  private boolean isFile(String path) {
    try {
      return s3.doesObjectExist(bucketName, toObjectKey(path));
    } catch (Exception e) {
      throw new IllegalStateException("The S3 server threw an error accessing " + path, e);
    }
  }

  private String toObjectKey(String path) {
    return combine(bucketPrefix, path);
  }
}
