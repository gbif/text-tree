package org.gbif.txtree;

import org.apache.commons.io.IOUtils;
import org.gbif.nameparser.api.Rank;
import org.junit.Test;

import java.io.InputStream;
import java.io.StringWriter;

import static org.junit.Assert.*;


public class TreeTest {

  @Test
  public void simpleWithRanks() throws Exception {
    var tree = Tree.simple(resource("badranks.txtree"));

    tree.print(System.out);

    StringWriter buffer = new StringWriter();
    tree.print(buffer);
    assertEquals(buffer.toString().trim(), IOUtils.toString(resource("badranks.txtree"), "UTF8").trim());


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
    assertEquals(buffer.toString().trim(), IOUtils.toString(resource("test.txt"), "UTF8").trim());


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
    assertEquals(IOUtils.toString(resource("test2-no-comments.txt"), "UTF8").trim(), buffer.toString().trim());


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
    assertEquals(IOUtils.toString(resource("homotypic.txtree"), "UTF8").trim(), buffer.toString().trim());


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
    assertEquals(IOUtils.toString(resource("extinct.txtree"), "UTF8").trim(), buffer.toString().trim());

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
    assertEquals(IOUtils.toString(resource("oldstyle.txtree"), "UTF8").trim()
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
    assertEquals(IOUtils.toString(resource("prov.txtree"), "UTF8").trim(), buffer.toString().trim());

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
  public void parsed() throws Exception {
    Tree<ParsedTreeNode> tree = Tree.parsed(resource("test2.txt"));

    tree.print(System.out);

    StringWriter buffer = new StringWriter();
    tree.print(buffer);
    assertEquals(IOUtils.toString(resource("test2-no-comments.txt"), "UTF8").trim(), buffer.toString().trim());

    System.out.println("Tree traversal");
    for (ParsedTreeNode n : tree) {
      System.out.println(n.name);
      assertNotNull(n.name);
      if (!n.name.startsWith("californicum")) {
        assertNotNull(n.rank);
      }
      assertNotNull(n.parsedName);
      System.out.println(n.parsedName.canonicalNameComplete()+"\n");
    }
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
    assertEquals(IOUtils.toString(resource("test3clean.txt"), "UTF8").trim(), buffer.toString().trim());

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
        assertEquals(new String[]{"1234"}, n.infos.get("ID"));
        assertEquals(new String[]{"Miller2019"}, n.infos.get("PUB"));
        assertEquals(new String[]{"terrestrial","marine"}, n.infos.get("ENV"));
        assertEquals(new String[]{"Döring2021","Banki2022"}, n.infos.get("REF"));
        assertEquals(new String[]{"de:Traubeneiche","fr:Chêne rouvre","dk:Vintereg","nl:Wintereik"}, n.infos.get("VERN"));

      } else if (n.name.startsWith("Abies balsamea")) {
        assertEquals(1, n.infos.size());
        assertEquals(new String[]{"Miller2019"}, n.infos.get("PUB"));

      } else if (n.name.startsWith("Abies alpina")) {
        assertEquals(3, n.infos.size());
        assertEquals(new String[]{"botany"}, n.infos.get("CODE"));
        assertEquals(new String[]{"holotype:Berlin,B45641","paratype:Berlin,B8932"}, n.infos.get("TM"));

      } else if (n.name.equals("Abies")) {
        assertEquals(2, n.infos.size());
        assertEquals(new String[]{"Abies_alba_Mill."}, n.infos.get("TYPE"));

      } else {
        assertTrue(n.infos.isEmpty());
      }
    }
    assertTrue(abies_alba_found);
  }

  static InputStream resource(String resourceName) {
    return ClassLoader.getSystemResourceAsStream(resourceName);
  }

}