package be.mathiasbosman.fs.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.shouldHaveThrown;

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

  protected abstract void assertExists(String path);

  protected abstract void assertFolderExists(String path);

  protected abstract void assertNotExists(String path);

  protected abstract String getContent(String path);

  protected abstract void putFolder(String path);

  protected abstract void putObject(String path, String data);

  protected abstract void putImageObject(String path);

  @Test
  void combine() {
    assertThat(AbstractFileService.combine("x", "y", "z")).isEqualTo("x/y/z");
    assertThat(AbstractFileService.combine("x/y", "/z")).isEqualTo("x/y/z");
    assertThat(AbstractFileService.combine(false, "x/y", "/z"))
        .isEqualTo("x/y/z");
    assertThat(AbstractFileService.combine(true, "x", "y", "z"))
        .isEqualTo("/x/y/z");
  }

  @Test
  void countFiles() {
    putObject("x/a", "-");
    putObject("x/z", "-");
    putObject("x/b/a", "-");
    assertThat(fs.countFiles(fs.getFileNode("x"))).isEqualTo(2);
  }

  @Test
  void delete() {
    putObject("x/y", "-");
    fs.delete("x/y");
    assertNotExists("x/y");
  }

  @Test
  void exists() {
    assertThat(fs.exists("-")).isFalse();
    putObject("b", "-");
    assertThat(fs.exists("b")).isTrue();
  }

  @Test
  void getBytes() {
    String content = "John";
    putObject("x/a", content);
    assertThat(fs.getBytes("x/a")).isEqualTo(content.getBytes());
    assertThat(fs.getBytes("x", "a")).isEqualTo(content.getBytes());
    assertThat(fs.getBytes(fs.getFileNode("x/a"))).isEqualTo(content.getBytes());
  }

  @Test
  void getExtension() {
    assertThat(AbstractFileService.getExtension((String) null)).isNull();
    assertThat(AbstractFileService.getExtension("")).isNull();
    assertThat(AbstractFileService.getExtension("a")).isNull();
    assertThat(AbstractFileService.getExtension("a.jpeg")).isEqualTo("jpeg");
    assertThat(AbstractFileService.getExtension("a", "b", "c.xml")).isEqualTo("xml");
  }

  @Test
  void getFileNode() {
    try {
      fs.getFileNode("x/y");
      shouldHaveThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("Path does not exist on filesystem: x/y");
    }
    // test file
    putObject("x/y", "-");
    FileNode file = fs.getFileNode("x/y");
    assertThat(file.getParentPath()).isEqualTo("x");
    assertThat(file.getName()).isEqualTo("y");
    assertThat(file.getPath()).isEqualTo("x/y");
    assertThat(file.isDirectory()).isFalse();
    // test folder
    putFolder("z");
    FileNode folder = fs.getFileNode("z");
    assertThat(folder.getParentPath()).isNull();
    assertThat(folder.getName()).isEqualTo("z");
    assertThat(folder.getPath()).isEqualTo("z");
    assertThat(folder.isDirectory()).isTrue();
  }

  @Test
  void getMimeType() {
    putImageObject("a.jpeg");
    putImageObject("x/a.jpg");
    assertThat(fs.getMimeType(fs.getFileNode("a.jpeg"))).isEqualTo("image/jpeg");
    assertThat(fs.getMimeType(fs.getFileNode("x/a.jpg"))).isEqualTo("image/jpeg");
  }

  @Test
  void getParent() {
    fs.save(stringToInputStream("testContent"), "a/b/c.txt");
    FileNode c = fs.getFileNode("a", "b/c.txt");
    FileNode cParent = fs.getParent(c);
    FileNode cParentBis = fs.getParent("a", "b/c.txt");
    assertLocationAndName(cParent, "a", "b");
    assertLocationAndName(cParentBis, "a", "b");
    FileNode cParentParent = fs.getParent(cParent);
    assertThat(cParentParent).isNotNull();
    FileNode root = fs.getParent(cParentParent);
    assertThat(root).isNotNull();
    FileNode beyondRoot = fs.getParent(root);
    assertThat(beyondRoot).isNull();
  }

  @Test
  void getParentPath() {
    assertThat(AbstractFileService.getParentPath("a/b/c.txt")).isEqualTo("a/b");
  }

  @Test
  void getSize() {
    putObject("x/a", "John");
    assertThat(fs.getSize(fs.getFileNode("x/a"))).isEqualTo(4);
  }

  @Test
  void isFolder() {
    assertThat(fs.isDirectory("-")).isFalse();
    putObject("x/.folder", "");
    assertThat(fs.getFileNode("x").isDirectory()).isTrue();
    assertThat(fs.getFileNode("/x").isDirectory()).isTrue();
    assertThat(fs.getFileNode("x/").isDirectory()).isTrue();
  }

  @Test
  void list() {
    putObject("x/a", "John");
    putObject("x/b", "Doe");
    FileNode x = fs.getFileNode("x");
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
    putObject("y/e", "-");
    putObject("z", "-");
    assertThat(transformToPath(fs.list(fs.getFileNode("x")))).containsExactly("x/a", "x/b", "x/c");
    List<FileNode> subList = fs.list("x/c");
    assertThat(transformToPath(subList)).containsExactly("x/c/1", "x/c/d");
  }

  @Test
  void mkFolders() {
    fs.mkFolders("x");
    assertFolderExists("x");
  }

  @Test
  void move() {
    putFolder("x/source");
    putObject("x/source/y.txt", "-");
    putFolder("x/target");
    fs.move("x/source/y.txt", "x/target/y.txt");
    assertExists("x/target/y.txt");
  }

  @Test
  void open() {
    putObject("x/a", "John");
    assertThat(fs.open("x/a")).hasContent("John");
    assertThat(fs.open("x", "a")).hasContent("John");
    assertThat(fs.open(fs.getFileNode("x/a"))).hasContent("John");
  }


  @Test
  void save() {
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
    assertLocationAndName(fs.getFileNode("1"), null, "1");
    assertLocationAndName(fs.getFileNode("/1"), null, "1");
    assertLocationAndName(fs.getFileNode("1/"), null, "1");
    assertLocationAndName(fs.getFileNode("1/2"), "1", "2");
    assertLocationAndName(fs.getFileNode("1/2/3"), "1/2", "3");
  }

  @Test
  void read() {
    putObject("x/y", "-");
    FileNode file = fs.getFileNode("x/y");
    assertThat(fs.read(file)).isEqualTo("-");
  }

  @Test
  void saveText() {
    fs.saveText("-", "x/y/z.txt");
    assertThat(getContent("x/y/z.txt")).isEqualTo("-");
  }

  @Test
  void walk() {
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
    assertThat(Arrays
        .asList("> x", "x/a", "x/b", "> x/c", "x/c/1", "> x/c/d", "< x/c/d", "< x/c", "< x", "> y",
            "y/e", "< y", "z")).isEqualTo(spy.visitationOrder);
  }

  private void assertLocationAndName(FileNode node, String expectedParentPath,
      String expectedName) {
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
      return !fileNode.isDirectory();
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