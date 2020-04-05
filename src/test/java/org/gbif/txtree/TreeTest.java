package org.gbif.txtree;

import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.gbif.nameparser.api.Rank;
import org.junit.Test;

import java.io.InputStream;
import java.io.StringWriter;

import static org.junit.Assert.*;


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

  static InputStream resource(String resourceName) {
    return ClassLoader.getSystemResourceAsStream(resourceName);
  }

}