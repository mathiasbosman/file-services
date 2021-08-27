package be.mathiasbosman.fs.service.aws.s3;

import be.mathiasbosman.fs.domain.FileSystemNode;
import be.mathiasbosman.fs.domain.FileSystemNodeType;
import be.mathiasbosman.fs.service.AbstractFileService;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * An implementation of the {@link be.mathiasbosman.fs.service.FileService} for AmazonS3 file
 * systems.
 * <p>
 * A marker object name is used to mock directories.
 *
 * @author mathiasbosman
 * @see AmazonS3
 * @since 0.0.1
 */
public class S3FileService extends AbstractFileService {

  public static final String CONTENT_ENCODING = "aws-chunked";
  public static final String CONTENT_TYPE = "application/octet-stream";
  public static final String DIRECTORY_MARKER_OBJECT_NAME = ".directory";
  public static final String VALID_FILENAME_REGEX = "([0-9A-Za-z!\\-_.*+'()])+";

  private final String bucketName;
  private final String bucketPrefix;
  private final AmazonS3 s3;

  public S3FileService(AmazonS3 s3, String bucketName, String bucketPrefix) {
    this.s3 = s3;
    this.bucketName = bucketName;
    this.bucketPrefix = bucketPrefix;
  }

  public S3FileService(AmazonS3 s3, String bucketName) {
    this(s3, bucketName, "");
  }


  /**
   * Validates a given filename as object key
   *
   * @param filename The filename to validate
   * @return result
   */
  public static boolean isValidObjectKey(String filename) {
    return Pattern.matches(VALID_FILENAME_REGEX, filename);
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
      s3.deleteObject(bucketName, key);
    } catch (Exception e) {
      throw new IllegalStateException(
          "The S3 server threw an array deleting the object with key = " + key, e);
    }
  }

  @Override
  public String getMimeType(FileSystemNode node) {
    return getMetaData(node.getPath()).getContentType();
  }

  @Override
  public boolean isValidFilename(String filename) {
    return isValidObjectKey(filename);
  }

  @Override
  public List<FileSystemNode> list(FileSystemNode root) {
    return list(root, false);
  }

  @Override
  public InputStream open(FileSystemNode node) {
    String key = toObjectKey(node.getPath());
    try {
      return s3.getObject(bucketName, key).getObjectContent();
    } catch (Exception e) {
      throw new IllegalStateException("The S3 server threw an error opening " + key, e);
    }
  }

  @Override
  public void save(InputStream is, String path, long size) {
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
      s3.copyObject(bucketName, sourceKey, bucketName, destinationKey);
    } catch (Exception e) {
      throw new IllegalStateException(
          "The S3 server threw an error when copying from " + sourceKey +
              " to " + destinationKey, e);
    }
  }

  @Override
  protected boolean exists(String path) {
    return isFile(path) || isDirectory(path);
  }

  @Override
  protected FileSystemNodeType getFileNodeType(String path) {
    return isFile(path) ? FileSystemNodeType.FILE
        : isDirectory(path) ? FileSystemNodeType.DIRECTORY : FileSystemNodeType.NONE_EXISTENT;
  }

  @Override
  protected long getSize(String path) {
    ObjectMetadata objectMetadata;
    objectMetadata = getMetaData(path);
    if (objectMetadata == null) {
      throw new IllegalArgumentException("Path does not exist: " + path);
    }
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
