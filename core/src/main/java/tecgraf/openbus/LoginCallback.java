package tecgraf.openbus;

import tecgraf.openbus.core.AuthArgs;

/**
 * Interface que define um método para a obtenção dos dados de autenticação
 * necessários.
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
