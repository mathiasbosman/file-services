package be.mathiasbosman.fs.core.domain;

import java.util.Collection;
import java.util.function.Supplier;

public interface FileSystemTree<T> {

  Collection<FileSystemTree<T>> getChildren();

  default void addChild(String name, T child) {
    add(name, () -> child);
  }

  FileSystemTree<T> add(String name, Supplier<T> childSupplier);

  T getNode();
}
