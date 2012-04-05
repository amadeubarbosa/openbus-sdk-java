package tecgraf.openbus;

import java.util.Properties;

/**
 * Representa o ponto de entrada para o uso do SDK.
 * 
 * @author Tecgraf
 */
public interface OpenBus {

  /**
   * Inicializa um ORB para ser usado na conex�o com um barramento OpenBus.
   * Todos os acessos a servi�os e objetos em um barramento devem ser feitos
   * pelo ORB usado na obten��o do barramento.
   * 
   * @return ORB iniciado.
   */
  public BusORB initORB();

  /**
   * Inicializa um ORB para ser usado na conex�o com um barramento OpenBus.
   * Todos os acessos a servi�os e objetos em um barramento devem ser feitos
   * pelo ORB usado na obten��o do barramento.
   * 
   * @param args Par�metros usados na inicializa��o do ORB.
   * 
   * @return ORB iniciado.
   */
  public BusORB initORB(String[] args);

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
  public BusORB initORB(String[] args, Properties props);

  /**
   * Cria uma conex�o para um barramento a partir de um endere�o de rede IP e
   * uma porta.
   * 
   * @param host Endere�o de rede IP onde o barramento est� executando.
   * @param port Porta do processo do barramento no endere�o indicado.
   * 
   * @return Conex�o ao barramento referenciado.
   */
  public Connection connect(String host, int port);

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
  public Connection connect(String host, int port, BusORB orb);

}
