package tecgraf.openbus;

import tecgraf.openbus.core.AuthArgs;

/**
 * Interface para a obten��o de dados de autentica��o.
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
