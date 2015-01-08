package tecgraf.openbus.core;

import java.util.concurrent.atomic.AtomicInteger;

import tecgraf.openbus.core.Credential.Reset;

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
   * Construtor.
   * 
   * @param session identificador da sess�o
   * @param secret o segredo.
   */
  public Session(int session, byte[] secret) {
    this.session = session;
    this.secret = secret;
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
     * Originador da comunica��o.
     */
    final private String caller;

    /**
     * Construtor.
     * 
     * @param session identificador da sess�o
     * @param secret o segredo.
     * @param caller originador da comunica��o
     */
    public ServerSideSession(int session, byte[] secret, String caller) {
      super(session, secret);
      this.ticket = new TicketsHistory();
      this.caller = caller;
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

    /**
     * Recupera a informa��o do originador da comunica��o
     * 
     * @return o originador da comunica��o.
     */
    public String getCaller() {
      return this.caller;
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
     * Entidade do alvo da comunica��o.
     */
    final private String entity;
    /**
     * Indicador se comunica��o faz uso de protocolo legado
     */
    final private boolean legacy;

    /**
     * Construtor.
     * 
     * @param reset informa��es do CredentialReset
     * @param secret o segredo.
     */
    public ClientSideSession(Reset reset, byte[] secret) {
      super(reset.session, secret);
      this.ticket = new AtomicInteger(-1);
      this.legacy = reset.legacy;
      if (!legacy) {
        this.entity = reset.entity;
      }
      else {
        this.entity = reset.target;
      }
    }

    /**
     * Recupera o valor do pr�ximo ticket.
     * 
     * @return o valor do pr�ximo ticket.
     */
    public int nextTicket() {
      return this.ticket.incrementAndGet();
    }

    /**
     * Recupera a informa��o de entidade do alvo da comunica��o
     * 
     * @return o alvo da comunica��o.
     */
    public String getEntity() {
      return this.entity;
    }

    /**
     * Indicador se sess�o faz uso de protocolo legado
     * 
     * @return se sess�o faz uso de protocolo legado.
     */
    public boolean legacy() {
      return legacy;
    }
  }
}
