package tecgraf.openbus.core;

import java.util.logging.Logger;

import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;
import org.omg.IOP.Codec;

final class ORBMediator extends LocalObject {
  private static final Logger logger = Logger.getLogger(ORBMediator.class
    .getName());
  public static final String INITIAL_REFERENCE_ID = "openbus.ORBManager";

  private Codec codec;
  /** Identificador do slot da credencial */
  private final int CREDENTIAL_SLOT_ID;
  /** Identificador do slot de joined chain */
  private final int JOINED_CHAIN_SLOT_ID;
  /** Identificador do slot da caller chain */
  private final int CONNECTION_SLOT_ID;
  /** o ORB */
  private ORB orb;
  /** o multiplexador */
  private ConnectionManagerImpl multiplexer;
  /** suporte legado */
  private boolean legacy;

  ORBMediator(Codec codec, int credentialSlotId, int chainSlotId,
    int callerChainSlotId, ConnectionManagerImpl multiplexer) {
    this.codec = codec;
    this.CREDENTIAL_SLOT_ID = credentialSlotId;
    this.JOINED_CHAIN_SLOT_ID = chainSlotId;
    this.CONNECTION_SLOT_ID = callerChainSlotId;
    this.multiplexer = multiplexer;
    this.legacy = true;
  }

  ORB getORB() {
    return this.orb;
  }

  void setORB(ORB orb) {
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

  int getConnectionSlotId() {
    return this.CONNECTION_SLOT_ID;
  }

  public ConnectionManagerImpl getConnectionMultiplexer() {
    return multiplexer;
  }

  public boolean getLegacy() {
    return legacy;
  }
}
