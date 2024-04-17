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
  public final String rank;
  public final boolean extinct;
  public final boolean basionym;
  public final boolean homotypic;
  public final LinkedList<T> synonyms = new LinkedList<>();
  public final LinkedList<T> children = new LinkedList<>();
  public final Map<String, String[]> infos;
  public final String comment;

  public TreeNode(long id, String name, String rank, boolean extinct, boolean isBasionym, boolean homotypic) {
    this(id, name, rank, extinct, isBasionym, homotypic, null, null);
  }

  public TreeNode(long id, String name, String rank, boolean extinct, boolean isBasionym, boolean homotypic, Map<String, String[]> infos, String comment) {
    this.id = id;
    this.name = name;
    this.rank = rank;
    this.extinct = extinct;
    this.basionym = isBasionym;
    this.homotypic = homotypic;
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
