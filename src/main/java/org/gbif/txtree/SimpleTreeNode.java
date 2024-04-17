package org.gbif.txtree;

import java.util.Map;

/**
 * Represents a node in a taxonomic tree using mostly strings.
 */
public class SimpleTreeNode extends TreeNode<SimpleTreeNode> {

  public SimpleTreeNode(long id, String name, String rank) {
    super(id, name, rank, false, false, false);
  }

  public SimpleTreeNode(long id, String name, String rank, boolean extinct, boolean isBasionym, boolean homotypic) {
    super(id, name, rank, extinct, isBasionym, homotypic);
  }

  public SimpleTreeNode(long id, String name, String rank, boolean extinct, boolean isBasionym, boolean homotypic, Map<String, String[]> infos, String comment) {
    super(id, name, rank, extinct, isBasionym, homotypic, infos, comment);
  }
}
