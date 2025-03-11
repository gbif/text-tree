package org.gbif.txtree;

import org.apache.commons.lang3.StringUtils;
import org.gbif.nameparser.NameParserGBIF;
import org.gbif.nameparser.api.ParsedName;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.api.UnparsableNameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Simple class to keep a taxonomy of names.
 * We use this to compare nub build outputs with a very simple text based tree format that is very easy to read.
 * Especially useful for larger tree snippets.
 *
 * Iterating over the tree goes in
 */
public class Tree<T extends TreeNode<T>> implements Iterable<T> {
  public static final String SYNONYM_SYMBOL = "=";
  public static final String HOMOTYPIC_SYMBOL = "≡";
  public static final String BASIONYM_SYMBOL = "$";
  public static final String EXTINCT_SYMBOL = "†";
  public static final String PROVISIONAL_SYMBOL = "?";

  private static final Logger LOG = LoggerFactory.getLogger(Tree.class);
  private static final String ANY_CHAR = "[^\t\n\r]";
  private static final Pattern LINE_PARSER = Pattern.compile("^" +
      "((?:  )*)" +  // indent #1
      "([*" + SYNONYM_SYMBOL + HOMOTYPIC_SYMBOL + "])?" +  // #2
      "(\\" + BASIONYM_SYMBOL + ")?" +  // #3
      "(" + EXTINCT_SYMBOL + ")?" +  // #4
      "(\\" + PROVISIONAL_SYMBOL + ")?" +  // #5
      "([^\t\n\r\\s]"+ANY_CHAR+"+?)" +   // name & author #6
      "(?: \\[([a-z _-]+)])?" +  // rank #7
      "(?: +\\{("+ANY_CHAR+"*)})?" +  // infos #8
      "(?:\\s+#\\s*(.*))?" +  // comments #9
      "\\s*$");
  private static final Pattern INFO_PARSER = Pattern.compile("([A-Z]+)=([^=]+)(?: |$)");
  private static final Pattern COMMA_SPLITTER = Pattern.compile("\\s*,\\s*");
  private static final NameParserGBIF NAME_PARSER = new NameParserGBIF();
  private long count;
  private final List<T> root = new ArrayList<>();



  /**
   * @return the total number of nodes in the tree
   */
  public long size() {
    return count;
  }

  // to avoid dependency on apache or guava
  static boolean isBlank(String x) {
    int strLen;
    if (x == null || (strLen = x.length()) == 0) return true;
    for (int i = 0; i < strLen; i++) {
      if (!Character.isWhitespace(x.charAt(i))) {
        return false;
      }
    }
    return false;
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
   * In addition to {@link #simple(InputStream) read(InputStream)} it takes an optional listener
   * that is passed the verbatim tree line instance for each processed row.
   */
  public static Tree<SimpleTreeNode> simple(InputStream stream, Consumer<TreeLine> listener) throws IOException {
    return parse(new InputStreamReader(stream, StandardCharsets.UTF_8), listener, Tree::simpleNode);
  }

  public static Tree<SimpleTreeNode> simple(Reader reader) throws IOException {
    return parse(reader, null, Tree::simpleNode);
  }

  /**
   * Builds a new parsed tree instance by parsing the given UTF8 input stream
   * and using the GBIF name parser to create parsed names.
   *
   * @param stream the input stream to parse
   * @return the new tree instance
   * @throws IOException if the input could not be read
   * @throws IllegalArgumentException if the input contained was badly formatted
   */
  public static Tree<ParsedTreeNode> parsed(InputStream stream) throws IOException {
    return parsed(stream, null);
  }

  public static Tree<ParsedTreeNode> parsed(Reader reader) throws IOException {
    return parse(reader, null, Tree::parsedNode);
  }

  /**
   * Builds a new parsed tree instance by parsing the given UTF8 input stream
   * and using the GBIF name parser to create parsed names.
   *
   * In addition to {@link #parsed(InputStream) read(InputStream)} it takes an optional listener
   * that is passed the verbatim tree line instance for each processed row.
   */
  public static Tree<ParsedTreeNode> parsed(InputStream stream, Consumer<TreeLine> listener) throws IOException {
    return parse(new InputStreamReader(stream, StandardCharsets.UTF_8), listener, Tree::parsedNode);
  }

  private static <T extends TreeNode<T>> Tree<T> parse(Reader reader,
                                                       Consumer<TreeLine> listener,
                                                       BiFunction<Long, Matcher, T> builder
                                                       ) throws IOException {
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

          T n = builder.apply(row, m);
          if (level == 0) {
            tree.getRoot().add(n);
            parents.clear();
            parents.add(n);

          } else {
            while (parents.size() > level) {
              // remove latest parents until we are at the right level
              parents.removeLast();
            }
            if (parents.size() < level) {
              throw new IllegalArgumentException("Tree is not properly indented on line " + row + ". Use 2 spaces for children: " + line);
            }
            T p = parents.peekLast();
            if (m.group(2) != null) {
              p.synonyms.add(n);
            } else {
              p.children.add(n);
            }
            parents.add(n);
          }

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

  public static class VerificationResult {
    public final boolean valid;
    public final int lines;
    public final String message;

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
   * @return error message or NULL if the input stream contains a valid text tree
   */
  public static VerificationResult verify(InputStream stream) throws IOException {
    return verify(new InputStreamReader(stream, StandardCharsets.UTF_8));
  }

  /**
   * Verifies that the given input stream contains a valid text tree.
   * Especially useful for verifying that the tree is properly indented.
   * @param reader tree input
   * @return error message or NULL if the input stream contains a valid text tree
   */
  public static VerificationResult verify(Reader reader) throws IOException {
    var br = new BufferedReader(reader);
    String line = br.readLine();
    int counter = 0;
    try {
      int max = 0;
      int last = 0;
      while (line != null) {
        if (!StringUtils.isBlank(line)) {
          Matcher m = LINE_PARSER.matcher(line);
          if (m.find()) {
            int level = m.group(1).length();
            max = Math.max(max, level);
            if (level % 2 != 0) {
              return VerificationResult.failed(String.format("Tree is not indented properly on line %s. Use 2 spaces only: %s", counter, line), counter);
            }
            if (level-last>2) {
              return VerificationResult.failed(String.format("Tree is indented too much on line %s. Use 2 spaces only: %s", counter, line), counter);
            }
            last = level;
          } else {
            return VerificationResult.failed(String.format("Failed to parse Tree on line %s: %s", counter, line), counter);
          }
        }
        line = br.readLine();
        counter++;
      }
      if (max==0 && counter > 8) {
        return VerificationResult.failed("Tree is not indented at all", counter);
      }

    } catch (IllegalArgumentException e) {
      return VerificationResult.failed(String.format("Failed to parse Tree on line %s: %s", counter, line), counter);
    }
    // should we require some other level than just 0???
    return VerificationResult.valid(counter);
  }

  private static SimpleTreeNode simpleNode(long row, Matcher m) {
    boolean homotypic = Objects.equals(m.group(2), HOMOTYPIC_SYMBOL);
    boolean basionym = m.group(3) != null;
    boolean extinct = m.group(4) != null;
    boolean provisional = m.group(5) != null;
    String name = m.group(6).trim();
    String rank = StringUtils.trimToNull(m.group(7));
    return new SimpleTreeNode(row, name, rank, extinct, basionym, homotypic, provisional, parseInfos(m), m.group(9));
  }

  private static ParsedTreeNode parsedNode(long row, Matcher m) {
    boolean homotypic = Objects.equals(m.group(2), HOMOTYPIC_SYMBOL);
    boolean basionym = m.group(3) != null;
    boolean extinct = m.group(4) != null;
    boolean provisional = m.group(5) != null;
    String name = m.group(6).trim();
    Rank rank = null;
    String vrank = StringUtils.trimToNull(m.group(7));
    if (vrank != null) {
      rank = Rank.valueOf(vrank.toUpperCase().replace(' ', '_'));
    }

    ParsedName pn = null;
    try {
      pn = NAME_PARSER.parse(name, rank, null);
      pn.setRank(rank); // make sure to keep the original rank
    } catch (UnparsableNameException e) {
      LOG.warn("Failed to parse {} {}", e.getType(), e.getName());
    } catch (InterruptedException e) {
      throw new RuntimeException(e); // not great, but dont want to expose the exception
    }
    return new ParsedTreeNode(row, name, pn, extinct, basionym, homotypic, provisional, parseInfos(m), m.group(9));
  }

  private static Map<String, String[]> parseInfos(Matcher m) throws IllegalArgumentException {
    if (m.group(8) != null) {
      Matcher im = INFO_PARSER.matcher(m.group(8));
      Map<String, String[]> infos = new LinkedHashMap<>();
      while (im.find()) {
        infos.put(im.group(1), COMMA_SPLITTER.split(im.group(2).trim()));
      }
      return infos;
    }
    return Collections.EMPTY_MAP;
  }

  public List<T> getRoot() {
    return root;
  }

  /**
   * Prints the entire tree to a given output
   */
  public void print(Appendable out) throws IOException {
    for (T n : root) {
      n.print(out, 0, false);
    }
  }

  /**
   * Prints the tree into a given output stream using UTF8.
   * The stream remains open and must be closed by the caller.
   */
  public void print(OutputStream out) throws IOException {
    Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8);
    print(w);
    w.flush();
  }

  /**
   * Prints the tree into a new UTF8 encoded text file.
   */
  public void print(File f) throws IOException {
    try (Writer w = new FileWriter(f, StandardCharsets.UTF_8)) {
      print(w);
      w.flush();
    }
  }

  /**
   * Prints the tree into a given PrintStream using UTF8.
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
