package org.gbif.txtree;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

/**
 * Simple bean for representing a node in a taxonomic tree.
 *
 * @param <T> the concrete node type, so that {@link #synonyms} and {@link #children} are typed
 */
public abstract class TreeNode<T extends TreeNode<T>> {
  /** Identifier for this node. Trees read by {@link Tree} use the 1-based number of the line it came from. */
  public final long id;
  /** The scientific name including any authorship, exactly as it appeared in the tree. */
  public final String name;
  /** The rank as given in square brackets, or null if the line carried none. */
  public final String rank;
  /** The name was prefixed with the dagger symbol, marking an extinct taxon. */
  public final boolean extinct;
  /** The name was prefixed with the dollar symbol, marking it as the basionym. */
  public final boolean basionym;
  /** The name was prefixed with the homotypic symbol, marking it homotypic to the name above. */
  public final boolean homotypic;
  /** The name was prefixed with the question mark symbol, marking it provisional. */
  public final boolean provisional;
  /** The synonyms nested directly under this name, in the order they were read. */
  public final LinkedList<T> synonyms = new LinkedList<>();
  /** The accepted names nested directly under this one, in the order they were read. */
  public final LinkedList<T> children = new LinkedList<>();
  /** The key value pairs from the curly brackets. Empty when the line carried none, null when the node was built without them. */
  public final Map<String, String[]> infos;
  /** The trailing comment after the hash symbol, or null if the line carried none. */
  public final String comment;

  /**
   * Creates a node with no infos and no comment.
   *
   * @param id          identifier for the node, the 1-based line number when read by {@link Tree}
   * @param name        the scientific name including any authorship, as given
   * @param rank        the rank as given in square brackets, or null if the line carried none
   * @param extinct     the name was prefixed with the dagger symbol
   * @param isBasionym  the name was prefixed with the dollar symbol
   * @param homotypic   the name was prefixed with the homotypic symbol
   * @param provisional the name was prefixed with the question mark symbol
   */
  public TreeNode(long id, String name, String rank, boolean extinct, boolean isBasionym, boolean homotypic, boolean provisional) {
    this(id, name, rank, extinct, isBasionym, homotypic, provisional, null, null);
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
  public TreeNode(long id, String name, String rank, boolean extinct, boolean isBasionym, boolean homotypic, boolean provisional, Map<String, String[]> infos, String comment) {
    this.id = id;
    this.name = name;
    this.rank = rank;
    this.extinct = extinct;
    this.basionym = isBasionym;
    this.homotypic = homotypic;
    this.provisional = provisional;
    this.infos = infos;
    this.comment = comment;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (extinct) {
      sb.append(Tree.EXTINCT_SYMBOL);
    }
    sb.append(name);
    if (rank != null) {
      sb.append(" [");
      sb.append(rank);
      sb.append(']');
    }
    return sb.toString();
  }

  // to avoid dependency on apache or guava
  private static String indent(int level) {
    StringBuilder buf = new StringBuilder(level);
    for(int i = 0; i < level; ++i) {
      buf.append(" ");
    }
    return buf.toString();
  }

  /**
   * Prints this node and everything below it, one name per line.
   *
   * @param out     the output to append to
   * @param level   the indentation depth to print this node at, 0 for a root
   * @param synonym print this node with a synonym prefix
   * @throws IOException if the output could not be written to
   */
  public void print(Appendable out, int level, boolean synonym) throws IOException {
    out.append(indent(level * 2));
    if (synonym) {
      if (homotypic) {
        out.append(Tree.HOMOTYPIC_SYMBOL);
      } else {
        out.append(Tree.SYNONYM_SYMBOL);
      }
    }
    if (basionym) {
      out.append(Tree.BASIONYM_SYMBOL);
    }
    if (extinct) {
      out.append(Tree.EXTINCT_SYMBOL);
    }
    if (provisional) {
      out.append(Tree.PROVISIONAL_SYMBOL);
    }
    out.append(name);
    if (rank != null) {
      out.append(" [");
      out.append(rank);
      out.append("]");
    }
    if (infos != null && !infos.isEmpty()) {
      out.append(" {");
      boolean first = true;
      for (var x : infos.entrySet()) {
        if (!first) {
          out.append(" ");
        }
        first = false;
        out.append(x.getKey().toUpperCase());
        out.append("=");
        boolean multi = false;
        for (String val : x.getValue()) {
          if (multi) {
            out.append(",");
          }
          out.append(val);
          multi = true;
        }
      }
      out.append("}");
    }
    out.append("\n");
    // recursive
    for (T n : synonyms) {
      n.print(out, level + 1, true);
    }
    for (T n : children) {
      n.print(out, level + 1, false);
    }
  }
}
