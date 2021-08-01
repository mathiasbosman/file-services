package be.mathiasbosman.fs.service;

import be.mathiasbosman.fs.domain.FileNode;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

public interface FileService {

  List<FileNode> list(String... parts);

  List<FileNode> list(FileNode root);

  InputStream open(FileNode node);

  InputStream open(String... parts);

  void copy(String source, String target);

  void copy(FileNode source, String target);

  byte[] getBytes(String... parts);

  byte[] getBytes(FileNode node);

  String read(FileNode node);

  String read(String... parts);

  void mkFolders(String... path);

  void save(InputStream is, String path, long size);

  void save(InputStream is, String... parts);

  void save(byte[] bytes, String... parts);

  void saveText(String content, String... parts);

  Stream<FileNode> streamDirectory(FileNode root);

  FileNode get(String... parts);

  long getSize(FileNode node);

  FileNode getFileNode(String... parts);

  FileNode getOptionalFileNode(String... parts);

  FileNode getParent(FileNode node);

  FileNode getParent(String... path);

  String getParentPath(String... path);

  boolean isFolder(String... parts);

  boolean exists(String... parts);

  void delete(String... path);

  void delete(FileNode node);

  void delete(FileNode node, boolean recursive);

  /**
   * Als ik me niet vergis werkt dit enkel voor folders.
   */
  void move(String from, String to);

  /**
   * Pad from hoeft geen folder te zijn. Omdat deze lib eventueel op meerdere plaatsen
   * gebruikt wordt, op veilig gespeeld en een bijkomende methode toegevoegd. Zou kunnen
   * gemerged worden met move algemeen.
   */
  void mv(String from, String to);

  void walk(FileNode root, FileNodeVisitor visitor);

  long countFiles(FileNode fileNode);
}
