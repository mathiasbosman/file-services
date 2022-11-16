package be.mathiasbosman.fs.core.domain;

import be.mathiasbosman.fs.core.config.Generated;
import java.util.Collection;
import java.util.function.Supplier;

@Generated //ignores test coverage
public interface FileSystemTree<T> {

  Collection<FileSystemTree<T>> getChildren();

  default void addChild(String name, T child) {
    add(name, () -> child);
  }

  FileSystemTree<T> add(String name, Supplier<T> childSupplier);

  T getNode();
}
