package tecgraf.openbus;

import java.util.Properties;

import scs.core.IComponent;
import scs.core.IComponentHelper;
import tecgraf.openbus.core.BusInfo;
import tecgraf.openbus.core.BusORBImpl;
import tecgraf.openbus.core.ConnectionImpl;
import tecgraf.openbus.core.v2_00.BusObjectKey;

/**
 * Representa o ponto de entrada para o uso do SDK.
 * 
 * @author Tecgraf
 */
public abstract class OpenBus {

  /**
   * Inicializa um ORB para ser usado na conex�o com um barramento OpenBus.
   * Todos os acessos a servi�os e objetos em um barramento devem ser feitos
   * pelo ORB usado na obten��o do barramento.
   * 
   * @return ORB iniciado.
   */
  public BusORB initORB() {
    return this.initORB(null, null);
  }

  /**
   * Inicializa um ORB para ser usado na conex�o com um barramento OpenBus.
   * Todos os acessos a servi�os e objetos em um barramento devem ser feitos
   * pelo ORB usado na obten��o do barramento.
   * 
   * @param args Par�metros usados na inicializa��o do ORB.
   * 
   * @return ORB iniciado.
   */
  public BusORB initORB(String[] args) {
    return this.initORB(args, null);
  }

  /**
   * Inicializa um ORB para ser usado na conex�o com um barramento OpenBus.
   * Todos os acessos a servi�os e objetos em um barramento devem ser feitos
   * pelo ORB usado na obten��o do barramento.
   * 
   * @param args Par�metros usados na inicializa��o do ORB.
   * @param props Propriedades usadas na inicializa��o do ORB.
   * 
   * @return ORB iniciado.
   */
  public BusORB initORB(String[] args, Properties props) {
    return new BusORBImpl(args, props);
  }

  /**
   * Cria uma conex�o para um barramento a partir de um endere�o de rede IP e
   * uma porta.
   * 
   * @param host Endere�o de rede IP onde o barramento est� executando.
   * @param port Porta do processo do barramento no endere�o indicado.
   * 
   * @return Conex�o ao barramento referenciado.
   */
  public Connection connect(String host, int port) {
    return connect(host, port, initORB());
  }

  /**
   * Cria uma conex�o para um barramento a partir de um endere�o de rede IP e
   * uma porta.
   * 
   * @param host Endere�o de rede IP onde o barramento est� executando.
   * @param port Porta do processo do barramento no endere�o indicado.
   * @param orb ORB a ser utilizado na cria��o da refer�ncia. Se o valor desse
   *        par�metro for 'null' um ORB � inicializado com configura��es default
   *        a ser utilizado na cria��o da refer�ncia.
   * 
   * @return Conex�o ao barramento referenciado.
   */
  public Connection connect(String host, int port, BusORB orb) {
    ((BusORBImpl) orb).ignoreCurrentThread();
    try {
      String str =
        String.format("corbaloc::1.0@%s:%d/%s", host, port, BusObjectKey.value);
      org.omg.CORBA.Object obj = orb.getORB().string_to_object(str);
      if (obj == null) {
        return null;
      }
      IComponent component = IComponentHelper.narrow(obj);
      BusInfo bus = new BusInfo(component);
      Connection conn = new ConnectionImpl(bus, orb);
      return conn;
    }
    finally {
      ((BusORBImpl) orb).unignoreCurrentThread();
    }
  }

}
