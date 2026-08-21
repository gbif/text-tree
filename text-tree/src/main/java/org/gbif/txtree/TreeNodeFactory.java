package org.gbif.txtree;

/**
 * Builds a concrete node from the components of one parsed tree line.
 * Implementations may reject a line by throwing {@link IllegalArgumentException}.
 */
@FunctionalInterface
public interface TreeNodeFactory<T extends TreeNode<T>> {

  /**
   * @param data   the components of the line being read
   * @param parent the node this one hangs under, or null when it sits at the root level. Lets a
   *               node inherit from its ancestors without the factory holding state of its own.
   *               For a synonym this is the node it is nested under, usually the accepted name.
   * @return the new node, never null
   */
  T create(TreeNodeData data, T parent);
}
