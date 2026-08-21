package org.gbif.txtree;

import java.util.Map;

/**
 * Simple class to represent a verbatim line/row and its dynamic info properties in a tree file.
 */
public class TreeLine {
  /** The 1-based number of this line in the input. */
  public final long line;
  /** The indentation depth, counted in levels of 2 spaces. A root level line is 0. */
  public final int level;
  /** The verbatim line with surrounding whitespace trimmed. */
  public final String content;
  /** The key value pairs from the curly brackets, empty when the line carried none. */
  public final Map<String, String[]> infos;

  /**
   * Creates a verbatim line record.
   *
   * @param line    the 1-based number of this line in the input
   * @param level   the indentation depth in levels of 2 spaces, 0 at the root
   * @param content the verbatim line, trimmed
   * @param infos   the key value pairs from the curly brackets
   */
  public TreeLine(long line, int level, String content, Map<String, String[]> infos) {
    this.line = line;
    this.level = level;
    this.content = content;
    this.infos = infos;
  }
}
