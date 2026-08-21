package org.gbif.txtree;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;


public class TreeTest {

  @Test
  public void simpleWithRanks() throws Exception {
    var tree = Tree.simple(resource("badranks.txtree"));

    tree.print(System.out);

    StringWriter buffer = new StringWriter();
    tree.print(buffer);
    assertEquals(buffer.toString().trim(), read("badranks.txtree").trim());


    System.out.println("Tree traversal");
    for (var n : tree) {
      assertNotNull(n.name);
      assertNotNull(n.rank);
    }
  }

  @Test
  public void simple() throws Exception {
    var tree = Tree.simple(resource("test.txt"));

    tree.print(System.out);

    StringWriter buffer = new StringWriter();
    tree.print(buffer);
    assertEquals(buffer.toString().trim(), read("test.txt").trim());


    System.out.println("Tree traversal");
    for (var n : tree) {
      assertNotNull(n.name);
      assertNull(n.rank);
    }
  }

  @Test
  public void simple2() throws Exception {
    var tree = Tree.simple(resource("test2.txt"));

    tree.print(System.out);

    StringWriter buffer = new StringWriter();
    tree.print(buffer);
    assertEquals(read("test2-no-comments.txt").trim(), buffer.toString().trim());


    System.out.println("Tree traversal");
    for (var n : tree) {
      System.out.println(n.name);
      assertNotNull(n.name);
      if (!n.name.startsWith("californicum")) {
        assertNotNull(n.rank);
      }
    }
  }

  @Test
  public void simpleHomotypic() throws Exception {
    var tree = Tree.simple(resource("homotypic.txtree"));

    tree.print(System.out);

    StringWriter buffer = new StringWriter();
    tree.print(buffer);
    assertEquals(read("homotypic.txtree").trim(), buffer.toString().trim());


    int homotpics = 0;
    System.out.println("Tree traversal");
    for (var n : tree) {
      System.out.println(n.name);
      assertNotNull(n.name);
      assertNotNull(n.rank);
      if (n.homotypic) homotpics++;
    }
    assertEquals(9, homotpics);
  }

  @Test
  public void simpleExtinct() throws Exception {
    var tree = Tree.simple(resource("extinct.txtree"));

    tree.print(System.out);

    StringWriter buffer = new StringWriter();
    tree.print(buffer);
    assertEquals(read("extinct.txtree").trim(), buffer.toString().trim());

    int extinct = 0;
    System.out.println("Tree traversal");
    for (var n : tree) {
      System.out.println(n.name);
      assertNotNull(n.name);
      assertNotNull(n.rank);
      if (n.extinct) extinct++;
    }
    assertEquals(8, extinct);
  }

  @Test
  public void oldStyleSynonyms() throws Exception {
    var tree = Tree.simple(resource("oldstyle.txtree"));

    tree.print(System.out);

    StringWriter buffer = new StringWriter();
    tree.print(buffer);
    // * replaced with =
    assertEquals(read("oldstyle.txtree").trim()
            .replaceAll("\\* *", "="), buffer.toString().trim());
  }

  @Test
  public void comments() throws Exception {
    Tree<SimpleTreeNode> tree = Tree.simple(resource("test4comments.txt"));

    tree.print(System.out);

    for (TreeNode<SimpleTreeNode> n : tree) {
      if (n.name.equals("Abies alba Mill.")) {
        assertEquals("my first comment", n.comment);
      } else if (n.name.equals("Picea")) {
        assertEquals("2nd comment", n.comment);
      } else if (n.name.equals("Picea alba L.")) {
        assertEquals("I love to leave URLs as references where I found things: https://de.wikipedia.org/wiki/Titusbogen", n.comment);
      } else {
        assertNull(n.comment);
      }
    }
  }

  @Test
  public void provisional() throws Exception {
    Tree<SimpleTreeNode> tree = Tree.simple(resource("prov.txtree"));

    assertEquals(13, tree.size());
    int counter = 0;
    for (TreeNode<SimpleTreeNode> n : tree) {
      counter++;
      assertFalse(n.name.startsWith("?"));
      if (n.name.startsWith("Troximon humilis") || n.name.startsWith("Macrorhynchus humilis") || n.name.startsWith("Cichorioideae")) {
        assertTrue(n.provisional);
      } else {
        assertFalse(n.provisional);
      }
    }
    assertEquals(tree.size(), counter);

    StringWriter buffer = new StringWriter();
    tree.print(buffer);
    assertEquals(read("prov.txtree").trim(), buffer.toString().trim());

  }

  @Test
  public void multiIteration() throws Exception {
    Tree<SimpleTreeNode> tree = Tree.simple(resource("prov.txtree"));
    assertEquals(13, tree.size());

    StringWriter buffer = new StringWriter();
    tree.print(buffer);
    String v1 = buffer.toString().trim();

    buffer = new StringWriter();
    tree.print(buffer);
    String v2 = buffer.toString().trim();

    assertEquals(v2, v1);
  }

  @Test
  public void testVerify() throws Exception {
    assertFalse(Tree.verify(resource("badly-indented.txt")).valid);

    assertTrue(Tree.verify(resource("test.txt")).valid);
    assertTrue(Tree.verify(resource("test2.txt")).valid);
    assertTrue(Tree.verify(resource("test-ranks.txt")).valid);

    assertFalse(Tree.verify(resource("badly-indented.txt")).valid);
    assertFalse(Tree.verify(resource("badtree.txt")).valid);
    assertFalse(Tree.verify(resource("notree.txt")).valid);
    assertFalse(Tree.verify(resource("notree2.txt")).valid);
    assertFalse(Tree.verify(resource("dwca.txt")).valid);
  }

  @Test
  public void infos() throws Exception {
    var tree = Tree.simple(resource("test3.txt"));

    tree.print(System.out);

    StringWriter buffer = new StringWriter();
    tree.print(buffer);
    assertEquals(read("test3clean.txt").trim(), buffer.toString().trim());

    System.out.println("Tree traversal");
    for (var n : tree) {
      assertNotNull(n.name);
    }
  }

  @Test
  public void infos2() throws Exception {
    assertTrue(Tree.verify(resource("infos.txt")).valid);
    var tree = Tree.simple(resource("infos.txt"));

    tree.print(System.out);

    System.out.println("Tree traversal");
    boolean abies_alba_found = false;
    for (var n : tree) {
      assertNotNull(n.name);
      if (n.name.startsWith("Abies alba")) {
        abies_alba_found = true;
        // ID=1234
        // PUB=Miller2019
        // ENV=terrestrial,marine
        // REF=Döring2021,Banki2022
        // VERN=de:Traubeneiche,fr:Chêne rouvre,dk:Vintereg,nl:Wintereik
        assertEquals(5, n.infos.size());
        assertArrayEquals(new String[]{"1234"}, n.infos.get("ID"));
        assertArrayEquals(new String[]{"Miller2019"}, n.infos.get("PUB"));
        assertArrayEquals(new String[]{"terrestrial","marine"}, n.infos.get("ENV"));
        assertArrayEquals(new String[]{"Döring2021","Banki2022"}, n.infos.get("REF"));
        assertArrayEquals(new String[]{"de:Traubeneiche","fr:Chêne rouvre","dk:Vintereg","nl:Wintereik"}, n.infos.get("VERN"));

      } else if (n.name.startsWith("Abies balsamea")) {
        assertEquals(1, n.infos.size());
        assertArrayEquals(new String[]{"Miller2019"}, n.infos.get("PUB"));

      } else if (n.name.startsWith("Abies alpina")) {
        assertEquals(3, n.infos.size());
        assertArrayEquals(new String[]{"botany"}, n.infos.get("CODE"));
        assertArrayEquals(new String[]{"holotype:Berlin,B45641","paratype:Berlin,B8932"}, n.infos.get("TM"));

      } else if (n.name.equals("Abies")) {
        assertEquals(2, n.infos.size());
        assertArrayEquals(new String[]{"Abies_alba_Mill."}, n.infos.get("TYPE"));

      } else {
        assertTrue(n.infos.isEmpty());
      }
    }
    assertTrue(abies_alba_found);
  }

  @Test
  public void blankLinesAreSkipped() throws Exception {
    // a line holding only spaces or a tab is not content and must be skipped by both
    // verify() and simple() — they disagreed on blank lines specifically until 2.0.0
    assertTrue(Tree.verify(resource("blanklines.txtree")).valid);

    var tree = Tree.simple(resource("blanklines.txtree"));
    assertEquals(4, tree.size());

    StringWriter buffer = new StringWriter();
    tree.print(buffer);
    assertEquals("""
        Chordata [phylum]
          Mammalia [class]
            Canis [genus]
          Aves [class]""", buffer.toString().trim());
  }

  @Test
  public void multipleRootsAndARootLevelSynonym() throws Exception {
    // dwca.txt (2 roots) and test-ranks.txt (96 roots) only ever reach verify(), never parse() -
    // pin down that parse() itself copes with more than one root and with a root-level synonym,
    // the two branches the parent-stack reorder touched
    String content = """
        Aaa [genus]
          Bbb [species]
        Ccc [genus]
        =Ddd [species]""";

    var tree = Tree.simple(new StringReader(content));

    assertEquals(4, tree.size());
    assertEquals(3, tree.getRoot().size());
    assertEquals("Aaa", tree.getRoot().get(0).name);
    assertEquals(1, tree.getRoot().get(0).children.size());
    assertEquals("Bbb", tree.getRoot().get(0).children.getFirst().name);
    assertEquals("Ccc", tree.getRoot().get(1).name);
    // the = prefix at root level must not make Ddd land in some node's synonyms list instead
    assertEquals("Ddd", tree.getRoot().get(2).name);
    for (var n : tree) {
      assertTrue(n.synonyms.isEmpty());
    }
  }

  @Test
  public void customNodeFactory() throws Exception {
    // the SPI text-tree-parsed builds on: a factory sees every component of a line, plus its parent
    var seen = new java.util.ArrayList<TreeNodeData>();
    var parents = new java.util.ArrayList<String>();
    TreeNodeFactory<SimpleTreeNode> factory = (d, parent) -> {
      seen.add(d);
      parents.add(parent == null ? null : parent.name);
      return new SimpleTreeNode(d.id(), d.name(), d.rank(), d.extinct(), d.basionym(),
          d.homotypic(), d.provisional(), d.infos(), d.comment());
    };

    var tree = Tree.parse(new java.io.InputStreamReader(resource("extinct.txtree"),
        StandardCharsets.UTF_8), null, factory);

    assertEquals(tree.size(), seen.size());
    assertEquals("Animalia", seen.get(0).name());
    assertEquals("kingdom", seen.get(0).rank());
    assertEquals(1, seen.get(0).id());
    assertFalse(seen.get(0).extinct());
    assertTrue(seen.stream().anyMatch(TreeNodeData::extinct));

    // the root has no parent, everything below it does
    assertNull(parents.get(0));
    assertEquals("Animalia", parents.get(1));
    assertTrue(parents.stream().skip(1).allMatch(java.util.Objects::nonNull));
  }

  @Test
  public void verifyRejectsWhatParseRejects() throws Exception {
    // verify() seeded its indent tracking at 0, so it accepted a tree whose very first content line
    // was already indented - which parse() then refused for want of a root. ChecklistBank validates
    // uploads with verify() and imports them with simple(), so such a file passed validation and
    // failed the import.
    for (String bad : new String[]{
        "  Aaa [genus]\n    Bbb [species]",   // whole file shifted one level
        "  Aaa [genus]\nBbb [genus]",         // first line indented, the rest at the root
        "  Aaa [genus]"                       // a single indented line
    }) {
      assertFalse(Tree.verify(new StringReader(bad)).valid, () -> "should be invalid: " + bad);
      assertThrows(IllegalArgumentException.class, () -> Tree.simple(new StringReader(bad)),
          () -> "should not parse: " + bad);
    }

    // and a properly rooted tree still verifies and parses
    String good = "Aaa [genus]\n  Bbb [species]";
    assertTrue(Tree.verify(new StringReader(good)).valid);
    assertEquals(2, Tree.simple(new StringReader(good)).size());
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