package tecgraf.openbus.core;

import tecgraf.openbus.exception.CryptographyException;
import tecgraf.openbus.util.Cryptography;
import tecgraf.openbus.util.TicketsHistory;

/**
 * Classe que representa a parte comum entre a sessão do lado servidor e
 * cliente.
 * 
 * @author Tecgraf
 */
abstract class Session {

  /**
   * Identificador da sessão.
   */
  private int session;
  /**
   * O segredo compartilhado na sessão
   */
  protected byte[] secret;
  /**
   * Alvo da comunicação.
   */
  private String callee;

  /**
   * Construtor.
   * 
   * @param session identificador da sessão
   * @param secret o segredo.
   * @param callee alvo da comunicação
   */
  public Session(int session, byte[] secret, String callee) {
    this.session = session;
    this.secret = secret;
    this.callee = callee;
  }

  /**
   * Recupera o identificador da sessão.
   * 
   * @return o identificador da sessão.
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
   * Recupera a informação do alvo da comunicação
   * 
   * @return o alvo da comunicação.
   */
  public String getCallee() {
    return this.callee;
  }

  /**
   * Representa a sessão do lado servidor.
   * 
   * @author Tecgraf
   */
  static class ServerSideSession extends Session {
    /**
     * Histórico dos tickets.
     */
    private TicketsHistory ticket;

    /**
     * Construtor.
     * 
     * @param session identificador da sessão
     * @param secret o segredo.
     * @param callee alvo da comunicação
     */
    public ServerSideSession(int session, byte[] secret, String callee) {
      super(session, secret, callee);
      this.ticket = new TicketsHistory();
    }

    /**
     * Recupera o histórico dos tickets utilizados
     * 
     * @return o histórico dos tickets.
     */
    public TicketsHistory getTicket() {
      return this.ticket;
    }

  }

  /**
   * Repesenta a sessão do lado cliente.
   * 
   * @author Tecgraf
   */
  static class ClientSideSession extends Session {
    /**
     * Valor do último ticket utilizado
     */
    private int ticket;
    /**
     * Identifica se o segredo já foi desencriptado
     */
    private volatile boolean decrypted = false;

    /**
     * Construtor.
     * 
     * @param session identificador da sessão
     * @param secret o segredo.
     * @param callee alvo da comunicação
     */
    public ClientSideSession(int session, byte[] secret, String callee) {
      super(session, secret, callee);
      this.ticket = -1;
    }

    /**
     * Recupera o valor do próximo ticket.
     * 
     * @return o valor do próximo ticket.
     */
    public int nextTicket() {
      return ++this.ticket;
    }

    /**
     * Recupera o segredo desencriptado.
     * 
     * @param conn a conexão em uso.
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
