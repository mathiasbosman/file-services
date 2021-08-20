package be.mathiasbosman.fs.service.aws.s3;

import be.mathiasbosman.fs.domain.FileNode;
import be.mathiasbosman.fs.domain.FileNodeType;
import be.mathiasbosman.fs.service.AbstractFileService;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import java.io.ByteArrayInputStream;
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

public class S3FileService extends AbstractFileService {

  public static final String FOLDER_MARKER_OBJECT_NAME = ".folder";
  public static final String VALID_FILENAME_REGEX = "([0-9]|[A-Z]|[a-z]|[!\\-_.*'()])+";

  private final String bucketName;
  private final String prefix;
  private final AmazonS3 s3;

  public S3FileService(AmazonS3 s3, String bucketName, String prefix) {
    this.s3 = s3;
    this.bucketName = bucketName;
    this.prefix = prefix;
  }

  public S3FileService(AmazonS3Factory factory, String bucketName) {
    this(factory.toAmazonS3(), bucketName);
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
  public void delete(FileNode node, boolean recursive) {
    if (recursive) {
      final List<S3ObjectSummary> objectSummaries = getObjectSummaries(node.getPath());
      for (S3ObjectSummary objectSummary : objectSummaries) {
        delete(objectSummary.getKey());
      }
      return;
    }

    if (node.isDirectory()) {
      List<FileNode> list = list(node, true);
      if (CollectionUtils.size(list) == 1) {
        FileNode sub = list.get(0);
        if (FOLDER_MARKER_OBJECT_NAME.equals(sub.getName())) {
          delete(toObjectKey(sub.getPath()));
          return;
        }
      }
      if (CollectionUtils.isNotEmpty(list)) {
        throw new IllegalArgumentException("Folder not empty for deletion");
      }
      return;
    }
    delete(toObjectKey(node.getPath()));
  }

  private void delete(String key) {
    try {
      s3.deleteObject(bucketName, key);
    } catch (Exception e) {
      throw new RuntimeException(
          "The S3 server threw an array deleting the object with key = " + key, e);
    }
  }

  @Override
  public String getMimeType(FileNode fileNode) {
    return getMetaData(fileNode.getPath()).getContentType();
  }

  @Override
  public boolean isValidFilename(String filename) {
    return isValidObjectKey(filename);
  }

  @Override
  public List<FileNode> list(FileNode root) {
    return list(root, false);
  }

  @Override
  public InputStream open(FileNode node) {
    String key = toObjectKey(node.getPath());
    try {
      return s3.getObject(bucketName, key).getObjectContent();
    } catch (Exception e) {
      throw new RuntimeException("The S3 server threw an error opening " + key, e);
    }
  }

  @Override
  public void save(InputStream is, String path, long size) {
    put(toObjectKey(path), is, toMetadata(size));
  }

  @Override
  public Stream<FileNode> streamDirectory(FileNode root) {
    List<S3ObjectSummary> objectSummaries = getObjectSummaries(root.getPath());
    return objectSummaries.stream().map(s3ObjectSummary -> {
      String location = getLocation(s3ObjectSummary);
      return getFileNode(location);
    });
  }

  @Override
  protected void copyContent(FileNode source, String to) {
    String sourceKey = toObjectKey(source.getPath());
    String destinationKey = toObjectKey(to);

    try {
      s3.copyObject(bucketName, sourceKey, bucketName, destinationKey);
    } catch (Exception e) {
      throw new RuntimeException("The S3 server threw an error when copying from " + sourceKey +
          " to " + destinationKey, e);
    }
  }

  @Override
  protected boolean exists(String path) {
    return isFile(path) || isFolder(path);
  }

  @Override
  protected FileNodeType getFileNodeType(String path) {
    return isFile(path) ? FileNodeType.FILE
        : isFolder(path) ? FileNodeType.FOLDER : FileNodeType.NONE_EXISTENT;
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
  protected boolean isFolder(String path) {
    try {
      final ListObjectsRequest objectList = new ListObjectsRequest(bucketName, toObjectKey(path),
          null, null, 1);
      return CollectionUtils.isNotEmpty(s3.listObjects(objectList).getObjectSummaries());
    } catch (Exception e) {
      throw new RuntimeException("The S3 server threw an error listing objects on " + path, e);
    }
  }

  @Override
  protected void mkFolders(String path) {
    put(combine(toObjectKey(path), FOLDER_MARKER_OBJECT_NAME),
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
      throw new RuntimeException("The S3 server threw an array for the object with key = " + key,
          e);
    }
  }

  ObjectMetadata toMetadata(long size) {
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentEncoding("aws-chunked");
    metadata.setContentType("application/octet-stream");
    if (0 <= size) {
      metadata.setContentLength(size);
    }
    return metadata;
  }

  private List<FileNode> list(FileNode folder, boolean includeHiddenFolderMarkers) {
    List<FileNode> result = new LinkedList<>();
    boolean root = StringUtils.isEmpty(folder.getPath());
    String prefix = root ? "" : folder.getPath() + "/";
    Iterable<S3ObjectSummary> objectListing = getObjectSummaries(folder.getPath());
    Set<String> subfolders = new HashSet<>();
    for (S3ObjectSummary objectSummary : objectListing) {
      String location = getLocation(objectSummary);
      String subPad = location.substring(prefix.length());
      int firstSlash = subPad.indexOf('/');
      if (firstSlash < 0) {
        if (includeHiddenFolderMarkers || !FOLDER_MARKER_OBJECT_NAME.equals(subPad)) {
          result.add(createFileNode(location, false, objectSummary.getSize()));
        }
      } else {
        subfolders.add(prefix + subPad.substring(0, firstSlash));
      }
    }
    for (String subfolder : subfolders) {
      result.add(createFileNode(subfolder, true, 0));
    }
    result.sort(Comparator.comparing(FileNode::getName));
    return result;
  }

  private List<S3ObjectSummary> getObjectSummaries(String path) {
    List<S3ObjectSummary> result = new ArrayList<>();
    AtomicReference<ObjectListing> objectListing = new AtomicReference<>(
        s3.listObjects(bucketName, toObjectKey(path) + "/"));
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
    return s3ObjectSummary.getKey().substring(prefix.length());
  }

  private boolean isFile(String path) {
    try {
      return s3.doesObjectExist(bucketName, toObjectKey(path));
    } catch (Exception e) {
      throw new RuntimeException("The S3 server threw an error accessing " + path, e);
    }
  }

  private String toObjectKey(String path) {
    return combine(prefix, path);
  }
}
