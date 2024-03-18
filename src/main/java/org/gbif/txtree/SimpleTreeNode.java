package org.gbif.txtree;

import org.gbif.nameparser.api.Rank;

import java.util.Map;

/**
 * Simple bean for representing a node in a taxonomic tree.
 */
public class SimpleTreeNode extends TreeNode<SimpleTreeNode> {

  public SimpleTreeNode(long id, String name, Rank rank) {
    super(id, name, rank, false, false, false);
  }

  public SimpleTreeNode(long id, String name, Rank rank, boolean extinct, boolean isBasionym, boolean homotypic) {
    super(id, name, rank, extinct, isBasionym, homotypic);
  }

  public SimpleTreeNode(long id, String name, Rank rank, boolean extinct, boolean isBasionym, boolean homotypic, Map<String, String[]> infos, String comment) {
    super(id, name, rank, extinct, isBasionym, homotypic, infos, comment);
  }
}
