package tecgraf.openbus.core;

import tecgraf.openbus.SharedAuthSecret;
import tecgraf.openbus.core.v2_1.services.access_control.LoginProcess;

/**
 * Implementação da classe que representa o segredo para compartilhamento de uma
 * autenticação.
 *
 * @author Tecgraf/PUC-Rio
 */
class SharedAuthSecretImpl implements SharedAuthSecret {

  /** Identificador do barramento */
  final private String busid;
  /** A tentativa de login */
  final private LoginProcess attempt;
  /** O segredo */
  final private byte[] secret;
  /** Contexto associado */
  private OpenBusContextImpl context;

  /**
   * Construtor.
   * 
   * @param busId identificador de barramento
   * @param attempt tentativa de login
   * @param secret segredo
   * @param context contexto associado
   */
  public SharedAuthSecretImpl(String busId, LoginProcess attempt,
    byte[] secret, OpenBusContextImpl context) {
    this.busid = busId;
    this.attempt = attempt;
    this.secret = secret;
    this.context = context;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String busid() {
    return busid;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void cancel() {
    context.ignoreThread();
    try {
      attempt.cancel();
    }
    finally {
      context.unignoreThread();
    }
  }

  /**
   * Recupera o segredo associado.
   * 
   * @return o segredo.
   */
  byte[] secret() {
    return secret;
  }

  /**
   * Recupera a tentativa de login associada.
   * 
   * @return a tentativa de login.
   */
  LoginProcess attempt() {
    return attempt;
  }
}
