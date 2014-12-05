package demo.interceptor;

import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.LocalObject;
import org.omg.IOP.Codec;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

/**
 * Interceptador servidor para recuperação de informação extra no contexto.
 * 
 * @author Tecgraf
 */
public class ServerInterceptor extends LocalObject implements
  ServerRequestInterceptor {

  /**
   * Referência para o {@link ContextInspector} que possui dados necessários
   * para o interceptador.
   */
  ContextInspector inspector;

  /**
   * Construtor.
   * 
   * @param contextInspector inspetor de contexto.
   */
  public ServerInterceptor(ContextInspector contextInspector) {
    this.inspector = contextInspector;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void receive_request(ServerRequestInfo info) throws ForwardRequest {
    int slotId = inspector.getMySlotId();
    Codec codec = inspector.getCodec();
    int contextId = inspector.getContextId();
    byte[] encodedData;
    try {
      ServiceContext context = info.get_request_service_context(contextId);
      encodedData = context.context_data;
    }
    catch (BAD_PARAM e) {
      switch (e.minor) {
        case 26:
          return;
        default:
          throw e;
      }
    }

    if (encodedData != null) {
      try {
        Any any = codec.decode(encodedData);
        info.set_slot(slotId, any);
      }
      catch (InvalidSlot e) {
        String msg = "Erro inesperado ao acessar slot.";
        System.err.println(msg);
        throw new INTERNAL(msg, 0, CompletionStatus.COMPLETED_NO);
      }
      catch (FormatMismatch e) {
        String msg = "Erro inesperado ao decodificar dado.";
        System.err.println(msg);
        throw new INTERNAL(msg, 0, CompletionStatus.COMPLETED_NO);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void receive_request_service_contexts(ServerRequestInfo info)
    throws ForwardRequest {
    // do nothing
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send_exception(ServerRequestInfo arg0) throws ForwardRequest {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send_other(ServerRequestInfo arg0) throws ForwardRequest {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send_reply(ServerRequestInfo arg0) {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void destroy() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String name() {
    return this.getClass().getSimpleName();
  }

}
