package tecgraf.openbus.core;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.core.v2_1.credential.SignedData;
import tecgraf.openbus.core.v2_1.services.access_control.CallChain;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;

/**
 * Implementação do {@link CallerChain}
 * 
 * @author Tecgraf
 */
final class CallerChainImpl implements CallerChain {
  /**
   * Identificador do barramento.
   */
  private String busid;

  /**
   * Entidade para o qual a chamada esta destinada.
   */
  private String target;

  /**
   * Lista de informações de login de todas as entidades que realizaram chamadas
   * que originaram a cadeia de chamadas da qual essa chamada está inclusa.
   * Quando essa lista é vazia isso indica que a chamada não está inclusa numa
   * cadeia de chamadas.
   */
  private LoginInfo[] originators;

  /**
   * Informação de login da entidade que iniciou a chamada.
   */
  private LoginInfo caller;
  /**
   * A cadeia assinada desta {@link CallerChain}
   */
  private SignedData signedChain;

  /**
   * Construtor
   * 
   * @param chain a cadeia de chamadas.
   * @param signedChain a representação assinada da mesma cadeia.
   */
  public CallerChainImpl(CallChain chain, SignedData signedChain) {
    this(chain.bus, chain.target, chain.caller, chain.originators, signedChain);
  }

  /**
   * Constutor.
   * 
   * @param busid identificador do barramento.
   * @param target entidade para o qual a chamada esta destinada.
   * @param caller Informação de login da entidade que iniciou a chamada.
   * @param originators Lista de informações de login de todas as entidades que
   *        realizaram chamadas que originaram a cadeia de chamadas da qual essa
   *        chamada está inclusa.
   * @param signedChain a representação assinada da cadeia.
   */
  CallerChainImpl(String busid, String target, LoginInfo caller,
    LoginInfo[] originators, SignedData signedChain) {
    this.busid = busid;
    this.target = target;
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
  public String target() {
    return this.target;
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
   * Recupera a representação assinada da cadeia.
   * 
   * @return a cadeia assinada.
   */
  protected SignedData signedCallChain() {
    return this.signedChain;
  }
}
