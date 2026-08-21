package org.gbif.txtree.parsed;

import org.gbif.nameparser.api.NameType;
import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.ParseResult;
import org.gbif.nameparser.api.Rank;
import org.gbif.txtree.Tree;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ParsedTreeTest {

  private final FakeNameParser parser = new FakeNameParser();

  @Test
  public void parsed() throws Exception {
    Tree<ParsedTreeNode> tree = ParsedTree.parse(resource("test2.txt"), parser);

    StringWriter buffer = new StringWriter();
    tree.print(buffer);
    assertEquals(read("test2-no-comments.txt").trim(), buffer.toString().trim());

    for (ParsedTreeNode n : tree) {
      assertNotNull(n.name);
      assertNotNull(n.result);
      if (!n.name.startsWith("californicum")) {
        assertNotNull(n.rank);
      }
      assertNotNull(n.parsedName);
    }
  }

  @Test
  public void rankComesFromTheLineNotTheName() throws Exception {
    // an unparsable name used to lose its rank, because the rank was read off the ParsedName
    ParsedTreeNode n = first("BOLD:AAA1234 [species]");

    assertEquals("species", n.rank);
    assertInstanceOf(ParseResult.Unparsable.class, n.result);
    assertEquals(NameType.PLACEHOLDER, n.result.type());
    assertNull(n.parsedName);
  }

  @Test
  public void informalNamesKeepAParsedName() throws Exception {
    // 4.x returned a type=INFORMAL ParsedName rather than throwing, so parsedName stays populated
    ParsedTreeNode n = first("Rhizobium sp. [species]");

    assertInstanceOf(ParseResult.Informal.class, n.result);
    assertNotNull(n.parsedName);
    assertEquals("Rhizobium", n.parsedName.getGenus());
    assertEquals(Rank.SPECIES, n.parsedName.getRank());
  }

  @Test
  public void theTreeRankOverridesTheParsedOne() throws Exception {
    // FakeNameParser always reports UNRANKED, so GENUS here can only come from the [genus] marker
    ParsedTreeNode n = first("Abies [genus]");

    assertEquals(Rank.GENUS, n.parsedName.getRank());
    assertEquals("genus", n.rank);
  }

  @Test
  public void unknownRankIsRejectedWithTheLineNumber() {
    var e = assertThrows(IllegalArgumentException.class,
        () -> ParsedTree.parse(tree("Chordata [phylum]\n  Lynx [grrr]"), parser));

    assertTrue(e.getMessage().contains("grrr"), e.getMessage());
    assertTrue(e.getMessage().contains("line 2"), e.getMessage());
  }

  @Test
  public void ambiguousRankWithoutACodeThrows() {
    // Rank.DIVISION was split into DIVISION_BOTANY / DIVISION_ZOOLOGY in name-parser-api 5.0.0
    var e = assertThrows(IllegalArgumentException.class,
        () -> ParsedTree.parse(tree("Felidae [division]"), parser));

    assertTrue(e.getMessage().contains("division botany"), e.getMessage());
    assertTrue(e.getMessage().contains("division zoology"), e.getMessage());
    assertTrue(e.getMessage().contains("CODE"), e.getMessage());
  }

  @Test
  public void codeIsInheritedFromAncestors() throws Exception {
    Tree<ParsedTreeNode> tree = ParsedTree.parse(
        tree("Plantae [kingdom] {CODE=botany}\n  Bryophyta [division]"), parser);

    ParsedTreeNode root = tree.getRoot().get(0);
    ParsedTreeNode child = root.children.getFirst();
    assertEquals(NomCode.BOTANICAL, root.code);
    assertEquals(NomCode.BOTANICAL, child.code);
    assertEquals("division_botany", child.rank);
  }

  @Test
  public void aNodeCodeOverridesTheInheritedOne() throws Exception {
    Tree<ParsedTreeNode> tree = ParsedTree.parse(
        tree("Life [domain] {CODE=botany}\n  Animalia [kingdom] {CODE=zoology}\n    Felidae [division]"),
        parser);

    ParsedTreeNode division = tree.getRoot().get(0).children.getFirst().children.getFirst();
    assertEquals(NomCode.ZOOLOGICAL, division.code);
    assertEquals("division_zoology", division.rank);
  }

  @Test
  public void anUnrecognisedCodeIsIgnored() throws Exception {
    Tree<ParsedTreeNode> tree = ParsedTree.parse(
        tree("Plantae [kingdom] {CODE=botany}\n  Bryophyta [division] {CODE=nonsense}"), parser);

    ParsedTreeNode child = tree.getRoot().get(0).children.getFirst();
    assertEquals(NomCode.BOTANICAL, child.code, "an unparsable CODE falls back to the inherited one");
    assertEquals("division_botany", child.rank);
  }

  @Test
  public void anExplicitRankIsNotRewrittenByTheCode() throws Exception {
    // the author wrote the zoological constant explicitly; a botany CODE must not overrule it
    Tree<ParsedTreeNode> tree = ParsedTree.parse(
        tree("Plantae [kingdom] {CODE=botany}\n  Felidae [division zoology]"), parser);

    assertEquals("division_zoology", tree.getRoot().get(0).children.getFirst().rank);
  }

  @Test
  public void acronymsResolveExceptAmbiguousOnes() throws Exception {
    assertEquals(NomCode.ZOOLOGICAL, first("Canis [genus] {CODE=ICZN}").code);
    assertEquals(NomCode.BOTANICAL, first("Abies [genus] {CODE=ICN}").code);
    assertEquals(NomCode.BOTANICAL, first("Abies [genus] {CODE=BOTANICAL}").code);
    // PHYTO and PHYLO both report the acronym ICPN, so it must resolve to neither
    assertNull(first("Abies [genus] {CODE=ICPN}").code);
  }

  @Test
  public void theCodeIsPassedToTheParser() throws Exception {
    ParsedTree.parse(tree("Plantae [kingdom] {CODE=botany}\n  Abies [genus]"), parser);

    assertEquals(List.of(NomCode.BOTANICAL, NomCode.BOTANICAL), parser.codesSeen);
  }

  @Test
  public void aNullParserIsRejectedUpFront() {
    assertThrows(NullPointerException.class, () -> ParsedTree.factory(null));
  }

  private ParsedTreeNode first(String content) throws IOException {
    return ParsedTree.parse(tree(content), parser).getRoot().get(0);
  }

  private static InputStream tree(String content) {
    return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
  }

  static InputStream resource(String resourceName) {
    return ClassLoader.getSystemResourceAsStream(resourceName);
  }

  static String read(String resourceName) throws IOException {
    try (InputStream in = resource(resourceName)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
