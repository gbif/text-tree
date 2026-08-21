package org.gbif.txtree;

import java.util.Map;

/**
 * The components of a single tree line as extracted by the line parser, handed to a
 * {@link TreeNodeFactory} to build a concrete {@link TreeNode}.
 *
 * @param id          the 1-based number of the line in the input
 * @param name        the scientific name including any authorship, never null
 * @param rank        the rank as given in square brackets, or null if the line had none
 * @param extinct     the line was prefixed with the dagger symbol
 * @param basionym    the line was prefixed with the dollar symbol
 * @param homotypic   the line was prefixed with the homotypic symbol
 * @param provisional the line was prefixed with the question mark symbol
 * @param infos       the key value pairs from the curly brackets, empty if the line had none
 * @param comment     the trailing comment, or null if the line had none
 */
public record TreeNodeData(
    long id,
    String name,
    String rank,
    boolean extinct,
    boolean basionym,
    boolean homotypic,
    boolean provisional,
    Map<String, String[]> infos,
    String comment
) {}
