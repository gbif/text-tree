package org.gbif.txtree;

import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;

import java.util.Map;

/**
 * Simple bean for representing a node in a taxonomic tree.
 */
public class ParsedTreeNode extends TreeNode<ParsedTreeNode> {
  public final ParsedName parsedName;

  public ParsedTreeNode(long id, String name, Rank rank, ParsedName parsedName) {
    super(id, name, rank, false, false, false);
    this.parsedName = parsedName;
  }

  public ParsedTreeNode(long id, String name, Rank rank, ParsedName parsedName, boolean extinct, boolean isBasionym, boolean homotypic) {
    super(id, name, rank, extinct, isBasionym, homotypic);
    this.parsedName = parsedName;
  }

  public ParsedTreeNode(long id, String name, Rank rank, ParsedName parsedName, boolean extinct, boolean isBasionym, boolean homotypic, Map<String, String[]> infos, String comment) {
    super(id, name, rank, extinct, isBasionym, homotypic, infos, comment);
    this.parsedName = parsedName;
  }
}
