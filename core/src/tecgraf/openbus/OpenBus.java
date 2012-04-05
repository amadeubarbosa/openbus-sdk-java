package tecgraf.openbus;

import java.util.Properties;

/**
 * Representa o ponto de entrada para o uso do SDK.
 * 
 * @author Tecgraf
 */
public interface OpenBus {

  /**
   * Inicializa um ORB para ser usado na conexão com um barramento OpenBus.
   * Todos os acessos a serviços e objetos em um barramento devem ser feitos
   * pelo ORB usado na obtenção do barramento.
   * 
   * @return ORB iniciado.
   */
  public BusORB initORB();

  /**
   * Inicializa um ORB para ser usado na conexão com um barramento OpenBus.
   * Todos os acessos a serviços e objetos em um barramento devem ser feitos
   * pelo ORB usado na obtenção do barramento.
   * 
   * @param args Parâmetros usados na inicialização do ORB.
   * 
   * @return ORB iniciado.
   */
  public BusORB initORB(String[] args);

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
  public BusORB initORB(String[] args, Properties props);

  /**
   * Cria uma conexão para um barramento a partir de um endereço de rede IP e
   * uma porta.
   * 
   * @param host Endereço de rede IP onde o barramento está executando.
   * @param port Porta do processo do barramento no endereço indicado.
   * 
   * @return Conexão ao barramento referenciado.
   */
  public Connection connect(String host, int port);

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
  public Connection connect(String host, int port, BusORB orb);

}
