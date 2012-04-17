package tecgraf.openbus.core;

import java.util.Properties;

import org.omg.CORBA.ORB;

/**
 * Representa o ponto de entrada para o uso do SDK.
 * 
 * @author Tecgraf
 */
public class ORBInitializer {

  /**
   * Inicializa um ORB para ser usado na conexão com um barramento OpenBus.
   * Todos os acessos a serviços e objetos em um barramento devem ser feitos
   * pelo ORB usado na obtenção do barramento.
   * 
   * @return ORB iniciado.
   */
  public static ORB initORB() {
    return ORBInitializer.initORB(null, null);
  }

  /**
   * Inicializa um ORB para ser usado na conexão com um barramento OpenBus.
   * Todos os acessos a serviços e objetos em um barramento devem ser feitos
   * pelo ORB usado na obtenção do barramento.
   * 
   * @param args Parâmetros usados na inicialização do ORB.
   * 
   * @return ORB iniciado.
   */
  public static ORB initORB(String[] args) {
    return ORBInitializer.initORB(args, null);
  }

  /**
   * Inicializa um ORB para ser usado na conexão com um barramento OpenBus.
   * Todos os acessos a serviços e objetos em um barramento devem ser feitos
   * pelo ORB usado na obtenção do barramento.
   * 
   * @param args Parâmetros usados na inicialização do ORB.
   * @param props Propriedades usadas na inicialização do ORB.
   * 
   * @return ORB iniciado.
   */
  public static ORB initORB(String[] args, Properties props) {
    ORB orb = createORB(args, props);
    ORBMediator mediator = ORBUtils.getMediator(orb);
    mediator.setORB(orb);
    ConnectionManagerImpl manager = ORBUtils.getConnectionManager(orb);
    manager.setORB(orb);
    return orb;
  }

  /**
   * Cria o ORB.
   * 
   * @param args argumentos
   * @param props propriedades
   * @return o ORB
   */
  private static ORB createORB(String[] args, Properties props) {
    ORBBuilder orbBuilder = new ORBBuilder(args, props);
    orbBuilder.addInitializer(new ORBInitializerInfo(ORBInitializerImpl.class));
    return orbBuilder.build();
  }

}
