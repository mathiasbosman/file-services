package be.mathiasbosman.fs.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

import be.mathiasbosman.fs.core.domain.FileSystemNode;
import be.mathiasbosman.fs.core.domain.FileSystemNodeImpl;
import be.mathiasbosman.fs.core.util.FileServiceUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Abstract FileService test that can be extended by other service tests.
 */
public abstract class AbstractFileServiceTest {

  @Test
  void copyNode() {
    FileSystemNodeImpl node = new FileSystemNodeImpl("mockParent", "mock", false, 1);
    // assert none-existing
    FileService fs = getFs();
    assertThatThrownBy(() -> fs.copy(node, "target"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("File mockParent/mock does not exist.");

    // copy empty directory
    putDirectory("sourceDir");
    FileSystemNode sourceDirNode = fs.getFileNode("sourceDir");
    fs.copy(sourceDirNode, "targetDir");
    assertDirectoryExists("sourceDir");
    assertDirectoryExists("targetDir");

    // copy directory with files
    putObject("sourceDir/a");
    fs.copy(sourceDirNode, "targetDirB");
    assertExists("targetDirB/a");

    // copy file to existing target
    putObject("targetDirC/a");
    FileSystemNode sourceFileNode = fs.getFileNode("sourceDir/a");
    fs.copy(sourceFileNode, "targetDirC");

    // copy via path
    putObject("sourceDir/c");
    fs.copy("sourceDir/c", "targetDir/c");
    assertExists("sourceDir/c");
    assertExists("targetDir/c");
  }

  @Test
  void getBytes() {
    putObject("path/to/object", "content");

    FileService fs = getFs();
    FileSystemNode objectNode = fs.getFileNode("path/to/object");
    assertThat(fs.getBytes(objectNode)).isEqualTo("content".getBytes());
    assertThat(fs.getBytes("path", "to", "object")).isEqualTo("content".getBytes());

    try (MockedStatic<IOUtils> mockedIOUtils = Mockito.mockStatic(IOUtils.class)) {
      mockedIOUtils.when(() -> IOUtils.toByteArray(any(InputStream.class)))
          .thenThrow(new IOException("Mocked IOException"));

      putObject("path/to/failingObject");

      assertThatThrownBy(() -> fs.getBytes("path/to/failingObject"))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Mocked IOException");
    }
  }

  @Test
  void getFileNode() {
    FileService fs = getFs();
    FileSystemNode emptyNode = fs.getFileNode("");
    assertThat(emptyNode.getParentPath()).isNull();
    assertThat(emptyNode.getName()).isEmpty();
    assertThat(emptyNode.isDirectory()).isTrue();
    assertThat(emptyNode.getSize()).isZero();

    putObject("path/to/object");

    FileSystemNode fileNode = fs.getFileNode("path", "to", "object");
    assertThat(fileNode).isNotNull();
    assertThat(fileNode.isDirectory()).isFalse();
    assertThat(fileNode.getName()).isEqualTo("object");
    assertThat(fileNode.getPath()).isEqualTo("path/to/object");
    assertThat(fileNode.getParentPath()).isEqualTo("path/to");
    assertThatThrownBy(() -> fs.getFileNode("path/invalid"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Path does not exist on filesystem: path/invalid");
  }

  @Test
  void getOptionalFileNode() {
    putObject("path/to/object");

    assertThat(getFs().getOptionalFileNode("path/to/object")).isNotNull();
    assertThat(getFs().getOptionalFileNode("path/invalid")).isNull();
  }

  @Test
  void getParent() {
    putObject("path/to/object");

    FileSystemNode fileNode = getFs().getFileNode("path/to/object");
    FileSystemNode parentNode = getFs().getParent(fileNode);
    assertThat(parentNode).isNotNull();
    assertThat(parentNode.getPath()).isEqualTo("path/to");
    assertThat(parentNode.getParentPath()).isEqualTo("path");

    FileSystemNode rootNode = getFs().getFileNode("");
    assertThat(getFs().getParent(rootNode)).isNull();
    assertThat(getFs().getParent("path/to", "object")).isNotNull();
  }

  @Test
  void mkDirectories() {
    getFs().mkDirectories("path", "to", "dir");
    assertDirectoryExists("path/to/dir");
  }

  @Test
  void list() {
    putObject("path/to/dir/objectA");
    putObject("path/to/dir/objectB");

    // invalid path
    FileService fs = getFs();
    assertThat(fs.list("path/to/invalid")).isEmpty();
    // file listing
    assertThatThrownBy(() -> fs.list("path/to/dir/objectA"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cannot list contents of a file node");
    // directory listing
    assertThat(fs.list("path/to/dir"))
        .isNotEmpty()
        .hasSize(2);
    // root listing
    assertThat(fs.list("path"))
        .isNotEmpty();
  }

  @Test
  void open() {
    putObject("path/to/object", "content");

    assertThat(getFs().open("path/to/object")).hasContent("content");
  }

  @Test
  void save() {
    getFs().save(new ByteArrayInputStream("contentA".getBytes()), "path/to/objectA");
    assertThat(getContent("path/to/objectA")).isEqualTo("contentA");

    getFs().save("contentB".getBytes(StandardCharsets.UTF_8), "path/to/objectB");
    assertThat(getContent("path/to/objectB")).isEqualTo("contentB");
  }

  @Test
  @SneakyThrows
  void saveText() {
    getFs().saveText("content", "path/to/object");
    assertThat(getContent("path/to/object")).isEqualTo("content");
  }

  @Test
  void read() {
    putObject("path/to/object", "content");
    FileService fs = getFs();
    assertThat(fs.read("path/to/object")).isEqualTo("content");

    try (MockedStatic<IOUtils> mockedIOUtils = Mockito.mockStatic(IOUtils.class)) {
      mockedIOUtils.when(() -> IOUtils.toString(any(InputStream.class), any(Charset.class)))
          .thenThrow(new IOException("Mocked IOException"));
      putObject("path/to/failingObject");
      assertThatThrownBy(() -> fs.read("path/to/failingObject"))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Mocked IOException");
    }
  }

  @Test
  void isDirectory() {
    putDirectory("path/to/object");

    assertThat(getFs().isDirectory("path", "to", "invalid")).isFalse();
    assertThat(getFs().isDirectory("path", "to", "object")).isTrue();
  }

  @Test
  void exists() {
    putObject("path/to/object");

    assertThat(getFs().exists("path")).isTrue();
    assertThat(getFs().exists("path", "to")).isTrue();
    assertThat(getFs().exists("path", "to", "object")).isTrue();
    assertThat(getFs().exists("path", "to", "invalid")).isFalse();
  }

  @Test
  void delete() {
    putObject("path/to/object");
    getFs().delete("path", "to", "object");
    assertNotExists("path/to/object");
    assertExists("path/to");
    getFs().delete(getFs().getFileNode("path/to"));
    assertNotExists("path/to");
    assertExists("path");
    putObject("path/to/object");
    putObject("path/to/sub/dir/object");
    getFs().delete(getFs().getFileNode("path/to"), true);
    assertNotExists("path/to/object");
    assertNotExists("path/to/sub");
    assertNotExists("path/to/sub/dir");
    assertNotExists("path/to/sub/dir/object");
  }

  @Test
  void deleteFolder() {
    FileService fs = getFs();
    fs.mkDirectories("x");
    fs.delete(fs.getFileNode("x"));
    assertNotExists("x");
  }

  @Test
  void move() {
    putObject("path/to/object");
    getFs().move("path/to/object", "path/toBis/object");
    assertExists("path/toBis/object");
    assertNotExists("path/to/object");
  }

  @Test
  void getSize() {
    putObject("path/to/objectA", "contentA");
    putObject("path/to/objectB", "contentB");

    FileSystemNode directoryNode = getFs().getFileNode("path/to");
    FileSystemNode fileNode = getFs().getFileNode("path/to/objectA");
    assertThat(getFs().getSize(fileNode)).isEqualTo("contentA".length());
    assertThat(getFs().getSize(directoryNode)).isEqualTo(
        "contentA".length() + "contentB".length()
    );
  }

  @Test
  void walk() {
    putDirectory("x");
    putObject("x/a");
    putObject("x/b");
    putDirectory("x/c");
    putObject("x/c/1");
    putDirectory("x/c/d");
    putDirectory("y");
    putObject("y/e");
    putObject("z");
    SpyingTreeVisitor spy = new SpyingTreeVisitor();
    getFs().list().forEach(file -> getFs().walk(file, spy));
    assertThat(spy.visitedFiles).isEqualTo(Arrays.asList("x/a", "x/b", "x/c/1", "y/e", "z"));
    assertThat(spy.visitedFolders).isEqualTo(Arrays.asList("x", "x/c", "x/c/d", "y"));
    assertThat(spy.visitationOrder).isEqualTo(Arrays
        .asList("> x", "x/a", "x/b", "> x/c", "x/c/1", "> x/c/d", "< x/c/d", "< x/c", "< x", "> y",
            "y/e", "< y", "z"));
  }

  @Test
  void countFiles() {
    putObject("path/to/objectA");
    putObject("path/to/objectB");
    putObject("path/to/objectC");
    putObject("path/to/deeper/object");

    assertThat(getFs().countFiles(getFs().getFileNode("path/to"))).isEqualTo(3);
  }

  protected abstract void assertExists(String path);

  protected abstract void assertDirectoryExists(String path);

  protected abstract void assertNotExists(String path);

  protected abstract String getContent(String path);

  protected abstract void putDirectory(String path);

  protected abstract void putObject(String path, String data);

  void putObject(String path) {
    putObject(path, "-");
  }

  protected abstract FileService getFs();

  private static class SpyingTreeVisitor implements FileNodeVisitor {

    private final List<String> visitationOrder = new ArrayList<>();
    private final List<String> visitedFiles = new ArrayList<>();
    private final List<String> visitedFolders = new ArrayList<>();

    @Override
    public void on(FileSystemNode file) {
      visitedFiles.add(file.getPath());
      visitationOrder.add(file.getPath());
    }

    @Override
    public void pre(FileSystemNode folder) {
      visitedFolders.add(folder.getPath());
      visitationOrder.add("> " + folder.getPath());
    }

    @Override
    public void post(FileSystemNode folder) {
      visitationOrder.add("< " + folder.getPath());
    }
  }

  @Test
  public void stream() {
    putObject("x/a");
    putObject("x/z");
    putObject("x/b/a");
    Stream<FileSystemNode> stream = getFs().streamDirectory(getFs().getFileNode("x"));
    assertThat(stream).isNotNull();
    List<FileSystemNode> collected = stream.collect(Collectors.toList());
    assertThat(collected).hasSize(3);
  }

  @Test
  void zip() throws Exception {
    putObject("x/a");
    putObject("x/z");
    putObject("x/b/a");
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    getFs().zip("x", outputStream, null);
    ZipInputStream zipInputStream = new ZipInputStream(
        new ByteArrayInputStream(outputStream.toByteArray()));
    Set<String> names = new HashSet<>();
    names.add(zipInputStream.getNextEntry().getName());
    names.add(zipInputStream.getNextEntry().getName());
    names.add(zipInputStream.getNextEntry().getName());
    names.add(zipInputStream.getNextEntry().getName());
    assertThat(zipInputStream.getNextEntry()).isNull();
    assertThat(names).containsExactlyInAnyOrder("a", "z", "b/a", "b/");
  }

  @Test
  void zipWithPrefixAndUnzip() {
    putObject("x/a");
    putObject("x/z");
    putObject("x/b/a");
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final FileService fs = getFs();
    fs.zip("x", outputStream, "hello");
    ZipInputStream zipInputStream = new ZipInputStream(
        new ByteArrayInputStream(outputStream.toByteArray()));
    fs.unzip(zipInputStream, "test");
    assertThat(fs.exists("test/hello/a")).isTrue();
    assertThat(fs.exists("test/hello/z")).isTrue();
    assertThat(fs.exists("test/hello/b/a")).isTrue();
  }

  @Test
  void zipUnzipHidden() {
    putObject("x/a");
    putObject("x/z");
    putObject("x/b/e");
    putObject("x/b/.g");
    putObject("x/.c");
    putObject("x/.d/f");
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final FileService fs = getFs();
    fs.zip("x", outputStream);
    ZipInputStream zipInputStream = new ZipInputStream(
        new ByteArrayInputStream(outputStream.toByteArray()));
    fs.unzip(zipInputStream, "test", FileServiceUtils.visible);
    assertThat(fs.exists("test/a")).isTrue();
    assertThat(fs.exists("test/z")).isTrue();
    assertThat(fs.exists("test/b")).isTrue();
    assertThat(fs.exists("test/b/e")).isTrue();
    assertThat(fs.exists("test/b/.g")).isFalse();
    assertThat(fs.exists("test/.c")).isFalse();
    assertThat(fs.exists("test/.d/f")).isFalse();
    assertThat(fs.exists("test/.d")).isFalse();
  }
}
