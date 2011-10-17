package tecgraf.openbus.defaultimpl;

import junit.framework.Assert;

import org.junit.Test;
import org.omg.PortableInterceptor.ORBInitializer;

import tecgraf.openbus.defaultimpl.ORBInitializerInfo;

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
    Assert.assertNotNull(info.getId());
    Assert.assertNotNull(info.getClassName());
    Assert.assertEquals(info.getId(), info.getClassName());
    Assert.assertEquals(info.getClassName(), ORBInitializer.class.getName());
  }

  @Test
  public void createWithInitializerAndId() {
    String id = "initializer";
    ORBInitializerInfo info = new ORBInitializerInfo(ORBInitializer.class, id);
    Assert.assertNotNull(info.getId());
    Assert.assertNotNull(info.getClassName());
    Assert.assertTrue(!info.getId().equals(info.getClassName()));
    Assert.assertEquals(id, info.getId());
  }
}
