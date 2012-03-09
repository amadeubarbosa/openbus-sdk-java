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
  private int credentialSlotId;
  private BusORB orb;

  ORBMediator(Codec codec, int credentialSlotId) {
    this.codec = codec;
    this.credentialSlotId = credentialSlotId;
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
    return this.credentialSlotId;
  }
}
