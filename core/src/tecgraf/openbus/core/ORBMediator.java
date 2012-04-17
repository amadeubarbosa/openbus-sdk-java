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
  /** Identificador do slot da cadeia assinada */
  private final int SIGNED_CHAIN_SLOT_ID;
  /** Identificador do slot de joined chain */
  private final int JOINED_CHAIN_SLOT_ID;
  /** Identificador do slot da caller chain */
  private final int CONNECTION_SLOT_ID;
  /** o ORB */
  private ORB orb;
  /** o multiplexador */
  private ConnectionManagerImpl connections;

  ORBMediator(Codec codec, int signedChainSlotId, int chainSlotId,
    int callerChainSlotId, ConnectionManagerImpl multiplexer) {
    this.codec = codec;
    this.SIGNED_CHAIN_SLOT_ID = signedChainSlotId;
    this.JOINED_CHAIN_SLOT_ID = chainSlotId;
    this.CONNECTION_SLOT_ID = callerChainSlotId;
    this.connections = multiplexer;
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

  int getSignedChainSlotId() {
    return this.SIGNED_CHAIN_SLOT_ID;
  }

  int getJoinedChainSlotId() {
    return this.JOINED_CHAIN_SLOT_ID;
  }

  int getConnectionSlotId() {
    return this.CONNECTION_SLOT_ID;
  }

  public ConnectionManagerImpl getConnectionManager() {
    return connections;
  }

}
