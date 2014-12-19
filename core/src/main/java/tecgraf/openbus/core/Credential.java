package tecgraf.openbus.core;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.IOP.Codec;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.IOP.CodecPackage.TypeMismatch;

import tecgraf.openbus.core.v2_0.credential.SignedCallChain;
import tecgraf.openbus.core.v2_1.credential.CredentialContextId;
import tecgraf.openbus.core.v2_1.credential.CredentialData;
import tecgraf.openbus.core.v2_1.credential.CredentialDataHelper;
import tecgraf.openbus.core.v2_1.credential.CredentialReset;
import tecgraf.openbus.core.v2_1.credential.CredentialResetHelper;
import tecgraf.openbus.core.v2_1.credential.SignedData;
import tecgraf.openbus.core.v2_1.services.access_control.CallChain;
import tecgraf.openbus.core.v2_1.services.access_control.CallChainHelper;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;

/**
 * Tipo interno para representação da credencial que procura abstrair se a mesma
 * é legada ou não.
 *
 * @author Tecgraf/PUC-Rio
 */
class Credential {

  /** Identificador de barramento */
  public java.lang.String bus;
  /** Identificador de login do caller */
  public java.lang.String login;
  /** Identificador da sessão */
  public int session;
  /** Ticket */
  public int ticket;
  /** Hash da credencial */
  public byte[] hash;
  /** Cadeia */
  public Chain chain;
  /**
   * Indicador se representa uma credencial legada ou não. Quando
   * <code>null</code> significa que esta é uma credencial nula para iniciar o
   * handshake
   */
  public Boolean legacy = null;

  /**
   * Construtor
   * 
   * @param credential a credencial
   */
  Credential(CredentialData credential) {
    legacy = false;
    bus = credential.bus;
    login = credential.login;
    session = credential.session;
    ticket = credential.ticket;
    hash = credential.hash;
    chain = new Chain(credential.chain);
  }

  /**
   * Construtor
   * 
   * @param credential a credential legada
   */
  Credential(tecgraf.openbus.core.v2_0.credential.CredentialData credential) {
    legacy = true;
    bus = credential.bus;
    login = credential.login;
    session = credential.session;
    ticket = credential.ticket;
    hash = credential.hash;
    chain = new Chain(credential.chain);
  }

  /**
   * Construtor.
   * 
   * @param busId identificador de barramento
   * @param login identificador de login do caller
   * @param session identificador da sessão associada
   * @param ticket ticket associado à requisição
   * @param credentialDataHash hash da credencial
   * @param chain cadeia associada
   * @param legacy indicador se em modo legado
   */
  Credential(String busId, String login, int session, int ticket,
    byte[] credentialDataHash, Chain chain, Boolean legacy) {
    this.legacy = legacy;
    this.bus = busId;
    this.login = login;
    this.session = session;
    this.ticket = ticket;
    this.hash = credentialDataHash;
    this.chain = chain;
  }

  /**
   * Decodifica a informação de cadeia
   * 
   * @param codec o codec
   * 
   * @return a cadeia associada.
   * @throws TypeMismatch
   * @throws FormatMismatch
   */
  Chain decodeChain(Codec codec) throws FormatMismatch, TypeMismatch {
    if (!legacy) {
      Any any =
        codec.decode_value(chain.signedChain.encoded, CallChainHelper.type());
      CallChain callChain = CallChainHelper.extract(any);
      chain.updateInfos(callChain);
      return chain;
    }
    else {
      Any any =
        codec.decode_value(chain.signedLegacy.encoded,
          tecgraf.openbus.core.v2_0.services.access_control.CallChainHelper
            .type());
      tecgraf.openbus.core.v2_0.services.access_control.CallChain callChain =
        tecgraf.openbus.core.v2_0.services.access_control.CallChainHelper
          .extract(any);
      chain.updateInfos(bus, callChain);
      return chain;
    }
  }

  /**
   * Exporta a credencial para o formato {@link ServiceContext}, associando para
   * o identificador de contexto associado ao tipo da credencial.
   * 
   * @param orb o orb
   * @param codec o codificador
   * @return o {@link ServiceContext} desta credencial
   * @throws InvalidTypeForEncoding
   */
  ServiceContext toServiceContext(ORB orb, Codec codec)
    throws InvalidTypeForEncoding {
    Any anyCredential = orb.create_any();
    if (!legacy) {
      CredentialData credential =
        new CredentialData(this.bus, this.login, this.session, this.ticket,
          this.hash, this.chain.signedChain);
      CredentialDataHelper.insert(anyCredential, credential);
      byte[] encoded = codec.encode_value(anyCredential);
      return new ServiceContext(CredentialContextId.value, encoded);
    }
    else {
      tecgraf.openbus.core.v2_0.credential.CredentialData credential =
        new tecgraf.openbus.core.v2_0.credential.CredentialData(this.bus,
          this.login, this.session, this.ticket, this.hash,
          this.chain.signedLegacy);
      tecgraf.openbus.core.v2_0.credential.CredentialDataHelper.insert(
        anyCredential, credential);
      byte[] encoded = codec.encode_value(anyCredential);
      return new ServiceContext(
        tecgraf.openbus.core.v2_0.credential.CredentialContextId.value, encoded);
    }
  }

  /**
   * Tipo interno para representação da cadeia que procura abstrair se a mesma é
   * legada ou não.
   *
   * @author Tecgraf/PUC-Rio
   */
  static class Chain {

    /** Cadeia assinada */
    public SignedData signedChain = InterceptorImpl.NULL_SIGNED_CALL_CHAIN;
    /** Cadeia legada assinada */
    public SignedCallChain signedLegacy =
      InterceptorImpl.NULL_SIGNED_LEGACY_CALL_CHAIN;
    /** Identificador do barramento */
    public java.lang.String bus;
    /** Entidade alvo da cadeia */
    public java.lang.String target;
    /** Originadores da chamada */
    public tecgraf.openbus.core.v2_1.services.access_control.LoginInfo[] originators;
    /** Requerente da chamada que usou esta cadeia */
    public tecgraf.openbus.core.v2_1.services.access_control.LoginInfo caller;

    /**
     * Construtor.
     * 
     * @param chain cadeia assinada
     */
    Chain(SignedData chain) {
      if (chain != null) {
        signedChain = chain;
      }
    }

    /**
     * Construtor
     * 
     * @param chain Cadeia legada assinada
     */
    Chain(SignedCallChain chain) {
      if (chain != null) {
        signedLegacy = chain;
      }
    }

    /**
     * Construtor
     * 
     * @param chain cadeia assinada
     * @param legacy_chain cadeia legada assinada
     */
    Chain(SignedData chain, SignedCallChain legacy_chain) {
      if (chain != null) {
        signedChain = chain;
      }
      if (legacy_chain != null) {
        signedLegacy = legacy_chain;
      }
    }

    /**
     * Atualiza as informações da cadeia com base na informação passada como
     * argumento.
     * 
     * @param callChain a cadeia com as informações a serem referenciadas por
     *        este tipo {@link Chain}
     */
    void updateInfos(CallChain callChain) {
      bus = callChain.bus;
      target = callChain.target;
      caller = callChain.caller;
      originators = callChain.originators;
    }

    /**
     * Atualiza as informações da cadeia.
     * 
     * @param busid identificador de barramento
     * @param callChain a cadeia legada.
     */
    void updateInfos(String busid,
      tecgraf.openbus.core.v2_0.services.access_control.CallChain callChain) {
      bus = busid;
      target = callChain.target;
      caller = new LoginInfo(callChain.caller.id, callChain.caller.entity);
      originators = new LoginInfo[callChain.originators.length];
      for (int i = 0; i < callChain.originators.length; i++) {
        tecgraf.openbus.core.v2_0.services.access_control.LoginInfo info =
          callChain.originators[i];
        originators[i] = new LoginInfo(info.id, info.entity);
      }
    }

    /**
     * Recupera a cadeia codificada
     * 
     * @return a cadeia codificada
     */
    byte[] encoded() {
      if (signedChain != InterceptorImpl.NULL_SIGNED_CALL_CHAIN) {
        return signedChain.encoded;
      }
      else {
        return signedLegacy.encoded;
      }
    }

    /**
     * Recupera a assinatura da cadeia.
     * 
     * @return a assinatura.
     */
    byte[] signature() {
      if (signedChain != InterceptorImpl.NULL_SIGNED_CALL_CHAIN) {
        return signedChain.signature;
      }
      else {
        return signedLegacy.signature;
      }
    }

    /**
     * Indicação se a cadeia é legada ou não.
     * 
     * @return <code>true</code> caso seja legada, e <code>false</code> caso
     *         contrário.
     */
    boolean isLegacy() {
      return signedChain == InterceptorImpl.NULL_SIGNED_CALL_CHAIN;
    }

  }

  /**
   * Tipo interno para representação do CredentialReset, abstraindo se o mesmo é
   * legado ou não.
   *
   * @author Tecgraf/PUC-Rio
   */
  static class Reset {

    /** Identificador de login da entidade alvo da chamada */
    public java.lang.String target;
    /** Nome da entidade alvo da chamada */
    public java.lang.String entity;
    /** Identificador de sessão */
    public int session;
    /** Desafio */
    public byte[] challenge;
    /** Indicador se esta em modo legado */
    public boolean legacy;

    /**
     * Construtor
     * 
     * @param login login do alvo da chamada
     * @param session identificador da sessão
     * @param challenge o desafio
     * @param legacy se em modo legado
     */
    Reset(LoginInfo login, int session, byte[] challenge, boolean legacy) {
      this.target = login.id;
      this.entity = login.entity;
      this.session = session;
      this.challenge = challenge;
      this.legacy = legacy;
    }

    /**
     * Exporta o CredentialReset para o formato {@link ServiceContext},
     * associando para o identificador de contexto associado à versão de
     * protocolo adotada na comunicação.
     * 
     * @param orb o orb
     * @param codec o codificador
     * @return o {@link ServiceContext} desta credencial
     * @throws InvalidTypeForEncoding
     */
    ServiceContext toServiceContext(ORB orb, Codec codec)
      throws InvalidTypeForEncoding {
      if (!legacy) {
        CredentialReset reset =
          new CredentialReset(target, entity, session, challenge);
        Any any = orb.create_any();
        CredentialResetHelper.insert(any, reset);
        byte[] encodedCredential = codec.encode_value(any);
        return new ServiceContext(CredentialContextId.value, encodedCredential);
      }
      else {
        tecgraf.openbus.core.v2_0.credential.CredentialReset reset =
          new tecgraf.openbus.core.v2_0.credential.CredentialReset(target,
            session, challenge);
        Any any = orb.create_any();
        tecgraf.openbus.core.v2_0.credential.CredentialResetHelper.insert(any,
          reset);
        byte[] encodedCredential = codec.encode_value(any);
        return new ServiceContext(
          tecgraf.openbus.core.v2_0.credential.CredentialContextId.value,
          encodedCredential);
      }
    }
  }

}
