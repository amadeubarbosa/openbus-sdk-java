/*
 * $Id $
 */
package tecgraf.openbus.interceptors;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.SystemException;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

import tecgraf.openbus.Openbus;
import tecgraf.openbus.access_control_service.CredentialWrapper;
import tecgraf.openbus.core.v1_05.access_control_service.Credential;
import tecgraf.openbus.core.v1_05.access_control_service.IAccessControlService;
import tecgraf.openbus.util.Log;

/**
 * Implementa a pol�tica de valida��o de credenciais interceptadas em um
 * servidor, armazenando as credenciais j� validadas em um cache para futuras
 * consultas.
 * 
 * @author Tecgraf/PUC-Rio
 */
final class CachedCredentialValidatorServerInterceptor extends LocalObject
  implements ServerRequestInterceptor {
  /**
   * Intervalo de tempo para execu��o da tarefa de valida��o das credenciais do
   * cache.
   */
  private static long TASK_DELAY = 5 * 60 * 1000; // 5 minutos 
  /**
   * Tamanho m�ximo do cache de credenciais.
   */
  private static int MAXIMUM_CREDENTIALS_CACHE_SIZE = 20;
  /**
   * A inst�ncia �nica do interceptador.
   */
  private static CachedCredentialValidatorServerInterceptor instance;
  /**
   * O cache de credenciais, que � mantido utilizando-se a pol�tica LRU (Least
   * Recently Used).
   */
  private Deque<CredentialWrapper> credentials;
  /**
   * O mecanismo de lock utilizado para sincronizar o acesso ao cache de
   * credenciai.
   */
  private Lock lock;
  /**
   * O timer respons�vel por agendar a execu��o da tarefa de valida��o das
   * credenciais do cache.
   */
  private Timer timer;

  /**
   * Cria o interceptador para valida��o de credenciais que s�o armazenadas em
   * cache.
   */
  private CachedCredentialValidatorServerInterceptor() {
    this.credentials = new LinkedList<CredentialWrapper>();
    this.lock = new ReentrantLock();
    this.timer = new Timer();
    timer.schedule(new CredentialValidatorTask(), TASK_DELAY);
    Log.INTERCEPTORS.config("Cache de credenciais com capacidade m�xima de "
      + MAXIMUM_CREDENTIALS_CACHE_SIZE + " credenciais.");
    Log.INTERCEPTORS.config("Revalida��o do cache realizada a cada "
      + TASK_DELAY + " milisegundos.");
  }

  /**
   * Obt�m a inst�ncia �nica do interceptador.
   * 
   * @return A inst�ncia �nica do interceptador.
   */
  public static CachedCredentialValidatorServerInterceptor getInstance() {
    if (instance == null) {
      instance = new CachedCredentialValidatorServerInterceptor();
    }
    return instance;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void receive_request(ServerRequestInfo ri) throws ForwardRequest {
    Openbus bus = Openbus.getInstance();
    Credential interceptedCredential = bus.getInterceptedCredential();
    if (interceptedCredential == null) {
      Log.INTERCEPTORS.info("Nenhuma credencial foi interceptada.");
      throw new NO_PERMISSION(0, CompletionStatus.COMPLETED_NO);
    }

    CredentialWrapper wrapper = new CredentialWrapper(interceptedCredential);
    this.lock.lock();
    try {
      if (this.credentials.remove(wrapper)) {
        Log.INTERCEPTORS.fine("A credencial interceptada " + wrapper
          + " est� no cache.");
        this.credentials.offerLast(wrapper);
        return;
      }
    }
    finally {
      this.lock.unlock();
    }

    Log.INTERCEPTORS.fine("A credencial interceptada " + wrapper
      + " n�o est� no cache.");

    IAccessControlService acs = bus.getAccessControlService();
    boolean isValid;
    try {
      isValid = acs.isValid(interceptedCredential);
    }
    catch (SystemException e) {
      Log.INTERCEPTORS.severe("Erro ao tentar validar uma credencial.", e);
      throw new NO_PERMISSION(0, CompletionStatus.COMPLETED_NO);
    }

    if (isValid) {
      Log.INTERCEPTORS.info("A credencial interceptada " + wrapper
        + " � v�lida.");
      this.lock.lock();
      try {
        if (this.credentials.size() == MAXIMUM_CREDENTIALS_CACHE_SIZE) {
          Log.INTERCEPTORS.info("O cache est� cheio.");
          this.credentials.pollFirst();
        }
        this.credentials
          .offerLast(new CredentialWrapper(interceptedCredential));
      }
      finally {
        this.lock.unlock();
      }
    }
    else {
      Log.INTERCEPTORS.info("A credencial interceptada " + wrapper
        + " n�o � v�lida.");
      throw new NO_PERMISSION(0, CompletionStatus.COMPLETED_NO);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void receive_request_service_contexts(ServerRequestInfo ri)
    throws ForwardRequest {
    // Nada a ser feito.
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send_exception(ServerRequestInfo ri) throws ForwardRequest {
    // Nada a ser feito.
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send_other(ServerRequestInfo ri) throws ForwardRequest {
    // Nada a ser feito.
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void send_reply(ServerRequestInfo ri) {
    // Nada a ser feito.
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void destroy() {
    this.timer.cancel();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String name() {
    return CachedCredentialValidatorServerInterceptor.class.getName();
  }

  /**
   * Tarefa de valida��o das credenciais armazenadas em um cache.
   * 
   * @author Tecgraf/PUC-Rio
   */
  private class CredentialValidatorTask extends TimerTask {
    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
      lock.lock();
      try {
        if (credentials.size() > 0) {
          Log.INTERCEPTORS
            .info("Executando a tarefa de valida��o de credenciais.");
          CredentialWrapper[] credentialWrapperArray =
            credentials.toArray(new CredentialWrapper[0]);
          Credential[] credentialArray =
            new Credential[credentialWrapperArray.length];
          for (int i = 0; i < credentialArray.length; i++) {
            credentialArray[i] = credentialWrapperArray[i].getCredential();
          }
          Openbus bus = Openbus.getInstance();
          IAccessControlService acs = bus.getAccessControlService();
          boolean[] results;
          try {
            results = acs.areValid(credentialArray);
          }
          catch (SystemException e) {
            Log.INTERCEPTORS
              .severe("Erro ao tentar validar as credenciais.", e);
            return;
          }
          for (int i = 0; i < results.length; i++) {
            if (results[i] == false) {
              Log.INTERCEPTORS.finest("A credencial "
                + credentialWrapperArray[i] + " n�o � mais v�lida.");
              credentials.remove(credentialWrapperArray[i]);
            }
            else {
              Log.INTERCEPTORS.finest("A credencial "
                + credentialWrapperArray[i] + " ainda � v�lida.");
            }
          }
        }
        else {
          Log.INTERCEPTORS
            .info("Tarefa de valida��o de credenciais n�o foi executada pois n�o existem credenciais no cache.");
        }
      }
      finally {
        lock.unlock();
      }
    }
  }
}
