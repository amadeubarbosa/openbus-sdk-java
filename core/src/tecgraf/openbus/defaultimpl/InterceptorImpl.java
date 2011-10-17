package tecgraf.openbus.defaultimpl;

import org.omg.CORBA.LocalObject;
import org.omg.IOP.Codec;
import org.omg.PortableInterceptor.Interceptor;

abstract class InterceptorImpl extends LocalObject implements Interceptor {
  protected static final int CONTEXT_ID = 0x42555300;
  private String name;
  private ORBMediator mediator;
  private Codec codec;

  protected InterceptorImpl(String name, ORBMediator mediator, Codec codec) {
    this.name = name;
    this.mediator = mediator;
    this.codec = codec;
  }

  @Override
  public String name() {
    return this.name;
  }

  protected final ORBMediator getMediator() {
    return this.mediator;
  }

  protected final Codec getCodec() {
    return this.codec;
  }

  @Override
  public void destroy() {
  }
}
