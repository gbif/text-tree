package org.gbif.txtree.parsed;

import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.ParseResult;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;
import org.gbif.txtree.TreeNode;
import org.gbif.txtree.TreeNodeData;

import java.util.Locale;
import java.util.Objects;

/**
 * A node in a taxonomic tree backed by the result of running its name through a
 * {@link org.gbif.nameparser.api.NameParser}.
 */
public class ParsedTreeNode extends TreeNode<ParsedTreeNode> {

  /**
   * The full three way parse result for {@link #name}. Never null.
   *
   * <p>For a {@link ParseResult.Parsed} result, the {@link ParsedName} it wraps is the same object
   * as {@link #parsedName}: the tree's rank and code are written back onto it, so
   * {@code result.code()} reflects the tree's code, not necessarily what the parser inferred. For
   * an {@link ParseResult.Informal} result {@code result} and {@link #parsedName} are unrelated
   * objects, so {@code result.code()} stays the parser's own answer while {@link #parsedName}'s
   * code is still the tree's.
   */
  public final ParseResult result;

  /**
   * The structured name. Null only for a {@link ParseResult.Unparsable} result — an
   * {@link ParseResult.Informal} result contributes its rebuilt taxon anchor, matching the
   * type=INFORMAL ParsedName that the 4.x Java parser returned instead of throwing.
   */
  public final ParsedName parsedName;

  /**
   * The nomenclatural code in scope for this name: its own {@code CODE} info if it declares a
   * recognised one, otherwise the one inherited from its parent. Null when the tree declares none.
   */
  public final NomCode code;

  /**
   * Creates a node from one parsed line and the outcome of running its name through a parser.
   *
   * @param data   the components of the line this node was read from
   * @param rank   the resolved rank, or null if the line carried none
   * @param code   the nomenclatural code in scope, or null if none was declared
   * @param result the parse result for {@link #name}, never null
   */
  public ParsedTreeNode(TreeNodeData data, Rank rank, NomCode code, ParseResult result) {
    super(data.id(), data.name(), rankName(rank), data.extinct(), data.basionym(),
        data.homotypic(), data.provisional(), data.infos(), data.comment());
    this.code = code;
    this.result = Objects.requireNonNull(result, "result required");
    this.parsedName = parsedNameOf(result, rank, code);
  }

  private static String rankName(Rank rank) {
    return rank == null ? null : rank.name().toLowerCase(Locale.ROOT);
  }

  private static ParsedName parsedNameOf(ParseResult result, Rank rank, NomCode code) {
    ParsedName pn = result instanceof ParseResult.Informal informal
        ? informal.toParsedName()
        : result.parsed().orElse(null);
    if (pn != null) {
      // keep the rank and code the tree declared, not whatever the parser inferred
      if (rank != null) {
        pn.setRank(rank);
      }
      if (code != null) {
        pn.setCode(code);
      }
    }
    return pn;
  }
}
