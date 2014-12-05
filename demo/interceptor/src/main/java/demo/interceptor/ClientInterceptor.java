package demo.interceptor;

import org.omg.CORBA.Any;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.TCKind;
import org.omg.IOP.Codec;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.InvalidSlot;

/**
 * Interceptador cliente para inclusão de informação extra no contexto.
 * 
 * @author Tecgraf
 */
public class ClientInterceptor extends LocalObject implements
  ClientRequestInterceptor {

  /**
   * Referência para o {@link ContextInspector} que possui dados necessários
   * para o interceptador.
   */
  ContextInspector inspector;

  /**
   * Construtor.
   * 
   * @param contextInspector o inspetor de contexto
   */
  public ClientInterceptor(ContextInspector contextInspector) {
    this.inspector = contextInspector;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void receive_exception(ClientRequestInfo arg0) throws ForwardRequest {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void receive_other(ClientRequestInfo arg0) throws ForwardRequest {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void receive_reply(ClientRequestInfo arg0) {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send_poll(ClientRequestInfo arg0) {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send_request(ClientRequestInfo info) throws ForwardRequest {
    int slotId = inspector.getMySlotId();
    Codec codec = inspector.getCodec();
    int contextId = inspector.getContextId();
    try {
      Any any = info.get_slot(slotId);
      if (any.type().kind().value() == TCKind._tk_null) {
        // se não existe informação a ser enviada pelo contexto 
        return;
      }
      byte[] encodedData = codec.encode(any);
      ServiceContext legacyServiceContext =
        new ServiceContext(contextId, encodedData);
      info.add_request_service_context(legacyServiceContext, false);
    }
    catch (InvalidSlot e) {
      throw new INTERNAL("Erro inesperado ao acessar slot.", 0,
        CompletionStatus.COMPLETED_NO);
    }
    catch (InvalidTypeForEncoding e) {
      throw new INTERNAL("Erro inesperado ao codificar dado.", 0,
        CompletionStatus.COMPLETED_NO);
    }
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
