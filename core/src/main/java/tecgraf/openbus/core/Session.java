package tecgraf.openbus.core;

import java.util.concurrent.atomic.AtomicInteger;

import tecgraf.openbus.core.Credential.Reset;

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
   * Construtor.
   * 
   * @param session identificador da sessão
   * @param secret o segredo.
   */
  public Session(int session, byte[] secret) {
    this.session = session;
    this.secret = secret;
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
     * Originador da comunicação.
     */
    final private String caller;

    /**
     * Construtor.
     * 
     * @param session identificador da sessão
     * @param secret o segredo.
     * @param caller originador da comunicação
     */
    public ServerSideSession(int session, byte[] secret, String caller) {
      super(session, secret);
      this.ticket = new TicketsHistory();
      this.caller = caller;
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

    /**
     * Recupera a informação do originador da comunicação
     * 
     * @return o originador da comunicação.
     */
    public String getCaller() {
      return this.caller;
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
     * Entidade do alvo da comunicação.
     */
    final private String entity;
    /**
     * Indicador se comunicação faz uso de protocolo legado
     */
    final private boolean legacy;

    /**
     * Construtor.
     * 
     * @param reset informações do CredentialReset
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
     * Recupera o valor do próximo ticket.
     * 
     * @return o valor do próximo ticket.
     */
    public int nextTicket() {
      return this.ticket.incrementAndGet();
    }

    /**
     * Recupera a informação de entidade do alvo da comunicação
     * 
     * @return o alvo da comunicação.
     */
    public String getEntity() {
      return this.entity;
    }

    /**
     * Indicador se sessão faz uso de protocolo legado
     * 
     * @return se sessão faz uso de protocolo legado.
     */
    public boolean legacy() {
      return legacy;
    }
  }
}
