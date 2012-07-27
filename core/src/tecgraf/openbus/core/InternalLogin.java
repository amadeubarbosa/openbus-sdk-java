package tecgraf.openbus.core;

import java.util.logging.Level;
import java.util.logging.Logger;

import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;

/**
 * Classe que representa o login da conexão internamente.
 * 
 * @author Tecgraf
 */
class InternalLogin {

  /** Instância de logging. */
  private static final Logger logger = Logger.getLogger(ConnectionImpl.class
    .getName());

  /** Informações do login */
  private LoginInfo login;
  /** Informações do login inválido */
  private LoginInfo invalid = null;
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
   * Recupera a informação do login atual.
   * 
   * @return informação do login.
   */
  LoginInfo login() {
    conn.readLock().lock();
    try {
      return this.login;
    }
    finally {
      conn.readLock().unlock();
    }
  }

  /**
   * Recupera a informação do login inválido.
   * 
   * @return informação do login inválido.
   */
  LoginInfo invalid() {
    conn.readLock().lock();
    try {
      return this.invalid;
    }
    finally {
      conn.readLock().unlock();
    }
  }

  /**
   * Retorna a informação do login, relizando a chamada da callback, caso o
   * login esteja inválido.
   * 
   * @return informações do login.
   */
  LoginInfo getLogin() {
    conn.readLock().lock();
    LoginInfo login = this.login;
    LoginInfo invalid = this.invalid;
    conn.readLock().unlock();
    if (login == null) {
      while (invalid != null) {
        InvalidLoginCallback callback = conn.onInvalidLoginCallback();
        if (callback != null) {
          try {
            callback.invalidLogin(conn, invalid);
          }
          catch (Exception ex) {
            logger.log(Level.SEVERE,
              "Callback gerou um erro durante execução.", ex);
          }
          LoginInfo curr = this.invalid();
          if (curr != null && curr.id.equals(invalid.id)
            && curr.entity.equals(invalid.entity)) {
            invalid = null;
            conn.writeLock().lock();
            this.invalid = null;
            conn.writeLock().unlock();
          }
          else {
            invalid = curr;
          }
        }
        else {
          invalid = null;
          conn.writeLock().lock();
          this.invalid = null;
          conn.writeLock().unlock();
        }
      }
      login = this.login();
    }
    return login;
  }

  /**
   * Configura o login como logado.
   * 
   * @param login a informação do login.
   */
  void setLoggedIn(LoginInfo login) {
    conn.writeLock().lock();
    this.login = login;
    this.invalid = null;
    conn.writeLock().unlock();
  }

  /**
   * Configura o login como deslogado.
   * 
   * @return o login que foi deslogado.
   */
  LoginInfo setLoggedOut() {
    conn.writeLock().lock();
    LoginInfo old = login;
    login = null;
    invalid = null;
    conn.writeLock().unlock();
    return old;
  }

  /**
   * Configura o login como inválido.
   */
  void setInvalid() {
    conn.writeLock().lock();
    invalid = login;
    login = null;
    conn.writeLock().unlock();
  }

}
