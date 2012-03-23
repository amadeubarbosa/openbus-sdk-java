package tecgraf.openbus.core;

import java.util.logging.Logger;

import org.omg.CORBA.LocalObject;
import org.omg.IOP.Codec;

import tecgraf.openbus.BusORB;

final class ORBMediator extends LocalObject {
  private static final Logger logger = Logger.getLogger(ORBMediator.class
    .getName());
  public static final String INITIAL_REFERENCE_ID = "openbus.ORBManager";

  private Codec codec;
  private final int CREDENTIAL_SLOT_ID;
  /** Identificador do slot de joined chain */
  private final int JOINED_CHAIN_SLOT_ID;
  private BusORB orb;
  private ConnectionMultiplexerImpl multiplexer;

  ORBMediator(Codec codec, int credentialSlotId, int chainSlotId,
    ConnectionMultiplexerImpl multiplexer) {
    this.codec = codec;
    this.CREDENTIAL_SLOT_ID = credentialSlotId;
    this.JOINED_CHAIN_SLOT_ID = chainSlotId;
    this.multiplexer = multiplexer;
  }

  BusORB getORB() {
    return this.orb;
  }

  void setORB(BusORB orb) {
    this.orb = orb;
  }

  Codec getCodec() {
    return this.codec;
  }

  int getCredentialSlotId() {
    return this.CREDENTIAL_SLOT_ID;
  }

  int getJoinedChainSlotId() {
    return this.JOINED_CHAIN_SLOT_ID;
  }

  public ConnectionMultiplexerImpl getConnectionMultiplexer() {
    return multiplexer;
  }
}
