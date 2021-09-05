package be.mathiasbosman.fs.core.service;

import be.mathiasbosman.fs.core.domain.FileSystemNode;

/**
 * Simple visitor for {@link FileSystemNode}s.
 *
 * @author mathiasbosman
 * @since 0.0.1
 */
public interface FileNodeVisitor {

  void on(FileSystemNode node);

  void pre(FileSystemNode node);

  void post(FileSystemNode node);
}
