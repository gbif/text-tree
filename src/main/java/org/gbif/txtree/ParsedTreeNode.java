package org.gbif.txtree;

import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;

import java.util.Map;

/**
 * Represents a node in a taxonomic tree backed by a ParsedName instance from the GBIF name parser.
 */
public class ParsedTreeNode extends TreeNode<ParsedTreeNode> {
  public final ParsedName parsedName;

  private static String rankOf(ParsedName pn) {
    return pn == null || pn.getRank() == null || pn.getRank() == Rank.UNRANKED ? null : pn.getRank().name().toLowerCase();
  }

  public ParsedTreeNode(long id, String name, ParsedName pn) {
    super(id, name, rankOf(pn), false, false, false);
    this.parsedName = pn;
  }

  public ParsedTreeNode(long id, String name, ParsedName pn, boolean extinct, boolean isBasionym, boolean homotypic) {
    super(id, name, rankOf(pn), extinct, isBasionym, homotypic);
    this.parsedName = pn;
  }

  public ParsedTreeNode(long id, String name, ParsedName pn, boolean extinct, boolean isBasionym, boolean homotypic, Map<String, String[]> infos, String comment) {
    super(id, name, rankOf(pn), extinct, isBasionym, homotypic, infos, comment);
    this.parsedName = pn;
  }
}
