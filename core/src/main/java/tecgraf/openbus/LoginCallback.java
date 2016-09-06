package tecgraf.openbus;

import tecgraf.openbus.core.AuthArgs;

/**
 * Interface para a obtenção de dados de autenticação.
 */
public interface LoginCallback {
  /**
   * Método a ser chamado pela biblioteca de acesso OpenBus quando necessitar
   * dos dados para uma nova autenticação.
   *
   * @return Dados válidos para uma autenticação.
   */
  AuthArgs authenticationArguments();
}
