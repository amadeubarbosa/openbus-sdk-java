package openbus;

import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
  public static Test suite() {
    TestSuite suite = new TestSuite("Tests for openbus");
    suite.addTest(new JUnit4TestAdapter(OpenbusInitializeTest.class));
    suite.addTest(new JUnit4TestAdapter(OpenbusTest.class));
    return suite;
  }
}
