package org.gbif.txtree;

import java.util.Map;

/**
 * Simple class to represent a verbatim line/row and its dynamic info properties in a tree file.
 */
public class TreeLine {
  public final long line;
  public final int level;
  public final String content;
  public final Map<String, String[]> infos;

  public TreeLine(long line, int level, String content, Map<String, String[]> infos) {
    this.line = line;
    this.level = level;
    this.content = content;
    this.infos = infos;
  }
}
