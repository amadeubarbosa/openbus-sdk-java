package tecgraf.openbus.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.omg.PortableInterceptor.ORBInitializer;

public final class ORBInitializerInfoTest {
  @Test(expected = NullPointerException.class)
  public void createWithNullInitializer() {
    new ORBInitializerInfo(null);
  }

  @Test(expected = NullPointerException.class)
  public void createWithNullInitializerAndId() {
    new ORBInitializerInfo(null, "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void createWithInitializerAndNullId() {
    new ORBInitializerInfo(ORBInitializer.class, null);
  }

  @Test
  public void createWithInitializer() {
    ORBInitializerInfo info = new ORBInitializerInfo(ORBInitializer.class);
    assertNotNull(info.getId());
    assertNotNull(info.getClassName());
    assertEquals(info.getId(), info.getClassName());
    assertEquals(info.getClassName(), ORBInitializer.class.getName());
  }

  @Test
  public void createWithInitializerAndId() {
    String id = "initializer";
    ORBInitializerInfo info = new ORBInitializerInfo(ORBInitializer.class, id);
    assertNotNull(info.getId());
    assertNotNull(info.getClassName());
    assertTrue(!info.getId().equals(info.getClassName()));
    assertEquals(id, info.getId());
  }
}
