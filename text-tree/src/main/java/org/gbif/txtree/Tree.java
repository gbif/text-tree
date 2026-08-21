package org.gbif.txtree;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Simple class to keep a taxonomy of names.
 * We use this to compare nub build outputs with a very simple text based tree format that is very easy to read.
 * Especially useful for larger tree snippets.
 *
 * <p>Iterating over the tree goes depth first: each name, then its synonyms, then its children.
 *
 * @param <T> the node type this tree holds, see {@link #simple(InputStream)} for the string based
 *            one and {@link #parse(Reader, Consumer, TreeNodeFactory)} to supply your own
 */
public class Tree<T extends TreeNode<T>> implements Iterable<T> {
  /** The synonym prefix used by older files, equivalent to {@link #SYNONYM_SYMBOL}. */
  public static final String SYNONYM_OLD_SYMBOL = "*";
  /** Prefix marking a name as a synonym of the name it is nested under. */
  public static final String SYNONYM_SYMBOL = "=";
  /** Prefix marking a synonym as homotypic with the name it is nested under. */
  public static final String HOMOTYPIC_SYMBOL = "≡";
  /** Prefix marking a name as the basionym. */
  public static final String BASIONYM_SYMBOL = "$";
  /** Prefix marking a taxon as extinct. */
  public static final String EXTINCT_SYMBOL = "†";
  /** Prefix marking a name as provisional. */
  public static final String PROVISIONAL_SYMBOL = "?";

  private static final String ANY_CHAR = "[^\t\n\r]";
  private static final Pattern LINE_PARSER = Pattern.compile("^" +
      "((?:  )*)" + // indent #1
      "([=≡$†?*]+ *)?" + // name prefix symbols #2
      "([^\t\n\r\\s]"+ANY_CHAR+"+?)" +   // name & author #3
      "(?: \\[([a-z _-]+)])?" +  // rank #4
      "(?: +\\{("+ANY_CHAR+"*)})?" +  // infos #5
      "(?:\\s+#\\s*(.*))?" +  // comments #6
      "\\s*$");
  private static final Pattern INFO_PARSER = Pattern.compile("([A-Z]+)=([^=]+)(?: |$)");
  private static final Pattern COMMA_SPLITTER = Pattern.compile("\\s*(?<!,),(?!,)\\s*");
  private long count;
  private final List<T> root = new ArrayList<>();



  /**
   * Creates an empty tree. Use {@link #simple(InputStream)} or
   * {@link #parse(Reader, Consumer, TreeNodeFactory)} to read one from an input.
   */
  public Tree() {
  }

  /**
   * The number of names read into this tree, counting synonyms and children alike.
   *
   * @return the total number of nodes in the tree
   */
  public long size() {
    return count;
  }

  // to avoid dependency on apache or guava
  static boolean isBlank(String x) {
    if (x == null) {
      return true;
    }
    for (int i = 0; i < x.length(); i++) {
      if (!Character.isWhitespace(x.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  // to avoid dependency on apache or guava
  static String trimToNull(String x) {
    if (x == null) {
      return null;
    }
    String trimmed = x.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  /**
   * Builds a new simple tree instance by parsing the given UTF8 input stream.
   *
   * @param stream the input stream to parse
   * @return the new tree instance
   * @throws IOException if the input could not be read
   * @throws IllegalArgumentException if the input contained was badly formatted
   */
  public static Tree<SimpleTreeNode> simple(InputStream stream) throws IOException {
    return simple(stream, null);
  }

  /**
   * Builds a new simple tree instance by parsing the given UTF8 input stream.
   * In addition to {@link #simple(InputStream)} it takes an optional listener
   * that is passed the verbatim tree line instance for each processed row.
   *
   * @param stream   the UTF8 input stream to parse
   * @param listener optional listener passed the verbatim line for each processed row, may be null
   * @return the new tree instance
   * @throws IOException if the input could not be read
   * @throws IllegalArgumentException if the input was badly formatted
   */
  public static Tree<SimpleTreeNode> simple(InputStream stream, Consumer<TreeLine> listener) throws IOException {
    return parse(new InputStreamReader(stream, StandardCharsets.UTF_8), listener, Tree::simpleNode);
  }

  /**
   * Builds a new simple tree instance by parsing the given reader.
   *
   * @param reader the tree input
   * @return the new tree instance
   * @throws IOException if the input could not be read
   * @throws IllegalArgumentException if the input was badly formatted
   */
  public static Tree<SimpleTreeNode> simple(Reader reader) throws IOException {
    return parse(reader, null, Tree::simpleNode);
  }

  /**
   * Builds a new simple tree instance by parsing the given reader.
   * In addition to {@link #simple(Reader)} it takes an optional listener
   * that is passed the verbatim tree line instance for each processed row.
   *
   * @param reader   the tree input
   * @param listener optional listener passed the verbatim line for each processed row, may be null
   * @return the new tree instance
   * @throws IOException if the input could not be read
   * @throws IllegalArgumentException if the input was badly formatted
   */
  public static Tree<SimpleTreeNode> simple(Reader reader, Consumer<TreeLine> listener) throws IOException {
    return parse(reader, listener, Tree::simpleNode);
  }

  /**
   * Builds a tree of any node type by parsing the given reader, delegating node construction to the
   * given factory. This is the extension point {@code text-tree-parsed} builds on.
   *
   * @param reader   the tree input
   * @param listener optional listener passed the verbatim line for each processed row, may be null
   * @param factory  builds a node from the components of each line and its parent
   * @param <T>      the node type the factory builds
   * @return the new tree instance
   * @throws IOException if the input could not be read
   * @throws IllegalArgumentException if the input was badly formatted, or the factory rejected a line
   */
  public static <T extends TreeNode<T>> Tree<T> parse(Reader reader,
                                                      Consumer<TreeLine> listener,
                                                      TreeNodeFactory<T> factory
                                                      ) throws IOException {
    Objects.requireNonNull(factory, "factory required");
    Tree<T> tree = new Tree<>();
    LinkedList<T> parents = new LinkedList<>();

    BufferedReader br = new BufferedReader(reader);
    long row = 1;
    String line = br.readLine();
    while (line != null) {
      int level = 0;
      if (!isBlank(line)) {
        tree.count++;
        Matcher m = LINE_PARSER.matcher(line);
        if (m.find()) {
          level = m.group(1).length();
          if (level % 2 != 0) {
            throw new IllegalArgumentException("Tree is not indented properly on line " + row + ". Use 2 spaces only: " + line);
          }
          level = level / 2;

          // resolve the parent before building the node, so the factory can see it
          T parent = null;
          if (level == 0) {
            parents.clear();
          } else {
            while (parents.size() > level) {
              // remove latest parents until we are at the right level
              parents.removeLast();
            }
            if (parents.size() < level) {
              throw new IllegalArgumentException("Tree is not properly indented on line " + row + ". Use 2 spaces for children: " + line);
            }
            parent = parents.peekLast();
          }

          T n = factory.create(data(row, m), parent);
          Objects.requireNonNull(n, "factory returned no node for line " + row);
          if (parent == null) {
            tree.getRoot().add(n);
          } else if (isSynonym(m.group(2))) {
            parent.synonyms.add(n);
          } else {
            parent.children.add(n);
          }
          parents.add(n);

          if (listener != null) {
            TreeLine tl = new TreeLine(row, level, line.trim(), n.infos);
            listener.accept(tl);
          }
        } else {
          throw new IllegalArgumentException("Failed to parse Tree on line " + row + ": " + line);
        }
      }
      line = br.readLine();
      row++;
    }
    return tree;
  }

  private static boolean isSynonym(String flags) {
    return flags != null && (
           flags.contains(SYNONYM_SYMBOL) ||
           flags.contains(SYNONYM_OLD_SYMBOL) ||
           flags.contains(HOMOTYPIC_SYMBOL)
    );
  }

  /** The outcome of {@link #verify(InputStream)}. */
  public static class VerificationResult {
    /** True if the input is a well formed text tree. */
    public final boolean valid;
    /** The number of lines read, which for an invalid tree is the line the problem was found on. */
    public final int lines;
    /** A human readable description of the problem, or a confirmation that the tree is valid. */
    public final String message;

    /**
     * Creates a verification outcome.
     *
     * @param valid   true if the input is a well formed text tree
     * @param lines   the number of lines read
     * @param message a human readable description of the outcome
     */
    public VerificationResult(boolean valid, int lines, String message) {
        this.valid = valid;
        this.lines = lines;
        this.message = message;
    }
    private static VerificationResult failed(String message, int lines) {
      return new VerificationResult(false, lines, message);
    }
    private static VerificationResult valid(int lines) {
      return new VerificationResult(true, lines, "The tree is valid");
    }
  }

  /**
   * Verifies that the given input stream contains a valid text tree.
   * Especially useful for verifying that the tree is properly indented.
   * @param stream tree input
   * @return the outcome, never null. Check {@link VerificationResult#valid}.
   * @throws IOException if the input could not be read
   */
  public static VerificationResult verify(InputStream stream) throws IOException {
    return verify(new InputStreamReader(stream, StandardCharsets.UTF_8));
  }

  /**
   * Verifies that the given input stream contains a valid text tree.
   * Especially useful for verifying that the tree is properly indented.
   * @param reader tree input
   * @return the outcome, never null. Check {@link VerificationResult#valid}.
   * @throws IOException if the input could not be read
   */
  public static VerificationResult verify(Reader reader) throws IOException {
    var br = new BufferedReader(reader);
    String line = br.readLine();
    int lineNumber = 1;
    try {
      int max = 0;
      int last = 0;
      boolean rooted = false;
      while (line != null) {
        if (!isBlank(line)) {
          Matcher m = LINE_PARSER.matcher(line);
          if (m.find()) {
            int level = m.group(1).length();
            max = Math.max(max, level);
            if (level % 2 != 0) {
              return VerificationResult.failed(String.format("Tree is not indented properly on line %s. Use 2 spaces only: %s", lineNumber, line), lineNumber);
            }
            if (!rooted && level > 0) {
              // the first name in the file has nothing to hang off, so it must sit at the root level.
              // Without this, a whole file shifted right by one level verified but would not parse.
              return VerificationResult.failed(String.format("Tree does not start at the root level. Line %s is indented: %s", lineNumber, line), lineNumber);
            }
            if (level-last>2) {
              return VerificationResult.failed(String.format("Tree is indented too much on line %s. Use 2 spaces only: %s", lineNumber, line), lineNumber);
            }
            rooted = true;
            last = level;
          } else {
            return VerificationResult.failed(String.format("Failed to parse Tree on line %s: %s", lineNumber, line), lineNumber);
          }
        }
        line = br.readLine();
        lineNumber++;
      }
      if (max==0 && lineNumber > 8) {
        return VerificationResult.failed("Tree is not indented at all", lineNumber);
      }

    } catch (IllegalArgumentException e) {
      return VerificationResult.failed(String.format("Failed to parse Tree on line %s: %s", lineNumber, line), lineNumber);
    }
    // should we require some other level than just 0???
    return VerificationResult.valid(lineNumber-1);
  }

  private static TreeNodeData data(long row, Matcher m) {
    String flags = m.group(2) == null ? "" : m.group(2);
    return new TreeNodeData(
        row,
        m.group(3).trim(),
        trimToNull(m.group(4)),
        flags.contains(EXTINCT_SYMBOL),
        flags.contains(BASIONYM_SYMBOL),
        flags.contains(HOMOTYPIC_SYMBOL),
        flags.contains(PROVISIONAL_SYMBOL),
        parseInfos(m.group(5)),
        m.group(6)
    );
  }

  private static SimpleTreeNode simpleNode(TreeNodeData d, SimpleTreeNode parent) {
    return new SimpleTreeNode(d.id(), d.name(), d.rank(), d.extinct(), d.basionym(),
        d.homotypic(), d.provisional(), d.infos(), d.comment());
  }

  private static Map<String, String[]> parseInfos(String infoString) throws IllegalArgumentException {
    if (infoString != null) {
      Matcher im = INFO_PARSER.matcher(infoString);
      Map<String, String[]> infos = new LinkedHashMap<>();
      while (im.find()) {
        var vals = COMMA_SPLITTER.split(im.group(2).trim());
        infos.put(im.group(1), Arrays.stream(vals)
                .map(x->x.replaceAll(",,", ","))
                .toArray(String[]::new)
        );
      }
      return infos;
    }
    return Collections.emptyMap();
  }

  /**
   * The names sitting at the root level, in the order they were read. The list is mutable and backs
   * the tree directly.
   *
   * @return the root level nodes
   */
  public List<T> getRoot() {
    return root;
  }

  /**
   * Prints the entire tree to a given output
   *
   * @param out the output to append to
   * @throws IOException if the output could not be written to
   */
  public void print(Appendable out) throws IOException {
    for (T n : root) {
      n.print(out, 0, false);
    }
  }

  /**
   * Prints the tree into a given output stream using UTF8.
   * The stream remains open and must be closed by the caller.
   *
   * @param out the stream to write to
   * @throws IOException if the output could not be written to
   */
  public void print(OutputStream out) throws IOException {
    Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8);
    print(w);
    w.flush();
  }

  /**
   * Prints the tree into a new UTF8 encoded text file.
   *
   * @param f the file to create or overwrite
   * @throws IOException if the file could not be written
   */
  public void print(File f) throws IOException {
    try (Writer w = new FileWriter(f, StandardCharsets.UTF_8)) {
      print(w);
      w.flush();
    }
  }

  /**
   * Prints the tree into a given PrintStream using UTF8.
   *
   * @param out the stream to write to
   * @throws IOException if the output could not be written to
   */
  public void print(PrintStream out) throws IOException {
    print((Appendable)out);
  }

  @Override
  public Iterator<T> iterator() {
    return new NNIterator(this);
  }
  
  private class NNIter {
    private int synIdx;
    private final T node;
    
    NNIter(T node) {
      this.node = node;
    }
    
    boolean moreSynonyms() {
      return node.synonyms.size() > synIdx;
    }
    
    NNIter nextSynonym() {
      T n = node.synonyms.get(synIdx);
      synIdx++;
      return new NNIter(n);
    }
  }
  
  private class NNIterator implements Iterator<T> {
    private LinkedList<NNIter> stack = new LinkedList<>();
    private NNIter curr = null;
    
    NNIterator(Tree<T> tree) {
      for (T r : tree.getRoot()) {
        this.stack.addFirst(new NNIter(r));
      }
    }
    
    @Override
    public boolean hasNext() {
      return !stack.isEmpty() || (curr != null && curr.moreSynonyms());
    }
    
    @Override
    public T next() {
      if (curr == null) {
        poll();
        return curr.node;
        
      } else if (curr.moreSynonyms()) {
        return curr.nextSynonym().node;
        
      } else {
        poll();
        return curr.node;
      }
    }
    
    private void poll() {
      curr = stack.removeLast();
      if (!curr.node.children.isEmpty()) {
        var iter = curr.node.children.descendingIterator();
        while (iter.hasNext()) {
          stack.add(new NNIter(iter.next()));
        }
      }
    }
    
    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
