package be.mathiasbosman.fs.core.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FileSystemTreeImpl<T> implements FileSystemTree<T> {

  @Getter
  private final T node;
  private Map<String, FileSystemTree<T>> children;

  @Override
  public Collection<FileSystemTree<T>> getChildren() {
    return children != null ? children.values() : Collections.emptyList();
  }

  @Override
  public FileSystemTree<T> add(String name, Supplier<T> childSupplier) {
    if (children == null) {
      children = new HashMap<>();
    }

    return children.computeIfAbsent(name, k -> new FileSystemTreeImpl<>(childSupplier.get()));
  }
}
