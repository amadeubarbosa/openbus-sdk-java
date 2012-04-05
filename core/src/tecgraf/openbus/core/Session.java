package tecgraf.openbus.core;

import tecgraf.openbus.exception.CryptographyException;
import tecgraf.openbus.util.Cryptography;
import tecgraf.openbus.util.TicketsHistory;

/**
 * Classe que representa a parte comum entre a sess�o do lado servidor e
 * cliente.
 * 
 * @author Tecgraf
 */
abstract class Session {

  /**
   * Identificador da sess�o.
   */
  private int session;
  /**
   * O segredo compartilhado na sess�o
   */
  protected byte[] secret;
  /**
   * Alvo da comunica��o.
   */
  private String callee;

  /**
   * Construtor.
   * 
   * @param session identificador da sess�o
   * @param secret o segredo.
   * @param callee alvo da comunica��o
   */
  public Session(int session, byte[] secret, String callee) {
    this.session = session;
    this.secret = secret;
    this.callee = callee;
  }

  /**
   * Recupera o identificador da sess�o.
   * 
   * @return o identificador da sess�o.
   */
  public int getSession() {
    return this.session;
  }

  /**
   * Recupera o segredo.
   * 
   * @return o segredo.
   */
  public byte[] getSecret() {
    return this.secret;
  }

  /**
   * Recupera a informa��o do alvo da comunica��o
   * 
   * @return o alvo da comunica��o.
   */
  public String getCallee() {
    return this.callee;
  }

  /**
   * Representa a sess�o do lado servidor.
   * 
   * @author Tecgraf
   */
  static class ServerSideSession extends Session {
    /**
     * Hist�rico dos tickets.
     */
    private TicketsHistory ticket;

    /**
     * Construtor.
     * 
     * @param session identificador da sess�o
     * @param secret o segredo.
     * @param callee alvo da comunica��o
     */
    public ServerSideSession(int session, byte[] secret, String callee) {
      super(session, secret, callee);
      this.ticket = new TicketsHistory();
    }

    /**
     * Recupera o hist�rico dos tickets utilizados
     * 
     * @return o hist�rico dos tickets.
     */
    public TicketsHistory getTicket() {
      return this.ticket;
    }

  }

  /**
   * Repesenta a sess�o do lado cliente.
   * 
   * @author Tecgraf
   */
  static class ClientSideSession extends Session {
    /**
     * Valor do �ltimo ticket utilizado
     */
    private int ticket;
    /**
     * Identifica se o segredo j� foi desencriptado
     */
    private volatile boolean decrypted = false;

    /**
     * Construtor.
     * 
     * @param session identificador da sess�o
     * @param secret o segredo.
     * @param callee alvo da comunica��o
     */
    public ClientSideSession(int session, byte[] secret, String callee) {
      super(session, secret, callee);
      this.ticket = -1;
    }

    /**
     * Recupera o valor do pr�ximo ticket.
     * 
     * @return o valor do pr�ximo ticket.
     */
    public int nextTicket() {
      return ++this.ticket;
    }

    /**
     * Recupera o segredo desencriptado.
     * 
     * @param conn a conex�o em uso.
     * @return o segredo.
     * @throws CryptographyException
     */
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
