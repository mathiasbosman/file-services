package be.mathiasbosman.fs.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

import be.mathiasbosman.fs.core.domain.FileSystemNode;
import be.mathiasbosman.fs.core.domain.FileSystemNodeImpl;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Abstract FileService test that can be extended by other service tests. The abstract methods
 * should be overridden for each file system.
 *
 * @since 0.0.1
 */
public abstract class AbstractFileServiceTest {

  @Test
  void copyNode() {
    FileSystemNodeImpl node = new FileSystemNodeImpl("mockParent", "mock", false, 1);
    // assert none-existing
    assertThatThrownBy(() -> getFs().copy(node, "target"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("File mockParent/mock does not exist.");
    // copy empty directory
    putDirectory("sourceDir");
    FileSystemNode sourceDirNode = getFs().getFileNode("sourceDir");
    getFs().copy(sourceDirNode, "targetDir");
    assertDirectoryExists("sourceDir");
    assertDirectoryExists("targetDir");
    // copy directory with files
    putObject("sourceDir/a");
    getFs().copy(sourceDirNode, "targetDirB");
    assertExists("targetDirB/a");
    // copy file to existing target
    putObject("targetDirC/a");
    FileSystemNode sourceFileNode = getFs().getFileNode("sourceDir/a");
    getFs().copy(sourceFileNode, "targetDirC");
    // copy via path
    putObject("sourceDir/c");
    getFs().copy("sourceDir/c", "targetDir/c");
    assertExists("sourceDir/c");
    assertExists("targetDir/c");
  }

  @Test
  void getBytes() {
    putObject("path/to/object", "content");
    FileSystemNode objectNode = getFs().getFileNode("path/to/object");
    assertThat(getFs().getBytes(objectNode)).isEqualTo("content".getBytes());
    assertThat(getFs().getBytes("path", "to", "object")).isEqualTo("content".getBytes());

    try (MockedStatic<IOUtils> mockedIOUtils = Mockito.mockStatic(IOUtils.class)) {
      mockedIOUtils.when(() -> IOUtils.toByteArray(any(InputStream.class)))
          .thenThrow(new IOException("Mocked IOException"));
      putObject("path/to/failingObject");
      assertThatThrownBy(() -> getFs().getBytes("path/to/failingObject"))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Mocked IOException");
    }
  }

  @Test
  void getFileNode() {
    FileSystemNode emptyNode = getFs().getFileNode("");
    assertThat(emptyNode.getParentPath()).isNull();
    assertThat(emptyNode.getName()).isEmpty();
    assertThat(emptyNode.isDirectory()).isTrue();
    assertThat(emptyNode.getSize()).isEqualTo(0);
    putObject("path/to/object");
    FileSystemNode fileNode = getFs().getFileNode("path", "to", "object");
    assertThat(fileNode).isNotNull();
    assertThat(fileNode.isDirectory()).isFalse();
    assertThat(fileNode.getName()).isEqualTo("object");
    assertThat(fileNode.getPath()).isEqualTo("path/to/object");
    assertThat(fileNode.getParentPath()).isEqualTo("path/to");
    assertThatThrownBy(() -> getFs().getFileNode("path/invalid"))
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
    assertThat(getFs().list("path/to/invalid")).isEmpty();
    // file listing
    assertThatThrownBy(() -> getFs().list("path/to/dir/objectA"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cannot list contents of a file node");
    // directory listing
    assertThat(getFs().list("path/to/dir"))
        .isNotEmpty()
        .hasSize(2);
    // root listing
    assertThat(getFs().list("path"))
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
    assertThat(getFs().read("path/to/object")).isEqualTo("content");

    try (MockedStatic<IOUtils> mockedIOUtils = Mockito.mockStatic(IOUtils.class)) {
      mockedIOUtils.when(() -> IOUtils.toString(any(InputStream.class), any(Charset.class)))
          .thenThrow(new IOException("Mocked IOException"));
      putObject("path/to/failingObject");
      assertThatThrownBy(() -> getFs().read("path/to/failingObject"))
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
    for (FileSystemNode file : getFs().list()) {
      getFs().walk(file, spy);
    }
    assertThat(Arrays.asList("x/a", "x/b", "x/c/1", "y/e", "z")).isEqualTo(spy.visitedFiles);
    assertThat(Arrays.asList("x", "x/c", "x/c/d", "y")).isEqualTo(spy.visitedFolders);
    assertThat(Arrays
        .asList("> x", "x/a", "x/b", "> x/c", "x/c/1", "> x/c/d", "< x/c/d", "< x/c", "< x", "> y",
            "y/e", "< y", "z")).isEqualTo(spy.visitationOrder);
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
}
