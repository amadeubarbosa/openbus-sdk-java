package tecgraf.openbus.core;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.core.v2_0.credential.SignedCallChain;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;

/**
 * Implementa��o do {@link CallerChain}
 * 
 * @author Tecgraf
 */
final class CallerChainImpl implements CallerChain {
  /**
   * Identificador do barramento.
   */
  private String busid;
  /**
   * Lista de informa��es de login de todas as entidades que realizaram chamadas
   * que originaram a cadeia de chamadas da qual essa chamada est� inclusa.
   * Quando essa lista � vazia isso indica que a chamada n�o est� inclusa numa
   * cadeia de chamadas.
   */
  private LoginInfo[] originators;

  /**
   * Informa��o de login da entidade que iniciou a chamada.
   */
  private LoginInfo caller;
  /**
   * A cadeia assinada desta {@link CallerChain}
   */
  private SignedCallChain signedChain;

  /**
   * Constutor.
   * 
   * @param busid identificador do barramento.
   * @param caller Informa��o de login da entidade que iniciou a chamada.
   * @param originators Lista de informa��es de login de todas as entidades que
   *        realizaram chamadas que originaram a cadeia de chamadas da qual essa
   *        chamada est� inclusa.
   * @param signedChain a representa��o assinada da cadeia.
   */
  CallerChainImpl(String busid, LoginInfo caller, LoginInfo[] originators,
    SignedCallChain signedChain) {
    this.busid = busid;
    this.caller = caller;
    this.originators = originators;
    this.signedChain = signedChain;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String busid() {
    return this.busid;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LoginInfo[] originators() {
    return this.originators;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LoginInfo caller() {
    return this.caller;
  }

  /**
   * Recupera a representa��o assinada da cadeia.
   * 
   * @return a cadeia assinada.
   */
  protected SignedCallChain signedCallChain() {
    return this.signedChain;
  }
}
