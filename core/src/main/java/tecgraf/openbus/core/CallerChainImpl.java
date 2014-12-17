package tecgraf.openbus.core;

import org.omg.CORBA.Any;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.IOP.CodecPackage.TypeMismatch;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.core.Credential.Chain;
import tecgraf.openbus.core.v2_0.credential.SignedCallChain;
import tecgraf.openbus.core.v2_1.credential.SignedData;
import tecgraf.openbus.core.v2_1.services.access_control.CallChain;
import tecgraf.openbus.core.v2_1.services.access_control.CallChainHelper;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.interceptors.CallChainInfo;

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
   * @param signed a cadeia assinada.
   */
  public CallerChainImpl(CallChain chain, SignedData signed) {
    this.chain = new Chain(signed);
    this.chain.updateInfos(chain);
  }

  /**
   * Construtor
   * 
   * @param chain a cadeia de chamadas.
   * @param signed a cadeia assinada.
   * @param signedLegacy a cadeia legada assinada.
   */
  public CallerChainImpl(CallChain chain, SignedData signed,
    SignedCallChain signedLegacy) {
    this.chain = new Chain(signed, signedLegacy);
    this.chain.updateInfos(chain);
  }

  /**
   * Construtor
   * 
   * @param bus identificador do barramento
   * @param chain a cadeia de chamadas.
   * @param signed a cadeia assinada.
   */
  public CallerChainImpl(String bus,
    tecgraf.openbus.core.v2_0.services.access_control.CallChain chain,
    SignedCallChain signed) {
    this.chain = new Chain(signed);
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

  /**
   * Recupera a representação interna de cadeia
   * 
   * @return a representação interna de cadeia
   */
  Chain internal_chain() {
    return chain;
  }

  /**
   * Constrói uma {@link CallerChain} a partir das informações passadas via
   * contexto ({@link CallChainInfo})
   * 
   * @param info as informações sobre a cadeia
   * @param codec o codec
   * @return a represetação em {@link CallerChain} das informações.
   * @throws FormatMismatch
   * @throws TypeMismatch
   */
  static CallerChainImpl info2CallerChain(CallChainInfo info, Codec codec)
    throws FormatMismatch, TypeMismatch {
    if (!info.legacy) {
      Any anyChain =
        codec.decode_value(info.chain.encoded, CallChainHelper.type());
      CallChain callchain = CallChainHelper.extract(anyChain);
      return new CallerChainImpl(callchain, info.chain, info.legacy_chain);
    }
    else {
      Any anyChain =
        codec.decode_value(info.legacy_chain.encoded,
          tecgraf.openbus.core.v2_0.services.access_control.CallChainHelper
            .type());
      tecgraf.openbus.core.v2_0.services.access_control.CallChain callchain =
        tecgraf.openbus.core.v2_0.services.access_control.CallChainHelper
          .extract(anyChain);
      return new CallerChainImpl(info.bus, callchain, info.legacy_chain);
    }
  }
}
