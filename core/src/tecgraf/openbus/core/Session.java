package tecgraf.openbus.core;

import tecgraf.openbus.exception.CryptographyException;
import tecgraf.openbus.util.Cryptography;
import tecgraf.openbus.util.TicketsHistory;

abstract class Session {

  private int session;
  protected byte[] secret;
  private String callee;

  public Session(int session, byte[] secret, String callee) {
    this.session = session;
    this.secret = secret;
    this.callee = callee;
  }

  public int getSession() {
    return this.session;
  }

  public byte[] getSecret() {
    return this.secret;
  }

  public String getCallee() {
    return this.callee;
  }

  static class ServerSideSession extends Session {
    private TicketsHistory ticket;

    public ServerSideSession(int session, byte[] secret, String callee) {
      super(session, secret, callee);
      this.ticket = new TicketsHistory();
    }

    public TicketsHistory getTicket() {
      return this.ticket;
    }

  }

  static class ClientSideSession extends Session {
    private int ticket;
    private volatile boolean decrypted = false;

    public ClientSideSession(int session, byte[] secret, String callee) {
      super(session, secret, callee);
      this.ticket = -1;
    }

    public int nextTicket() {
      return ++this.ticket;
    }

    public byte[] getDecryptedSecret(ConnectionImpl conn)
      throws CryptographyException {
      if (!decrypted) {
        synchronized (this) {
          if (!decrypted) {
            Cryptography crypto = Cryptography.getInstance();
            byte[] decrypted =
              crypto.decrypt(this.secret, conn.getPrivateKey());
            this.secret = decrypted;
            this.decrypted = true;
          }
        }
      }
      return this.secret;
    }
  }
}