package tecgraf.openbus.core.interceptor;

import tecgraf.openbus.core.v2_00.services.access_control.SignedCallChain;

public class CredentialSession {

  private int ticket = 0;
  private int session;
  private byte[] secret;
  private SignedCallChain chain;

  public CredentialSession(int session, byte[] secret, SignedCallChain callChain) {
    this.session = session;
    this.secret = secret;
    this.chain = callChain;
  }

  public void generateNextTicket() {
    this.ticket++;
  }

  public int getSession() {
    return this.session;
  }

  public SignedCallChain getDefaultCallChain() {
    return this.chain;
  }

  public int getTicket() {
    return this.ticket;
  }

  public byte[] getSecret() {
    return this.secret;
  }

}
