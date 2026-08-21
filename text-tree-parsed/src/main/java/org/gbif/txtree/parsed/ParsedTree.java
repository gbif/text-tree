package org.gbif.txtree.parsed;

import org.gbif.nameparser.api.NameParser;
import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.ParseResult;
import org.gbif.nameparser.api.Rank;
import org.gbif.nameparser.util.RankUtils;
import org.gbif.txtree.Tree;
import org.gbif.txtree.TreeLine;
import org.gbif.txtree.TreeNodeData;
import org.gbif.txtree.TreeNodeFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Reads TextTree input into {@link ParsedTreeNode}s, running every name through a
 * {@link NameParser} supplied by the caller.
 *
 * <p>text-tree depends on {@code name-parser-api} only, never on an implementation: the current
 * one, {@code org.gbif.nameparser:name-parser-rust}, needs JDK 22+ and a per platform native jar,
 * which would otherwise be forced on every consumer. Wire it up yourself:
 *
 * <pre>{@code
 * Tree<ParsedTreeNode> tree = ParsedTree.parse(in, new NameParserRust());
 * }</pre>
 *
 * <p>The {@code CODE} info key sets the nomenclatural code for a name and is inherited by its
 * descendants, as in ChecklistBank. It disambiguates the ranks that exist once per code
 * ({@code division}, plus {@code section} and {@code series} with their sub/super forms;
 * {@code subdivision} and {@code superdivision} are single ranks and need no code) and is passed
 * on to the name parser.
 */
public final class ParsedTree {

  /** Recognised {@code CODE} values, lower cased. */
  private static final Map<String, NomCode> NOM_CODES = buildNomCodes();

  private static final String CODE_KEY = "CODE";

  private ParsedTree() {
  }

  public static Tree<ParsedTreeNode> parse(InputStream stream, NameParser parser) throws IOException {
    return parse(stream, parser, null);
  }

  public static Tree<ParsedTreeNode> parse(InputStream stream, NameParser parser,
                                           Consumer<TreeLine> listener) throws IOException {
    return parse(new InputStreamReader(stream, StandardCharsets.UTF_8), parser, listener);
  }

  public static Tree<ParsedTreeNode> parse(Reader reader, NameParser parser) throws IOException {
    return parse(reader, parser, null);
  }

  public static Tree<ParsedTreeNode> parse(Reader reader, NameParser parser,
                                           Consumer<TreeLine> listener) throws IOException {
    return Tree.parse(reader, listener, factory(parser));
  }

  /**
   * The node factory this class drives, exposed so it can be handed to
   * {@link Tree#parse(Reader, Consumer, TreeNodeFactory)} directly. Stateless and reusable;
   * thread safety follows the parser you supply.
   */
  public static TreeNodeFactory<ParsedTreeNode> factory(NameParser parser) {
    Objects.requireNonNull(parser, "parser required");
    return (data, parent) -> {
      NomCode code = code(data, parent);
      Rank rank = rank(data.rank(), data.id(), code);
      ParseResult result = parser.parse(data.name(), null, rank, code);
      return new ParsedTreeNode(data, rank, code, result);
    };
  }

  /**
   * The nomenclatural code in scope: this line's {@code CODE} info if it declares a recognised one,
   * else the parent's. An unrecognised value is ignored rather than rejected — ChecklistBank treats
   * it as a data issue, not a structural error.
   */
  static NomCode code(TreeNodeData data, ParsedTreeNode parent) {
    Map<String, String[]> infos = data.infos();
    String[] values = infos == null ? null : infos.get(CODE_KEY);
    if (values != null && values.length > 0 && values[0] != null) {
      NomCode code = NOM_CODES.get(values[0].trim().toLowerCase(Locale.ROOT));
      if (code != null) {
        return code;
      }
    }
    return parent == null ? null : parent.code;
  }

  /**
   * Resolves the rank as written in the tree to a {@link Rank} constant.
   *
   * <p>A rank that names a constant outright is taken verbatim, including an explicitly
   * code-qualified {@code [division zoology]} — deliberately not run through
   * {@link RankUtils#bestCodeCompliantRank}, which would rewrite it under a conflicting
   * {@code CODE}. Only the bare ambiguous form is resolved by the code in scope. A code that is
   * neither zoological nor botany-ish (e.g. {@code VIRUS}) leaves that resolution falling back to
   * the botanical constant — {@link RankUtils#bestCodeCompliantRank}'s own fallback, not this
   * method's.
   *
   * @param value the verbatim rank, may be null
   * @param line  the line it was read from, used in the error message
   * @param code  the nomenclatural code in scope, may be null
   * @return the rank, or null if the line carried none
   * @throws IllegalArgumentException if the rank is unknown, or ambiguous with no code in scope
   */
  static Rank rank(String value, long line, NomCode code) {
    if (value == null) {
      return null;
    }
    String name = value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');

    Rank explicit = rankOrNull(name);
    if (explicit != null) {
      return explicit;
    }

    // a bare ambiguous form such as [division]: only the code specific constants exist
    Rank botanical = rankOrNull(name + "_BOTANY");
    if (botanical != null) {
      String plain = name.toLowerCase(Locale.ROOT).replace('_', ' ');
      if (code == null) {
        throw new IllegalArgumentException("Ambiguous rank [" + value + "] on line " + line
            + ". It exists for both codes - declare a {CODE=...} or write it out as ["
            + plain + " botany] or [" + plain + " zoology].");
      }
      // bestCodeCompliantRank returns its input unchanged for a null code, hence the check above
      return RankUtils.bestCodeCompliantRank(botanical, code);
    }

    throw new IllegalArgumentException("Unknown rank [" + value + "] on line " + line);
  }

  private static Rank rankOrNull(String name) {
    try {
      return Rank.valueOf(name);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static Map<String, NomCode> buildNomCodes() {
    // an acronym claimed by more than one code resolves to neither - PHYTO and PHYLO both report
    // ICPN today. Counted rather than hardcoded, so the entry appears by itself if that is fixed.
    Map<String, Integer> acronyms = new HashMap<>();
    for (NomCode c : NomCode.values()) {
      acronyms.merge(c.getAcronym().toLowerCase(Locale.ROOT), 1, Integer::sum);
    }

    Map<String, NomCode> map = new HashMap<>();
    for (NomCode c : NomCode.values()) {
      map.put(c.name().toLowerCase(Locale.ROOT), c);
      String acronym = c.getAcronym().toLowerCase(Locale.ROOT);
      if (acronyms.get(acronym) == 1) {
        map.put(acronym, c);
      }
    }

    // the spellings the ChecklistBank publishing guide and its users actually write
    map.put("botany", NomCode.BOTANICAL);
    map.put("bot", NomCode.BOTANICAL);
    map.put("plant", NomCode.BOTANICAL);
    map.put("plants", NomCode.BOTANICAL);
    map.put("zoology", NomCode.ZOOLOGICAL);
    map.put("zoo", NomCode.ZOOLOGICAL);
    map.put("animal", NomCode.ZOOLOGICAL);
    map.put("animals", NomCode.ZOOLOGICAL);
    map.put("bacteria", NomCode.BACTERIAL);
    map.put("bact", NomCode.BACTERIAL);
    map.put("prokaryotes", NomCode.BACTERIAL);
    map.put("virus", NomCode.VIRUS);
    map.put("viruses", NomCode.VIRUS);
    map.put("cultivars", NomCode.CULTIVARS);
    map.put("cultivated plants", NomCode.CULTIVARS);
    map.put("phyto", NomCode.PHYTO);
    map.put("phytosociology", NomCode.PHYTO);
    map.put("phylo", NomCode.PHYLO);
    map.put("phylocode", NomCode.PHYLO);
    map.put("phylogenetic", NomCode.PHYLO);
    return Map.copyOf(map);
  }
}
