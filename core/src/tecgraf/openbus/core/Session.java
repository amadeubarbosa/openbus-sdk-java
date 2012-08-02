package tecgraf.openbus.core;

import java.util.concurrent.atomic.AtomicInteger;

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
  final private int session;
  /**
   * O segredo compartilhado na sess�o
   */
  final protected byte[] secret;
  /**
   * Alvo da comunica��o.
   */
  final private String callee;

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
     * Verifica se o ticket � v�lido e marca com utilizado caso seja v�lido.
     * 
     * @param id o ticket a ser utilizado.
     * @return <code>true</code> caso o ticket era v�lido e foi marcado, e
     *         <code>false</code> caso o ticket n�o fosse v�lido.
     */
    public boolean checkTicket(int id) {
      return this.ticket.check(id);
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
    private AtomicInteger ticket;

    /**
     * Construtor.
     * 
     * @param session identificador da sess�o
     * @param secret o segredo.
     * @param callee alvo da comunica��o
     */
    public ClientSideSession(int session, byte[] secret, String callee) {
      super(session, secret, callee);
      this.ticket = new AtomicInteger(-1);
    }

    /**
     * Recupera o valor do pr�ximo ticket.
     * 
     * @return o valor do pr�ximo ticket.
     */
    public int nextTicket() {
      return this.ticket.incrementAndGet();
    }

  }
}
