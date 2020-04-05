package org.gbif.txtree;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.gbif.nameparser.api.Rank;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Simple bean for representing a node in a taxonomic tree.
 */
public class SimpleTreeNode extends TreeNode<SimpleTreeNode> {

  public SimpleTreeNode(long id, String name, Rank rank, boolean isBasionym) {
    super(id, name, rank, isBasionym);
  }

}
