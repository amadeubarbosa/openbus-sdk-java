/*
 * $Id$
 */
package openbus.common.interceptors;

import openbus.common.CredentialManager;

import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactory;
import org.omg.IOP.CodecFactoryHelper;
import org.omg.IOP.Encoding;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitializer;

/**
 * Essa classe é responsável pelo procedimento de inicialização do servidor
 * associada à inicialização do ORB.
 * 
 * @author Tecgraf/PUC-Rio
 */
public class ServerInitializer extends org.omg.CORBA.LocalObject implements
  ORBInitializer {

  /**
   * Constrói o inicializador do servidor.
   */
  public ServerInitializer() {
  }

  /**
   * {@inheritDoc} <br>
   * Registra o interceptador de requisições de serviço do servidor
   */
  public void post_init(ORBInitInfo info) {
    try {
      /* Instala o interceptador */
      CodecFactory codecFactory = CodecFactoryHelper.narrow(info
        .resolve_initial_references("CodecFactory"));
      Encoding encoding = new Encoding((short) 0, (byte) 1, (byte) 2);
      Codec codec = codecFactory.create_codec(encoding);

      info.add_server_request_interceptor(new ServerInterceptor(codec));
      System.out.println("REGISTREI INTERCEPTADOR SERVIDOR!");

      /* Aloca um slot para transporte da credencial */
      int credentialSlot = info.allocate_slot_id();
      CredentialManager.getInstance().setCredentialSlot(credentialSlot);
    }
    catch (org.omg.PortableInterceptor.ORBInitInfoPackage.InvalidName in) {
      System.out.println("ERRO NO REGISTRO DO INTERCEPTADOR SERVIDOR!");
      in.printStackTrace();
    }
    catch (org.omg.IOP.CodecFactoryPackage.UnknownEncoding ue) {
      System.out.println("ERRO NO REGISTRO DO INTERCEPTADOR SERVIDOR!");
      ue.printStackTrace();
    }
    catch (org.omg.PortableInterceptor.ORBInitInfoPackage.DuplicateName e) {
      System.out.println("ERRO NO REGISTRO DO INTERCEPTADOR SERVIDOR!");
    }
  }

  /**
   * {@inheritDoc}
   */
  public void pre_init(ORBInitInfo info) {
  }
}
