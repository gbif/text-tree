package org.gbif.txtree;

import org.gbif.nameparser.api.Rank;

import java.util.Map;

/**
 * Simple bean for representing a node in a taxonomic tree.
 */
public class SimpleTreeNode extends TreeNode<SimpleTreeNode> {

  public SimpleTreeNode(long id, String name, Rank rank, boolean isBasionym) {
    super(id, name, rank, isBasionym);
  }

  public SimpleTreeNode(long id, String name, Rank rank, boolean isBasionym, Map<String, String[]> infos) {
    super(id, name, rank, isBasionym, infos);
  }
}
