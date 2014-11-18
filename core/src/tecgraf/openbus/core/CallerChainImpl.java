package tecgraf.openbus.core;

import java.util.Arrays;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.core.v2_0.credential.SignedCallChain;
import tecgraf.openbus.core.v2_0.services.access_control.CallChain;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;

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
  private SignedCallChain signedChain;

  /**
   * Indicador de tipo interno desta cadeia
   */
  private ChainType type;

  /**
   * Construtor,
   * 
   * @param busid identificador do barramento.
   * @param chain a cadeia de chamadas.
   * @param signedChain a representação assinada da mesma cadeia.
   */
  public CallerChainImpl(String busid, CallChain chain,
    SignedCallChain signedChain) {
    this(busid, chain.target, chain.caller, chain.originators, signedChain);
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
    LoginInfo[] originators, SignedCallChain signedChain) {
    this.busid = busid;
    this.target = target;
    this.caller = caller;
    this.originators = originators;
    this.signedChain = signedChain;
    if (!Arrays.equals(LegacySupport.LEGACY_ENCRYPTED_BLOCK,
      signedChain.signature)) {
      this.type = ChainType.CHAIN_2_0;
    }
    else {
      this.type = ChainType.CHAIN_1_5;
    }
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
  protected SignedCallChain signedCallChain() {
    return this.signedChain;
  }

  /**
   * Recupera a informação de tipo interno da Cadeia
   * 
   * @return o tipo interno da cadeia
   */
  protected ChainType type() {
    return type;
  }

  /**
   * Definição interna de tipo de cadeia
   *
   * @author Tecgraf/PUC-Rio
   */
  static enum ChainType {
    /** Cadeia 2.0 */
    CHAIN_2_0,
    /** Cadeia Legada 1.5 */
    CHAIN_1_5;
  }
}
