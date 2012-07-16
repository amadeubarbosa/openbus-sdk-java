package tecgraf.openbus.core;

import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;

/**
 * Classe que representa o login da conexão internamente.
 * 
 * @author Tecgraf
 */
class InternalLogin {

  /**
   * Enumeração dos possíveis estados do login.
   * 
   * @author Tecgraf
   */
  enum LoginStatus {
    /** Logado */
    loggedIn,
    /** Deslogado */
    loggedOut,
    /** Inválido */
    invalid
  }

  /** Estado do login. */
  private LoginStatus status;
  /** Informações do login */
  private LoginInfo login;
  /** Cópia das informações do login */
  private LoginInfo cplogin;
  /** A conexão deste login */
  private final ConnectionImpl conn;

  /**
   * Construtor.
   * 
   * @param conn a conexão detentora do login.
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
   * Retorna a informação do login.
   * 
   * @return informações do login.
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
   * @param login a informação do login.
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
   * Configura o login como inválido.
   */
  void setInvalid() {
    conn.writeLock().lock();
    status = LoginStatus.invalid;
    conn.writeLock().unlock();
  }

}
