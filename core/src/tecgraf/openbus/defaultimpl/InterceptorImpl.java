package tecgraf.openbus.defaultimpl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.omg.CORBA.LocalObject;
import org.omg.PortableInterceptor.Interceptor;

abstract class InterceptorImpl extends LocalObject implements Interceptor {
  protected static final byte BUS_MAJOR_VERSION = 2;
  protected static final byte BUS_MINOR_VERSION = 0;

  private String name;
  private ORBMediator mediator;

  protected InterceptorImpl(String name, ORBMediator mediator) {
    this.name = name;
    this.mediator = mediator;
  }

  @Override
  public String name() {
    return this.name;
  }

  protected final ORBMediator getMediator() {
    return this.mediator;
  }

  @Override
  public void destroy() {
  }
}
