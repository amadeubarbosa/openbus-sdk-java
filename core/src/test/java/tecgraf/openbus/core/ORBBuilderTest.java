package tecgraf.openbus.core;

import java.util.Properties;

import org.junit.Test;

import tecgraf.openbus.core.ORBBuilder;

public final class ORBBuilderTest {
  @Test
  public void create() {
    new ORBBuilder();
  }

  @Test
  public void createWithArgs() {
    String[] args = new String[0];
    new ORBBuilder(args);
  }

  @Test
  public void createWithNullArgs() {
    Properties props = new Properties();
    new ORBBuilder(null, props);
  }

  @Test
  public void createWithProperties() {
    Properties props = new Properties();
    new ORBBuilder(props);
  }

  @Test
  public void createWithNullProperties() {
    String[] args = new String[0];
    new ORBBuilder(args, null);
  }

  @Test
  public void createWithNullArgsAndNullProperties() {
    new ORBBuilder(null, null);
  }

  @Test
  public void createWithArgsAndProperties() {
    String[] args = new String[0];
    Properties props = new Properties();
    new ORBBuilder(args, props);
  }
}
