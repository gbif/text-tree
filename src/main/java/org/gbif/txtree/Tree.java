package org.gbif.txtree;

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
  public static final String SYNONYM_SYMBOL = "*";
  public static final String BASIONYM_SYMBOL = "$";
  private static final Logger LOG = LoggerFactory.getLogger(Tree.class);
  private static final Pattern LINE_PARSER = Pattern.compile("^" +
      "( *)" +  // indent #1
      "(\\" + SYNONYM_SYMBOL + ")?" +  // #2
      "(\\" + BASIONYM_SYMBOL + ")?" +  // #3
      "(.+?)" +   // name & author #4
      "(?: \\[([a-z]+)])?" +  // rank #5
      "(?: +\\{(.+)})?" +  // infos #6
      "(?: +#.*)?" +  // comments
      " *$");
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
   * Builds a new simple tree instance by parsing the given input stream.
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
   * Builds a new simple tree instance by parsing the given input stream.
   * In addition to {@link #simple(InputStream) read(InputStream)} it takes an optional listener
   * that is passed the verbatim tree line instance for each processed row.
   */
  public static Tree<SimpleTreeNode> simple(InputStream stream, Consumer<TreeLine> listener) throws IOException {
    return parse(stream, listener, Tree::simpleNode);
  }

  /**
   * Builds a new parsed tree instance by parsing the given input stream
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

  /**
   * Builds a new parsed tree instance by parsing the given input stream
   * and using the GBIF name parser to create parsed names.
   *
   * In addition to {@link #parsed(InputStream) read(InputStream)} it takes an optional listener
   * that is passed the verbatim tree line instance for each processed row.
   */
  public static Tree<ParsedTreeNode> parsed(InputStream stream, Consumer<TreeLine> listener) throws IOException {
    return parse(stream, listener, Tree::parsedNode);
  }

  private static <T extends TreeNode<T>> Tree<T> parse(InputStream stream,
                                                       Consumer<TreeLine> listener,
                                                       BiFunction<Long, Matcher, T> builder
                                                       ) throws IOException {
    Tree<T> tree = new Tree<>();
    LinkedList<T> parents = new LinkedList<>();

    BufferedReader br = new BufferedReader(new InputStreamReader(stream));
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
            TreeLine tl = new TreeLine(row, level, line.trim());
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

  private static SimpleTreeNode simpleNode(long row, Matcher m) {
    boolean basionym = m.group(3) != null;
    String name = m.group(4).trim();
    Rank rank = parseRank(m);
    return new SimpleTreeNode(row, name, rank, basionym, parseInfos(m));
  }

  private static ParsedTreeNode parsedNode(long row, Matcher m) {
    boolean basionym = m.group(3) != null;
    String name = m.group(4).trim();
    Rank rank = parseRank(m);

    ParsedName pn = null;
    try {
      pn = NAME_PARSER.parse(name, rank, null);
    } catch (UnparsableNameException e) {
      LOG.warn("Failed to parse {} {}", e.getType(), e.getName());
    }
    return new ParsedTreeNode(row, name, rank, pn, basionym, parseInfos(m));
  }

  private static Rank parseRank(Matcher m) throws IllegalArgumentException {
    if (m.group(5) != null) {
      return Rank.valueOf(m.group(5).toUpperCase());
    }
    return Rank.UNRANKED;
  }

  private static Map<String, String[]> parseInfos(Matcher m) throws IllegalArgumentException {
    if (m.group(6) != null) {
      Matcher im = INFO_PARSER.matcher(m.group(6));
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
   */
  public void print(OutputStream out) throws IOException {
    Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8);
    print(w);
  }

  /**
   * Prints the tree into a new UTF8 encoded text file.
   */
  public void print(File f) throws IOException {
    Writer w = new FileWriter(f, StandardCharsets.UTF_8);
    print(w);
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
    
    public boolean moreSynonyms() {
      return node.synonyms.size() > synIdx;
    }
    
    public NNIter nextSynonym() {
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
      while (!curr.node.children.isEmpty()) {
        stack.add(new NNIter(curr.node.children.removeLast()));
      }
    }
    
    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
