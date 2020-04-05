package org.gbif.txtree;

import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;

/**
 * Simple bean for representing a node in a taxonomic tree.
 */
public class ParsedTreeNode extends TreeNode<ParsedTreeNode> {
  public final ParsedName parsedName;

  public ParsedTreeNode(long id, String name, Rank rank, ParsedName parsedName, boolean isBasionym) {
    super(id, name, rank, isBasionym);
    this.parsedName = parsedName;
  }

}
