package be.mathiasbosman.fs.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.shouldHaveThrown;
import static org.junit.Assert.assertThrows;

import be.mathiasbosman.fs.domain.FileNode;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

public abstract class AbstractFileServiceTest {

  private static final Function<FileNode, String> toPath = FileNode::getPath;
  private FileService fs;

  protected FileService getFs() {
    return fs;
  }

  protected void setFs(FileService fs) {
    this.fs = fs;
  }

  protected abstract void putFolder(String path);
  protected abstract void putObject(String path, String data);
  protected abstract void putImageObject(String path);
  protected abstract void assertExists(String path);
  protected abstract void assertFolderExists(String path);
  protected abstract void assertNotExists(String path);
  protected abstract String getContent(String path);

  @Test
  public void exists() {
    assertThat(fs.exists("-")).isFalse();
    putObject("b", "-");
    assertThat(fs.exists("b")).isTrue();
  }

  @Test
  public void save() {
    fs.save(stringToInputStream("-"), "x/y");
    assertExists("x/y");
    assertThat(getContent("x/y")).isEqualTo("-");
    fs.save(stringToInputStream("+"), "a/b/c");
    assertThat(getContent("a/b/c")).isEqualTo("+");
    // check if parent folders are created
    fs.save(stringToInputStream("-"), "1/2/3");
    assertFolderExists("1/2");
    assertFolderExists("1");
    // check creation from path
    assertLocationAndName(fs.get("1"), null, "1");
    assertLocationAndName(fs.get("/1"), null, "1");
    assertLocationAndName(fs.get("1/"), null, "1");
    assertLocationAndName(fs.get("1/2"), "1", "2");
    assertLocationAndName(fs.get("1/2/3"), "1/2", "3");
  }

  @Test
  public void getFileNode() {
    try {
      fs.get("x/y");
      shouldHaveThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("Path does not exist on filesystem: x/y");
    }
    // test file
    putObject("x/y", "-");
    FileNode file = fs.get("x/y");
    assertThat(file.getParentPath()).isEqualTo("x");
    assertThat(file.getName()).isEqualTo("y");
    assertThat(file.getPath()).isEqualTo("x/y");
    assertThat(file.isFolder()).isFalse();
    // test folder
    putFolder("z");
    FileNode folder = fs.get("z");
    assertThat(folder.getParentPath()).isNull();
    assertThat(folder.getName()).isEqualTo("z");
    assertThat(folder.getPath()).isEqualTo("z");
    assertThat(folder.isFolder()).isTrue();
  }

  @Test
  public void getParent() {
    fs.save(stringToInputStream("testContent"), "a/b/c.txt");
    FileNode c = fs.get("a", "b/c.txt");
    FileNode b = fs.getParent(c);
    assertThat(b).isNotNull();
    FileNode a = fs.getParent(b);
    assertThat(a).isNotNull();
    FileNode root = fs.getParent(a);
    assertThat(root).isNotNull();
    FileNode beyondRoot = fs.getParent(root);
    assertThat(beyondRoot).isNull();
  }

  @Test
  public void mkFolders() {
    fs.mkFolders("x");
    assertFolderExists("x");
  }

  @Test
  public void read() {
    putObject("x/y", "-");
    FileNode file = fs.get("x/y");
    assertThat(fs.read(file)).isEqualTo("-");
  }

  @Test
  public void isFolder() {
    assertThat(fs.isFolder("-")).isFalse();
    putObject("x/.folder", "");
    assertThat(fs.get("x").isFolder()).isTrue();
    assertThat(fs.get("/x").isFolder()).isTrue();
    assertThat(fs.get("x/").isFolder()).isTrue();
  }

  @Test
  public void delete() {
    putObject("x/y", "-");
    fs.delete("x/y");
    assertNotExists("x/y");
  }

  @Test
  public void list() {
    putObject("x/a", "John");
    putObject("x/b", "Doe");
    FileNode x = fs.get("x");
    List<FileNode> list = fs.list(x);
    assertThat(transformToPath(list)).containsExactly("x/a", "x/b");
    Iterable<FileNode> filter = Iterables.filter(list, new ByFiles());
    Iterable<String> transform = StreamSupport.stream(filter.spliterator(), false).map(fileNode -> {
      try {
        return IOUtils.toString(fs.open(fileNode), StandardCharsets.UTF_8);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }).collect(Collectors.toList());
    assertThat(transform).containsExactly("John", "Doe");
    // check size
    assertThat(list.size()).isEqualTo(2);
    // check if folders are included
    putFolder("x");
    putFolder("x/c");
    putObject("x/c/1", "-");
    putFolder("x/c/d");
    putObject("y/e","-");
    putObject("z", "-");
    assertThat(transformToPath(fs.list(fs.get("x")))).containsExactly("x/a","x/b","x/c");
    List<FileNode> subList = fs.list("x/c");
    assertThat(transformToPath(subList)).containsExactly("x/c/1","x/c/d");
  }

  @Test
  public void walk() {
    putFolder("x");
    putObject("x/a", "-");
    putObject("x/b", "-");
    putFolder("x/c");
    putObject("x/c/1", "-");
    putFolder("x/c/d");
    putFolder("y");
    putObject("y/e", "-");
    putObject("z", "-");
    SpyingTreeVisitor spy = new SpyingTreeVisitor();
    for (FileNode file : fs.list()) {
      fs.walk(file, spy);
    }
    assertThat(Arrays.asList("x/a", "x/b", "x/c/1", "y/e", "z")).isEqualTo(spy.visitedFiles);
    assertThat(Arrays.asList("x", "x/c", "x/c/d", "y")).isEqualTo(spy.visitedFolders);
    assertThat(Arrays.asList("> x", "x/a", "x/b", "> x/c", "x/c/1", "> x/c/d", "< x/c/d", "< x/c", "< x", "> y", "y/e", "< y", "z")).isEqualTo(spy.visitationOrder);
  }

  @Test
  public void countFiles() {
    putObject("x/a", "-");
    putObject("x/z", "-");
    putObject("x/b/a", "-");
    assertThat(fs.countFiles(fs.get("x"))).isEqualTo(2);
  }

  @Test
  public void getMimeType() {
    putImageObject("a.jpeg");
    putImageObject("x/a.jpg");
    assertThat(fs.getMimeType(fs.get("a.jpeg"))).isEqualTo("image/jpeg");
    assertThat(fs.getMimeType(fs.get("x/a.jpg"))).isEqualTo("image/jpeg");
  }

  @Test
  public void getExtension() {
    assertThrows(IllegalArgumentException.class, () -> AbstractFileService.getExtension("a"));
    assertThat(AbstractFileService.getExtension("a.jpeg")).isEqualTo("jpeg");
    assertThat(AbstractFileService.getExtension("a","b","c.xml")).isEqualTo("xml");
  }

  private void assertLocationAndName(FileNode node, String expectedParentPath, String expectedName) {
    assertThat(node.getParentPath()).isEqualTo(expectedParentPath);
    assertThat(node.getName()).isEqualTo(expectedName);
  }

  private InputStream stringToInputStream(String input) {
    return new ByteArrayInputStream(input.getBytes());
  }

  private Iterable<String> transformToPath(Iterable<FileNode> nodes) {
    return StreamSupport.stream(nodes.spliterator(), false).map(toPath)
        .collect(Collectors.toList());
  }

  private static class ByFiles implements Predicate<FileNode> {

    @Override
    public boolean apply(FileNode fileNode) {
      assert fileNode != null;
      return !fileNode.isFolder();
    }
  }

  private static class SpyingTreeVisitor implements FileNodeVisitor {
    private final List<String> visitationOrder = new ArrayList<>();
    private final List<String> visitedFiles = new ArrayList<>();
    private final List<String> visitedFolders = new ArrayList<>();

    @Override
    public void on(FileNode file) {
      visitedFiles.add(file.getPath());
      visitationOrder.add(file.getPath());
    }

    @Override
    public void pre(FileNode folder) {
      visitedFolders.add(folder.getPath());
      visitationOrder.add("> " + folder.getPath());
    }

    @Override
    public void post(FileNode folder) {
      visitationOrder.add("< " + folder.getPath());
    }
  }
}