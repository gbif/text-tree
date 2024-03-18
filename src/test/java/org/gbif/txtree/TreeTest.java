package org.gbif.txtree;

import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.gbif.nameparser.api.Rank;
import org.junit.Test;

import java.io.InputStream;
import java.io.StringWriter;

import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;


public class TreeTest {

  @Test
  public void simple() throws Exception {
    Tree<?> tree = Tree.simple(resource("test.txt"));

    tree.print(System.out);

    StringWriter buffer = new StringWriter();
    tree.print(buffer);
    assertEquals(buffer.toString().trim(), IOUtils.toString(resource("test.txt"), "UTF8").trim());


    System.out.println("Tree traversal");
    for (TreeNode<?> n : tree) {
      assertNotNull(n.name);
      assertEquals(Rank.UNRANKED, n.rank);
    }
  }

  @Test
  public void simple2() throws Exception {
    Tree<?> tree = Tree.simple(resource("test2.txt"));

    tree.print(System.out);

    StringWriter buffer = new StringWriter();
    tree.print(buffer);
    assertEquals(IOUtils.toString(resource("test2-no-comments.txt"), "UTF8").trim(), buffer.toString().trim());


    System.out.println("Tree traversal");
    for (TreeNode<?> n : tree) {
      System.out.println(n.name);
      assertNotNull(n.name);
      assertNotNull(n.rank);
    }
  }

  @Test
  public void simpleHomotypic() throws Exception {
    Tree<?> tree = Tree.simple(resource("homotypic.txtree"));

    tree.print(System.out);

    StringWriter buffer = new StringWriter();
    tree.print(buffer);
    assertEquals(IOUtils.toString(resource("homotypic.txtree"), "UTF8").trim(), buffer.toString().trim());


    int homotpics = 0;
    System.out.println("Tree traversal");
    for (TreeNode<?> n : tree) {
      System.out.println(n.name);
      assertNotNull(n.name);
      assertNotNull(n.rank);
      if (n.homotypic) homotpics++;
    }
    assertEquals(9, homotpics);
  }

  @Test
  public void simpleExtinct() throws Exception {
    Tree<?> tree = Tree.simple(resource("extinct.txtree"));

    tree.print(System.out);

    StringWriter buffer = new StringWriter();
    tree.print(buffer);
    assertEquals(IOUtils.toString(resource("extinct.txtree"), "UTF8").trim(), buffer.toString().trim());

    int extinct = 0;
    System.out.println("Tree traversal");
    for (TreeNode<?> n : tree) {
      System.out.println(n.name);
      assertNotNull(n.name);
      assertNotNull(n.rank);
      if (n.extinct) extinct++;
    }
    assertEquals(8, extinct);
  }

  @Test
  public void oldStyleSynonyms() throws Exception {
    Tree<?> tree = Tree.simple(resource("oldstyle.txtree"));

    tree.print(System.out);

    StringWriter buffer = new StringWriter();
    tree.print(buffer);
    // * replaced with =
    assertEquals(IOUtils.toString(resource("oldstyle.txtree"), "UTF8").trim()
            .replaceAll("\\*", "="), buffer.toString().trim());
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
      assertNotNull(n.rank);
      assertNotNull(n.parsedName);
      System.out.println(n.parsedName.canonicalNameComplete()+"\n");
    }
  }

  @Test
  public void testVerify() throws Exception {
    assertTrue(Tree.verify(resource("test.txt")));
    assertTrue(Tree.verify(resource("test2.txt")));
    assertTrue(Tree.verify(resource("test-ranks.txt")));

    assertFalse(Tree.verify(resource("badtree.txt")));
    assertFalse(Tree.verify(resource("notree.txt")));
    assertFalse(Tree.verify(resource("notree2.txt")));
    assertFalse(Tree.verify(resource("dwca.txt")));
  }

  @Test
  public void infos() throws Exception {
    Tree<?> tree = Tree.simple(resource("test3.txt"));

    tree.print(System.out);

    StringWriter buffer = new StringWriter();
    tree.print(buffer);
    assertEquals(IOUtils.toString(resource("test3clean.txt"), "UTF8").trim(), buffer.toString().trim());

    System.out.println("Tree traversal");
    for (TreeNode<?> n : tree) {
      assertNotNull(n.name);
    }
  }
  static InputStream resource(String resourceName) {
    return ClassLoader.getSystemResourceAsStream(resourceName);
  }

}