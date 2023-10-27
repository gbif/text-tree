package org.gbif.txtree;

import org.gbif.nameparser.api.Rank;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Simple bean for representing a node in a taxonomic tree.
 */
abstract class TreeNode<T extends TreeNode<T>> {
  public final long id;
  public final String name;
  public final Rank rank;
  public final boolean basionym;
  public final List<T> synonyms = new ArrayList<>();
  public final LinkedList<T> children = new LinkedList<>();
  public final Map<String, String[]> infos;
  public final String comment;

  public TreeNode(long id, String name, Rank rank, boolean isBasionym) {
    this(id, name, rank, isBasionym, null, null);
  }

  public TreeNode(long id, String name, Rank rank, boolean isBasionym, Map<String, String[]> infos, String comment) {
    this.id = id;
    this.name = name;
    this.rank = rank;
    this.basionym = isBasionym;
    this.infos = infos;
    this.comment = comment;
  }

  @Override
  public String toString() {
    return rank == null ?
        name :
        name + " [" + rank .name().toLowerCase() + ']';
  }

  // to avoid dependency on apache or guava
  private static String indent(int level) {
    StringBuilder buf = new StringBuilder(level);
    for(int i = 0; i < level; ++i) {
      buf.append(" ");
    }
    return buf.toString();
  }

  public void print(Appendable out, int level, boolean synonym) throws IOException {
    out.append(indent(level * 2));
    if (synonym) {
      out.append(Tree.SYNONYM_SYMBOL);
    }
    if (basionym) {
      out.append(Tree.BASIONYM_SYMBOL);
    }
    out.append(name);
    if (rank != null && rank != Rank.UNRANKED) {
      out.append(" [");
      out.append(rank.name().toLowerCase());
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
