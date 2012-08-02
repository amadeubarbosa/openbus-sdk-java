package tecgraf.openbus.core;

import java.util.concurrent.atomic.AtomicInteger;

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
  final private int session;
  /**
   * O segredo compartilhado na sessão
   */
  final protected byte[] secret;
  /**
   * Alvo da comunicação.
   */
  final private String callee;

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
     * Verifica se o ticket é válido e marca com utilizado caso seja válido.
     * 
     * @param id o ticket a ser utilizado.
     * @return <code>true</code> caso o ticket era válido e foi marcado, e
     *         <code>false</code> caso o ticket não fosse válido.
     */
    public boolean checkTicket(int id) {
      return this.ticket.check(id);
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
    private AtomicInteger ticket;

    /**
     * Construtor.
     * 
     * @param session identificador da sessão
     * @param secret o segredo.
     * @param callee alvo da comunicação
     */
    public ClientSideSession(int session, byte[] secret, String callee) {
      super(session, secret, callee);
      this.ticket = new AtomicInteger(-1);
    }

    /**
     * Recupera o valor do próximo ticket.
     * 
     * @return o valor do próximo ticket.
     */
    public int nextTicket() {
      return this.ticket.incrementAndGet();
    }

  }
}
