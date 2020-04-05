package org.gbif.txtree;

/**
 * Simple class to represent a verbatim line/row in a tree file.
 */
public class TreeLine {
  public final long line;
  public final int level;
  public final String content;

  public TreeLine(long line, int level, String content) {
    this.line = line;
    this.level = level;
    this.content = content;
  }
}
