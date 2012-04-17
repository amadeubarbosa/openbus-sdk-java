package tecgraf.openbus.core;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.core.v2_00.credential.SignedCallChain;
import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;

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
   * Informa��o dos participantes desta cadeia de chamadas.
   */
  private LoginInfo[] callers;
  /**
   * A cadeia assinada desta {@link CallerChain}
   */
  private SignedCallChain signedChain;

  /**
   * Constutor.
   * 
   * @param busid identificador do barramento.
   * @param callers os participantes da cadeia.
   * @param signedChain a representa��o assinada da cadeia.
   */
  CallerChainImpl(String busid, LoginInfo[] callers, SignedCallChain signedChain) {
    this.busid = busid;
    this.callers = callers;
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
  public LoginInfo[] callers() {
    return this.callers;
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
