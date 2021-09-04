package be.mathiasbosman.fs.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import be.mathiasbosman.fs.domain.FileSystemNode;
import com.amazonaws.util.StringInputStream;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

public abstract class AbstractFileServiceTest {

  private static final Function<FileSystemNode, String> toPath = FileSystemNode::getPath;
  private FileService fs;

  protected FileService getFs() {
    return fs;
  }

  protected void setFs(FileService fs) {
    this.fs = fs;
  }

  protected abstract void assertExists(String path);

  protected abstract void assertDirectoryExists(String path);

  protected abstract void assertNotExists(String path);

  protected abstract String getContent(String path);

  protected abstract void putDirectory(String path);

  protected abstract void putObject(String path, String data);

  protected abstract void putImageObject(String path);

  protected abstract String getRemotePath(String path);

  @Test
  void countFiles() {
    putObject("x/a", "-");
    putObject("x/z", "-");
    putObject("x/b/a", "-");
    assertThat(fs.countFiles(fs.getFileNode(getRemotePath("x")))).isEqualTo(2);
  }

  @Test
  void copy() throws Exception {
    FileService fs = getFs();
    fs.save(new StringInputStream("information"), getRemotePath("dir/file.txt"));
    fs.save(new StringInputStream("more data"), getRemotePath("dir/more.txt"));
    fs.copy(fs.getFileNode(getRemotePath("dir")), getRemotePath("copy"));
    assertThat(fs.exists(getRemotePath("copy/more.txt"))).isTrue();
    assertThat(fs.read(getRemotePath("copy/file.txt"))).isEqualTo("information");
    assertThatThrownBy(
        () -> fs.copy(fs.getFileNode(getRemotePath("mock.txt")), getRemotePath("target")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("mock.txt")
        .hasMessageContaining("does not exist");
    fs.mkDirectories(getRemotePath("sourceDir"));
    fs.copy(getRemotePath("sourceDir"), getRemotePath("targetDir"));
    fs.copy(fs.getFileNode(getRemotePath("dir")), getRemotePath("copy"));
  }

  @Test
  void delete() {
    putObject("x/y", "-");
    fs.delete(getRemotePath("x/y"));
    assertNotExists("x/y");
    assertExists("x");
    putObject("a/b/c", "-");
    fs.delete(fs.getFileNode(getRemotePath("a")), true);
    assertNotExists("a");
  }

  @Test
  void exists() {
    assertThat(fs.exists(getRemotePath("-"))).isFalse();
    putObject("b", "-");
    assertThat(fs.exists(getRemotePath("b"))).isTrue();
  }

  @Test
  void getBytes() {
    String content = "John";
    putObject("x/a", content);
    assertThat(fs.getBytes(getRemotePath("x/a"))).isEqualTo(content.getBytes());
    assertThat(fs.getBytes(getRemotePath("x", "a"))).isEqualTo(content.getBytes());
    assertThat(fs.getBytes(fs.getFileNode(getRemotePath("x/a")))).isEqualTo(content.getBytes());
  }

  @Test
  void getFileNode() {
    assertThatThrownBy(() -> fs.getFileNode(getRemotePath("x/y")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Path does not exist on filesystem:");
    // test file
    putObject("x/y", "-");
    FileSystemNode file = fs.getFileNode(getRemotePath("x/y"));
    assertThat(file.getParentPath()).isEqualTo(getRemotePath("x"));
    assertThat(file.getName()).isEqualTo("y");
    assertThat(file.getPath()).isEqualTo(getRemotePath("x/y"));
    assertThat(file.isDirectory()).isFalse();
    assertThat(file.getSize()).isPositive();
    // test directory
    putDirectory("z");
    FileSystemNode directory = fs.getFileNode(getRemotePath("z"));
    assertThat(directory.getName()).isEqualTo("z");
    assertThat(directory.getPath()).isEqualTo(getRemotePath("z"));
    assertThat(directory.isDirectory()).isTrue();
    assertThat(directory.getSize()).isZero();
  }

  @Test
  void getOptionalFileNode() {
    putObject("x/y", "-");
    assertThat(getFs().getOptionalFileNode(getRemotePath("x/y"))).isNotNull();
    assertThat(getFs().getOptionalFileNode(getRemotePath("x/z"))).isNull();
  }

  @Test
  void getMimeType() {
    putImageObject("a.jpeg");
    putImageObject("x/a.jpg");
    assertThat(fs.getMimeType(fs.getFileNode(getRemotePath("a.jpeg")))).isEqualTo("image/jpeg");
    assertThat(fs.getMimeType(fs.getFileNode(getRemotePath("x/a.jpg")))).isEqualTo("image/jpeg");
  }

  @Test
  void getParent() {
    fs.save(stringToInputStream("testContent"), getRemotePath("a/b/c.txt"));
    FileSystemNode c = fs.getFileNode(getRemotePath("a", "b/c.txt"));
    FileSystemNode cParent = fs.getParent(c);
    FileSystemNode cParentBis = fs.getParent(getRemotePath("a", "b/c.txt"));
    assertLocationAndName(cParent, getRemotePath("a"), "b");
    assertLocationAndName(cParentBis, getRemotePath("a"), "b");
    FileSystemNode cParentParent = fs.getParent(cParent);
    assertThat(cParentParent).isNotNull();
    FileSystemNode root = fs.getParent(cParentParent);
    assertThat(root).isNotNull();
  }

  @Test
  void getSize() {
    putObject("x/a", "John");
    assertThat(fs.getSize(fs.getFileNode(getRemotePath("x/a")))).isEqualTo(4);
    putDirectory("dir");
    assertThatThrownBy(() -> fs.getSize(fs.getFileNode(getRemotePath("dir"))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Size of directory not determined.");
  }

  @Test
  void isDirectory() {
    assertThat(fs.isDirectory(getRemotePath("-"))).isFalse();
    putDirectory("x/.directory");
    assertThat(fs.getFileNode(getRemotePath("x")).isDirectory()).isTrue();
    assertThat(fs.getFileNode(getRemotePath("/x")).isDirectory()).isTrue();
    assertThat(fs.getFileNode(getRemotePath("x/")).isDirectory()).isTrue();
    putObject("x/.directory/y", "-");
    assertThat(fs.getFileNode(getRemotePath("x/.directory/y")).isDirectory()).isFalse();
  }

  @Test
  void list() {
    putObject("x/a", "John");
    putObject("x/b", "Doe");
    FileSystemNode x = fs.getFileNode(getRemotePath("x"));
    List<FileSystemNode> list = fs.list(x);
    assertThat(transformToPath(list)).containsExactly(
        getRemotePath("x/a"), getRemotePath("x/b"));
    Iterable<FileSystemNode> filter = Iterables.filter(list, new ByFiles());
    Iterable<String> transform = StreamSupport.stream(filter.spliterator(), false)
        .map(fileNode -> inputStreamToString(fs.open(fileNode)))
        .collect(Collectors.toList());
    assertThat(transform).containsExactly("John", "Doe");
    // check size
    assertThat(list.size()).isEqualTo(2);
    // check if directories are included
    putDirectory("x");
    putDirectory("x/c");
    putObject("x/c/1", "-");
    putDirectory("x/c/d");
    putObject("y/e", "-");
    assertThat(transformToPath(fs.list(fs.getFileNode(getRemotePath("x")))))
        .containsExactly(
            getRemotePath("x/a"),
            getRemotePath("x/b"),
            getRemotePath("x/c"));
    List<FileSystemNode> subList = fs.list(getRemotePath("x/c"));
    assertThat(transformToPath(subList)).containsExactly(
        getRemotePath("x/c/1"), getRemotePath("x/c/d"));
    putDirectory("z");
    assertThat(fs.list(getRemotePath("z"))).isEmpty();
    assertThat(fs.list(getRemotePath("invalid"))).isEmpty();
    assertThatThrownBy(() -> fs.list(getRemotePath("x/c/1")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cannot list contents of a file node");
  }

  @Test
  void mkDirectories() {
    fs.mkDirectories(getRemotePath("x"));
    assertDirectoryExists("x");
  }

  @Test
  void move() {
    putDirectory("x/source");
    putObject("x/source/y.txt", "-");
    putDirectory("x/target");
    fs.move(getRemotePath("x/source/y.txt"), getRemotePath("x/target/y.txt"));
    assertNotExists("x/source/y.txt");
    assertExists("x/target/y.txt");
  }

  @Test
  void open() {
    putObject("x/a", "John");
    assertThat(fs.open(getRemotePath("x/a"))).hasContent("John");
    assertThat(fs.open(fs.getFileNode(getRemotePath("x/a")))).hasContent("John");
  }


  @Test
  void save() {
    fs.save(stringToInputStream("-"), getRemotePath("x/y"));
    assertExists("x/y");
    assertThat(getContent("x/y")).isEqualTo("-");
    fs.save(stringToInputStream("+"), getRemotePath("a/b/c"));
    assertThat(getContent("a/b/c")).isEqualTo("+");
    // check if parent directories are created
    fs.save(stringToInputStream("-"), getRemotePath("1/2/3"));
    assertDirectoryExists("1/2");
    assertDirectoryExists("1");
    // check creation from path
    assertLocationAndName(fs.getFileNode(getRemotePath("1")), getRemotePath((String) null), "1");
    assertLocationAndName(fs.getFileNode(getRemotePath("/1")), getRemotePath((String) null), "1");
    assertLocationAndName(fs.getFileNode(getRemotePath("1/")), getRemotePath((String) null), "1");
    assertLocationAndName(fs.getFileNode(getRemotePath("1/2")), getRemotePath("1"), "2");
    assertLocationAndName(fs.getFileNode(getRemotePath("1/2/3")), getRemotePath("1/2"), "3");
  }

  @Test
  void read() {
    putObject("x/y", "-");
    FileSystemNode file = fs.getFileNode(getRemotePath("x/y"));
    assertThat(fs.read(file)).isEqualTo("-");
  }

  @Test
  void saveText() {
    fs.saveText("-", getRemotePath("x/y/z.txt"));
    assertThat(getContent("x/y/z.txt")).isEqualTo("-");
  }

  @Test
  void walk() {
    putDirectory("x");
    putObject("x/a", "-");
    putObject("x/b", "-");
    putDirectory("x/c");
    putObject("x/c/1", "-");
    putDirectory("x/c/d");
    putDirectory("y");
    putObject("y/e", "-");
    putObject("z", "-");
    SpyingTreeVisitor spy = new SpyingTreeVisitor();
    fs.list(getRemotePath()).forEach(f -> fs.walk(f, spy));
    assertThat(spy.visitedFiles).isEqualTo(
        getRemotePaths(Arrays.asList("x/a", "x/b", "x/c/1", "y/e", "z")));
    assertThat(spy.visitedDirectories).isEqualTo(
        getRemotePaths(Arrays.asList("x", "x/c", "x/c/d", "y")));
    assertThat(spy.visitationOrder).isEqualTo(
        getRemotePaths(Arrays.asList(
            "x>", "x/a", "x/b", "x/c>", "x/c/1", "x/c/d>", "x/c/d<", "x/c<", "x<", "y>",
            "y/e", "y<", "z")));
  }

  private List<String> getRemotePaths(List<String> inputPaths) {
    return inputPaths.stream().map(this::getRemotePath).collect(Collectors.toList());
  }

  private String getRemotePath(String... path) {
    return getRemotePath(AbstractFileService.combine(path));
  }

  private void assertLocationAndName(FileSystemNode node, String expectedParentPath,
      String expectedName) {
    assertThat(node.getName()).isEqualTo(expectedName);
    assertThat(node.getParentPath()).isEqualTo(expectedParentPath);
  }

  private InputStream stringToInputStream(String input) {
    return new ByteArrayInputStream(input.getBytes());
  }

  @SneakyThrows
  private String inputStreamToString(InputStream is) {
    return IOUtils.toString(is, StandardCharsets.UTF_8);
  }

  private Iterable<String> transformToPath(Iterable<FileSystemNode> nodes) {
    return StreamSupport.stream(nodes.spliterator(), false)
        .map(toPath)
        .collect(Collectors.toList());
  }

  private static class ByFiles implements Predicate<FileSystemNode> {

    @Override
    public boolean apply(FileSystemNode fileSystemNode) {
      assert fileSystemNode != null;
      return !fileSystemNode.isDirectory();
    }
  }

  private static class SpyingTreeVisitor implements FileNodeVisitor {

    private final List<String> visitationOrder = new ArrayList<>();
    private final List<String> visitedFiles = new ArrayList<>();
    private final List<String> visitedDirectories = new ArrayList<>();

    @Override
    public void on(FileSystemNode file) {
      visitedFiles.add(file.getPath());
      visitationOrder.add(file.getPath());
    }

    @Override
    public void pre(FileSystemNode directory) {
      visitedDirectories.add(directory.getPath());
      visitationOrder.add(directory.getPath() + ">");
    }

    @Override
    public void post(FileSystemNode directory) {
      visitationOrder.add(directory.getPath() + "<");
    }
  }
}