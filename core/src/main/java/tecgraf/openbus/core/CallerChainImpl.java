package tecgraf.openbus.core;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.core.Credential.Chain;
import tecgraf.openbus.core.v2_0.credential.SignedCallChain;
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
   * A cadeia
   */
  private Chain chain;

  /**
   * Construtor
   * 
   * @param chain a cadeia de chamadas.
   */
  public CallerChainImpl(CallChain chain, SignedData signedChain) {
    this.chain = new Chain(signedChain);
    this.chain.updateInfos(chain);
  }

  /**
   * Construtor
   * 
   * @param chain a cadeia de chamadas.
   */
  public CallerChainImpl(String bus,
    tecgraf.openbus.core.v2_0.services.access_control.CallChain chain,
    SignedCallChain signedChain) {
    this.chain = new Chain(signedChain);
    this.chain.updateInfos(bus, chain);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String busid() {
    return chain.bus;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String target() {
    return chain.target;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LoginInfo[] originators() {
    return chain.originators;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LoginInfo caller() {
    return chain.caller;
  }

  Chain internal_chain() {
    return chain;
  }

}
