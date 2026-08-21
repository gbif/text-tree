package org.gbif.txtree;

import java.util.Map;

/**
 * Represents a node in a taxonomic tree using mostly strings.
 */
public class SimpleTreeNode extends TreeNode<SimpleTreeNode> {

  /**
   * Creates a node with no flags, infos or comment.
   *
   * @param id   identifier for the node, the 1-based line number when read by {@link Tree}
   * @param name the scientific name including any authorship, as given
   * @param rank the rank as given in square brackets, or null if the line carried none
   */
  public SimpleTreeNode(long id, String name, String rank) {
    super(id, name, rank, false, false, false, false);
  }

  /**
   * Creates a node with flags but no infos or comment.
   *
   * @param id          identifier for the node, the 1-based line number when read by {@link Tree}
   * @param name        the scientific name including any authorship, as given
   * @param rank        the rank as given in square brackets, or null if the line carried none
   * @param extinct     the name was prefixed with the dagger symbol
   * @param isBasionym  the name was prefixed with the dollar symbol
   * @param homotypic   the name was prefixed with the homotypic symbol
   * @param provisional the name was prefixed with the question mark symbol
   */
  public SimpleTreeNode(long id, String name, String rank, boolean extinct, boolean isBasionym, boolean homotypic, boolean provisional) {
    super(id, name, rank, extinct, isBasionym, homotypic, provisional);
  }

  /**
   * Creates a fully populated node.
   *
   * @param id          identifier for the node, the 1-based line number when read by {@link Tree}
   * @param name        the scientific name including any authorship, as given
   * @param rank        the rank as given in square brackets, or null if the line carried none
   * @param extinct     the name was prefixed with the dagger symbol
   * @param isBasionym  the name was prefixed with the dollar symbol
   * @param homotypic   the name was prefixed with the homotypic symbol
   * @param provisional the name was prefixed with the question mark symbol
   * @param infos       the key value pairs from the curly brackets, may be null
   * @param comment     the trailing comment after the hash symbol, or null
   */
  public SimpleTreeNode(long id, String name, String rank, boolean extinct, boolean isBasionym, boolean homotypic, boolean provisional, Map<String, String[]> infos, String comment) {
    super(id, name, rank, extinct, isBasionym, homotypic, provisional, infos, comment);
  }
}
