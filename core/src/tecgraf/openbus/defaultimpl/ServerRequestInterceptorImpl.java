package tecgraf.openbus.defaultimpl;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.Any;
import org.omg.CORBA.INTERNAL;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.IOP.CodecPackage.TypeMismatch;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

import tecgraf.openbus.core.v2_00.credential.CredentialContextId;
import tecgraf.openbus.core.v2_00.services.access_control.LoginInfoSeqHelper;

public final class ServerRequestInterceptorImpl extends InterceptorImpl
  implements ServerRequestInterceptor {
  private static final Logger logger = Logger
    .getLogger(ServerRequestInterceptorImpl.class.getName());

  private int credentialSlotId;

  ServerRequestInterceptorImpl(String name, ORBMediator mediator,
    int credentialSlotId) {
    super(name, mediator);
    this.credentialSlotId = credentialSlotId;
  }

  @Override
  public void receive_request_service_contexts(ServerRequestInfo ri) {
    ServiceContext requestServiceContext =
      ri.get_request_service_context(CredentialContextId.value);
    byte[] encodedCredential = requestServiceContext.context_data;
    Any any;
    try {
      any =
        this.getMediator().getCodec().decode_value(encodedCredential,
          LoginInfoSeqHelper.type());
    }
    catch (TypeMismatch e) {
      String message = "Falha inesperada ao decodificar a credencial";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
    catch (FormatMismatch e) {
      String message = "Falha inesperada ao decodificar a credencial";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
    try {
      ri.set_slot(this.credentialSlotId, any);
    }
    catch (InvalidSlot e) {
      String message = "Falha inesperada ao armazenar a credencial em seu slot";
      logger.log(Level.SEVERE, message, e);
      throw new INTERNAL(message);
    }
  }

  @Override
  public void receive_request(ServerRequestInfo ri) {
    // TODO Auto-generated method stub

  }

  @Override
  public void send_reply(ServerRequestInfo ri) {
  }

  @Override
  public void send_exception(ServerRequestInfo ri) {
  }

  @Override
  public void send_other(ServerRequestInfo ri) {
  }
}
