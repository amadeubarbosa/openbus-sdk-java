package tecgraf.openbus.core;

import java.util.ArrayList;
import java.util.List;

import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidLogins;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.LoginObserver;
import tecgraf.openbus.LoginSubscription;

/**
 * Representação local de uma inscrição de observação de login
 * 
 * @author Tecgraf
 */
class LoginSubscriptionImpl implements LoginSubscription, LoginObserver {
  /** Registro de logins */
  private final LoginRegistryImpl registry;
  /** Observador */
  private final LoginObserver observer;
  /** Lista de logins observáveis pelo observador */
  private final List<String> logins = new ArrayList<>();
  private final Object lock = new Object();

  /**
   * Construtor
   * 
   * @param callback observador
   * @param registry registro associado.
   */
  protected LoginSubscriptionImpl(LoginObserver callback, LoginRegistryImpl
    registry) {
    this.observer = callback;
    this.registry = registry;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean watchLogin(String loginId) throws ServiceFailure {
    boolean ret = registry.watchLogin(loginId);
    if (ret) {
      synchronized (lock) {
        logins.add(loginId);
      }
    }
    return ret;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void forgetLogin(String loginId) throws ServiceFailure {
    registry.forgetLogin(loginId);
    synchronized (lock) {
      logins.remove(loginId);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void watchLogins(List<String> loginIds) throws InvalidLogins,
    ServiceFailure {
    registry.watchLogins(loginIds);
    synchronized (lock) {
      logins.addAll(loginIds);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void forgetLogins(List<String> loginIds) throws ServiceFailure {
    registry.forgetLogins(loginIds);
    synchronized (lock) {
      logins.removeAll(loginIds);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<LoginInfo> watchedLogins() {
    // watched é uma cópia não-compartilhada
    List<LoginInfo> watched = registry.getWatchedLogins();
    List<LoginInfo> ret = new ArrayList<>();
    // aqui fiquei na dúvida se seria melhor colocar o synchronized dentro do
    // for. Deixei fora pois me parece que tipicamente o processamento dessa
    // lista será mais rápido do que realizar a sincronização diversas vezes.
    synchronized (lock) {
      for (LoginInfo info : watched) {
        if (logins.contains(info.id)) {
          ret.add(info);
        }
      }
    }
    return ret;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void remove() {
    synchronized (lock) {
      registry.remove(this);
      logins.clear();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LoginObserver observer() {
    synchronized (lock) {
      return observer;
    }
  }

  @Override
  public void entityLogout(LoginInfo login) {
    LoginObserver observer;
    boolean doit = false;
    synchronized (lock) {
      observer = this.observer;
      if (logins.contains(login.id)) {
        doit = true;
      }
    }
    if (doit) {
      observer.entityLogout(login);
    }
  }

  @Override
  public void nonExistentLogins(String[] logins) {
    observer().nonExistentLogins(logins);
  }
}
