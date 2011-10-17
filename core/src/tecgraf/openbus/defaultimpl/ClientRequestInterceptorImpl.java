package tecgraf.openbus.defaultimpl;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.Any;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.SystemException;
import org.omg.IOP.Codec;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ClientRequestInterceptor;

import tecgraf.openbus.BusORB;
import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.core.v2_00.services.access_control.Credential;
import tecgraf.openbus.core.v2_00.services.access_control.CredentialHelper;
import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;

public final class ClientRequestInterceptorImpl extends InterceptorImpl
  implements ClientRequestInterceptor {
  private static final Logger logger = Logger
    .getLogger(ClientRequestInterceptorImpl.class.getName());

  ClientRequestInterceptorImpl(String name, ORBMediator mediator, Codec codec) {
    super(name, mediator, codec);
  }

  @Override
  public void send_request(ClientRequestInfo ri) {
    try {
      String operation = ri.operation();
      logger.finest(String.format("A operação %s será requisitada", operation));
      BusORB orb = this.getMediator().getORB();
      if (orb.isCurrentThreadIgnored()) {
        logger
          .finest(String
            .format(
              "A operação %s não terá uma credencial, pois a thread atual está ignorada",
              operation));
        return;
      }
      if (isCORBAObjectOperation(ri)) {
        logger.finest(String.format(
          "A operação %s não terá uma credencial, pois é uma operação de %s",
          operation, org.omg.CORBA.Object.class.getName()));
        return;
      }
      Credential credential = this.generateCredential();
      Any any = orb.getORB().create_any();
      CredentialHelper.insert(any, credential);
      byte[] encodedCredential;
      try {
        encodedCredential = this.getCodec().encode_value(any);
      }
      catch (InvalidTypeForEncoding e) {
        String message = "Falha inesperada ao codificar a credencial";
        logger.log(Level.SEVERE, message, e);
        throw new INTERNAL(message);
      }
      ServiceContext requestServiceContext =
        new ServiceContext(CONTEXT_ID, encodedCredential);
      ri.add_request_service_context(requestServiceContext, false);
    }
    catch (SystemException e) {
      throw e;
    }
    catch (RuntimeException e) {
      e.printStackTrace();
    }
  }

  private Credential generateCredential() {
    BusORB orb = this.getMediator().getORB();
    Connection currentConnection = orb.getCurrentConnection();
    CallerChain chain = currentConnection.getJoinedChain();
    LoginInfo[] loginInfoSeq;
    if (chain == null) {
      logger.finest("Não há cadeia de chamadas aninhada");
      loginInfoSeq = new LoginInfo[1];
    }
    else {
      logger.finest("Há cadeia de chamadas aninhada");
      LoginInfo[] callers = chain.getCallers();
      loginInfoSeq = new LoginInfo[callers.length + 1];
      for (int i = 0; i < callers.length; i++) {
        loginInfoSeq[i] = callers[i];
      }
    }
    loginInfoSeq[loginInfoSeq.length - 1] = currentConnection.getLogin();
    return new Credential(currentConnection.getBus().getId(), loginInfoSeq);
  }

  private boolean isCORBAObjectOperation(ClientRequestInfo ri) {
    if (ri.operation().startsWith("_")) {
      return true;
    }
    return false;
  }

  @Override
  public void send_poll(ClientRequestInfo ri) {
  }

  @Override
  public void receive_reply(ClientRequestInfo ri) {
  }

  @Override
  public void receive_exception(ClientRequestInfo ri) {
  }

  @Override
  public void receive_other(ClientRequestInfo ri) {
  }
}
