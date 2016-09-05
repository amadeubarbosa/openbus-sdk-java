package tecgraf.openbus;

import tecgraf.openbus.core.AuthArgs;

/**
 * Interface que define um m�todo para a obten��o dos dados de autentica��o
 * necess�rios.
 */
public interface LoginCallback {
  /**
   * M�todo a ser chamado pela biblioteca de acesso OpenBus quando necessitar
   * dos dados para uma nova autentica��o.
   *
   * @return Dados v�lidos para uma autentica��o.
   */
  AuthArgs authenticationArguments();
}
