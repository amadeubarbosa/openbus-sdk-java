package tecgraf.openbus.core;

import java.util.logging.Logger;

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
import tecgraf.openbus.core.v2_1.credential.CredentialReset;
import tecgraf.openbus.core.v2_1.credential.CredentialResetHelper;
import tecgraf.openbus.core.v2_1.credential.SignedData;
import tecgraf.openbus.core.v2_1.services.access_control.CallChain;
import tecgraf.openbus.core.v2_1.services.access_control.CallChainHelper;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;

class Credential {

  /** Instância de logging. */
  private static final Logger logger = Logger.getLogger(Credential.class
    .getName());

  public java.lang.String bus;
  public java.lang.String login;
  public int session;
  public int ticket;
  public byte[] hash;
  public Chain chain;

  public boolean legacy;

  public Credential(CredentialData credential) {
    legacy = false;
    bus = credential.bus;
    login = credential.login;
    session = credential.session;
    ticket = credential.ticket;
    hash = credential.hash;
    chain = new Chain(credential.chain);
  }

  public Credential(
    tecgraf.openbus.core.v2_0.credential.CredentialData credential) {
    legacy = true;
    bus = credential.bus;
    login = credential.login;
    session = credential.session;
    ticket = credential.ticket;
    hash = credential.hash;
    chain = new Chain(credential.chain);
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
  public Chain decodeChain(Codec codec) throws FormatMismatch, TypeMismatch {
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

  public static class Chain {

    public SignedData signedChain = null;
    public SignedCallChain signedLegacy = null;

    public java.lang.String bus;
    public java.lang.String target;
    public tecgraf.openbus.core.v2_1.services.access_control.LoginInfo[] originators;
    public tecgraf.openbus.core.v2_1.services.access_control.LoginInfo caller;

    Chain(SignedData chain) {
      signedChain = chain;
    }

    Chain(SignedCallChain chain) {
      signedLegacy = chain;
    }

    void updateInfos(CallChain callChain) {
      bus = callChain.bus;
      target = callChain.target;
      caller = callChain.caller;
      originators = callChain.originators;
    }

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

    byte[] encoded() {
      if (signedChain != null) {
        return signedChain.encoded;
      }
      else {
        return signedLegacy.encoded;
      }
    }

    byte[] signature() {
      if (signedChain != null) {
        return signedChain.signature;
      }
      else {
        return signedLegacy.signature;
      }
    }
  }

  public static class Reset {

    private java.lang.String target;
    private java.lang.String entity;
    private int session;
    private byte[] challenge;
    private Credential credential;

    public Reset(LoginInfo login, int session, byte[] challenge,
      Credential credential) {
      this.target = login.id;
      this.entity = login.entity;
      this.session = session;
      this.challenge = challenge;
      this.credential = credential;
    }

    public ServiceContext getServiceContext(ORB orb, Codec codec)
      throws InvalidTypeForEncoding {
      if (!credential.legacy) {
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
