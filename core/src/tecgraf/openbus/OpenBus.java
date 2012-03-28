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
   * Inicializa um ORB para ser usado na conexão com um barramento OpenBus.
   * Todos os acessos a serviços e objetos em um barramento devem ser feitos
   * pelo ORB usado na obtenção do barramento.
   * 
   * @return ORB iniciado.
   */
  public BusORB initORB() {
    return this.initORB(null, null);
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
  public BusORB initORB(String[] args) {
    return this.initORB(args, null);
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
  public BusORB initORB(String[] args, Properties props) {
    return new BusORBImpl(args, props);
  }

  /**
   * Cria uma conexão para um barramento a partir de um endereço de rede IP e
   * uma porta.
   * 
   * @param host Endereço de rede IP onde o barramento está executando.
   * @param port Porta do processo do barramento no endereço indicado.
   * 
   * @return Conexão ao barramento referenciado.
   */
  public Connection connect(String host, int port) {
    return connect(host, port, initORB());
  }

  /**
   * Cria uma conexão para um barramento a partir de um endereço de rede IP e
   * uma porta.
   * 
   * @param host Endereço de rede IP onde o barramento está executando.
   * @param port Porta do processo do barramento no endereço indicado.
   * @param orb ORB a ser utilizado na criação da referência. Se o valor desse
   *        parâmetro for 'null' um ORB é inicializado com configurações default
   *        a ser utilizado na criação da referência.
   * 
   * @return Conexão ao barramento referenciado.
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
