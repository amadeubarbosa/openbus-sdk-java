package tecgraf.openbus.core;

import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;

/**
 * Classe que representa o login da conex�o internamente.
 * 
 * @author Tecgraf
 */
class InternalLogin {

  /**
   * Enumera��o dos poss�veis estados do login.
   * 
   * @author Tecgraf
   */
  enum LoginStatus {
    /** Logado */
    loggedIn,
    /** Deslogado */
    loggedOut,
    /** Inv�lido */
    invalid
  }

  /** Estado do login. */
  private LoginStatus status;
  /** Informa��es do login */
  private LoginInfo login;
  /** C�pia das informa��es do login */
  private LoginInfo cplogin;
  /** A conex�o deste login */
  private final ConnectionImpl conn;

  /**
   * Construtor.
   * 
   * @param conn a conex�o detentora do login.
   */
  InternalLogin(ConnectionImpl conn) {
    this.conn = conn;
    setLoggedOut();
  }

  /**
   * Retorna o estado do login
   * 
   * @return o estado
   */
  LoginStatus getStatus() {
    conn.readLock().lock();
    try {
      return status;
    }
    finally {
      conn.readLock().unlock();
    }
  }

  /**
   * Retorna a informa��o do login.
   * 
   * @return informa��es do login.
   */
  LoginInfo getLogin() {
    conn.readLock().lock();
    try {
      if (login != null) {
        return cplogin;
      }
      else {
        return null;
      }
    }
    finally {
      conn.readLock().unlock();
    }
  }

  /**
   * Configura o login como logado.
   * 
   * @param login a informa��o do login.
   */
  void setLoggedIn(LoginInfo login) {
    conn.writeLock().lock();
    this.login = login;
    this.cplogin = new LoginInfo(login.id, login.entity);
    status = LoginStatus.loggedIn;
    conn.writeLock().unlock();
  }

  /**
   * Configura o login como deslogado.
   * 
   * @return o login que foi deslogado.
   */
  LoginInfo setLoggedOut() {
    conn.writeLock().lock();
    login = null;
    status = LoginStatus.loggedOut;
    conn.writeLock().unlock();
    return cplogin;
  }

  /**
   * Configura o login como inv�lido.
   */
  void setInvalid() {
    conn.writeLock().lock();
    status = LoginStatus.invalid;
    conn.writeLock().unlock();
  }

}
