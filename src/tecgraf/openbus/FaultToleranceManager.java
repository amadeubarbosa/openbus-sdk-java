package tecgraf.openbus;

import java.util.ArrayList;

import tecgraf.openbus.util.Host;
import tecgraf.openbus.util.Log;
import tecgraf.openbus.util.PropertiesLoaderImpl;

public class FaultToleranceManager {

  /**
   * A lista de máquinas e portas que contem uma réplica rodando.
   */
  private ArrayList<Host> acsHosts;

  /**
   * A máquina que contem a réplica que está sendo usada.
   */
  private Host acsHostInUse;

  private int currIndex = 0;

  private static FaultToleranceManager ftManager;

  private int trials = 1;
  private int currTrial = 0;

  private FaultToleranceManager() {
    this.acsHosts = new ArrayList<Host>();
    setHosts();
  }

  public static FaultToleranceManager getInstance() {
    if (ftManager == null)
      ftManager = new FaultToleranceManager();
    return ftManager;
  }

  /**
   * Popula a lista de hosts que contem as réplicas do Serviço Tolerante a
   * Falhas.
   * 
   */
  private void setHosts() {

    if (this.acsHosts == null)
      this.acsHosts = new ArrayList<Host>();
    // Todos os hosts das replicas
    String[] hostsStr = (PropertiesLoaderImpl.getValor("hosts")).split(",");
    for (int i = 0; i < hostsStr.length; i++) {
      // para cada host de replica
      String[] hostStr = hostsStr[i].split(":");
      String name = hostStr[0];
      int port = Integer.valueOf(hostStr[1]);
      this.acsHosts.add(new Host(name, port));
    }

    this.acsHostInUse = this.acsHosts.get(currIndex);

    String _trials = PropertiesLoaderImpl.getValor("trials");
    if (_trials.length() > 0) {
      this.trials = Integer.valueOf(_trials);
    }
  }

  public ArrayList<Host> getHosts() {
    return acsHosts;
  }

  public void setHosts(ArrayList<Host> hosts) {
    this.acsHosts = hosts;
  }

  public Host getACSHostInUse() {
    return acsHostInUse;
  }

  public void setACSHostInUse(Host hostInUse) {
    this.acsHostInUse = hostInUse;
  }

  /**
   * No caso de uma falha de réplica, este método deve ser chamado para
   * atualizar a máquina a ser obtida uma réplica.
   */
  public boolean updateACSHostInUse() {
    Log.COMMON.finest("currTrial: " + currTrial);
    if (currTrial == trials) {
      return false;
    }

    // Se a maquina em uso eh a ultima da lista
    if (currIndex == this.acsHosts.size() - 1) {
      // eu pego a primeira
      currIndex = 0;
      currTrial += 1;
    }
    else {
      currIndex += 1;
    }
    this.acsHostInUse = this.acsHosts.get(currIndex);
    return true;
  }

  /**
  * Quando uma replica for encontrada antes de acabar um trial, é necessário resetar esta variável.
  * Caso contrário, o Manager poderá nunca encontrar réplicas válidas se estiver no último trial
  * e tiver havia uma falha e o index for maior que 0 e todas as replicas de hosts indexadas
  * acima da corrente não estiverem disponíveis
  */
  public void resetCurrTrial() {
		this.currTrial = 0;
  }

}
