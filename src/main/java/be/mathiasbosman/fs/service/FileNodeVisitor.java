package be.mathiasbosman.fs.service;

import be.mathiasbosman.fs.domain.FileNode;

public interface FileNodeVisitor {

  void on(FileNode node);

  void pre(FileNode node);

  void post(FileNode node);
}
