package tecgraf.openbus.defaultimpl;

import java.util.logging.Logger;

import org.omg.CORBA.LocalObject;

import tecgraf.openbus.BusORB;

final class ORBMediator extends LocalObject {
  private static final Logger logger = Logger.getLogger(ORBMediator.class
    .getName());
  public static final String INITIAL_REFERENCE_ID = "openbus.ORBManager";

  private BusORB orb;
  private int credentialSlotId;

  ORBMediator(int credentialSlotId) {
    this.credentialSlotId = credentialSlotId;
  }

  BusORB getORB() {
    return this.orb;
  }

  void setORB(BusORB orb) {
    this.orb = orb;
  }

  int getCredentialSlotId() {
    return this.credentialSlotId;
  }
}
