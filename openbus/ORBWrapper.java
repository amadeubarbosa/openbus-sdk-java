/*
 * $Id$
 */
package openbus;

import java.util.Properties;

import openbus.common.interceptors.ClientInitializer;
import openbus.common.interceptors.ServerInitializer;

import org.omg.CORBA.UserException;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManager;

/**
 * Encapsula um ORB que deve ser usado para acesso ao OpenBus.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class ORBWrapper {
  /**
   * O prefixo do nome da propriedade do(s) inicializador(es) do ORB.
   */
  private static final String ORB_INITIALIZER_PROPERTY_NAME_PREFIX = "org.omg.PortableInterceptor.ORBInitializerClass.";
  /**
   * O ORB real, encapsulado por este objeto.
   */
  private org.omg.CORBA.ORB orb;
  /**
   * O RootPOA.
   */
  private POA rootPOA;

  /**
   * Cria um objeto que encapsula um ORB.
   */
  public ORBWrapper() {
    this(new Properties());
  }

  /**
   * Cria um objeto que encapsula um ORB.
   * 
   * @param properties As propriedades para a criação do ORB real.
   */
  public ORBWrapper(Properties properties) {
    Properties props = new Properties(properties);
    String clientInitializerClassName = ClientInitializer.class.getName();
    props.put(
      ORB_INITIALIZER_PROPERTY_NAME_PREFIX + clientInitializerClassName,
      clientInitializerClassName);
    String serverInitializerClassName = ServerInitializer.class.getName();
    props.put(
      ORB_INITIALIZER_PROPERTY_NAME_PREFIX + serverInitializerClassName,
      serverInitializerClassName);
    this.orb = org.omg.CORBA.ORB.init((String[]) null, props);
  }

  /**
   * Obtém o ORB real.
   * 
   * @return O ORB real.
   */
  public org.omg.CORBA.ORB getORB() {
    return this.orb;
  }

  /**
   * Obtém o RootPOA.
   * 
   * <p>
   * OBS: A chamada a este método ativa o POAManager.
   * 
   * @return O RootPOA.
   * 
   * @throws UserException Caso ocorra algum erro ao obter o RootPOA.
   */
  public POA getRootPOA() throws UserException {
    if (this.rootPOA == null) {
      org.omg.CORBA.Object obj = this.orb.resolve_initial_references("RootPOA");
      this.rootPOA = POAHelper.narrow(obj);
      POAManager manager = this.rootPOA.the_POAManager();
      manager.activate();
    }
    return this.rootPOA;
  }

  /**
   * Executa o ORB.
   */
  public void run() {
    this.orb.run();
  }

  /**
   * Finaliza a execução do ORB.
   */
  public void finish() {
    this.orb.shutdown(true);
    this.orb.destroy();
  }
}
