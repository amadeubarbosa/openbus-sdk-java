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
 * Implementa a política de validação de credenciais interceptadas em um
 * servidor, armazenando as credenciais já validadas em um cache para futuras
 * consultas.
 * 
 * @author Tecgraf/PUC-Rio
 */
final class CachedCredentialValidatorServerInterceptor extends LocalObject
  implements ServerRequestInterceptor {
  /**
   * Intervalo de tempo para execução da tarefa de validação das credenciais do
   * cache.
   */
  private static long TASK_DELAY = 5 * 60 * 1000; // 5 minutos 
  /**
   * Tamanho máximo do cache de credenciais.
   */
  private static int MAXIMUM_CREDENTIALS_CACHE_SIZE = 20;
  /**
   * A instância única do interceptador.
   */
  private static CachedCredentialValidatorServerInterceptor instance;
  /**
   * O cache de credenciais, que é mantido utilizando-se a política LRU (Least
   * Recently Used).
   */
  private Deque<CredentialWrapper> credentials;
  /**
   * O mecanismo de lock utilizado para sincronizar o acesso ao cache de
   * credenciai.
   */
  private Lock lock;
  /**
   * O timer responsável por agendar a execução da tarefa de validação das
   * credenciais do cache.
   */
  private Timer timer;

  /**
   * Cria o interceptador para validação de credenciais que são armazenadas em
   * cache.
   */
  private CachedCredentialValidatorServerInterceptor() {
    this.credentials = new LinkedList<CredentialWrapper>();
    this.lock = new ReentrantLock();
    this.timer = new Timer();
    timer.schedule(new CredentialValidatorTask(), TASK_DELAY);
    Log.INTERCEPTORS.config("Cache de credenciais com capacidade máxima de "
      + MAXIMUM_CREDENTIALS_CACHE_SIZE + " credenciais.");
    Log.INTERCEPTORS.config("Revalidação do cache realizada a cada "
      + TASK_DELAY + " milisegundos.");
  }

  /**
   * Obtém a instância única do interceptador.
   * 
   * @return A instância única do interceptador.
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
          + " está no cache.");
        this.credentials.offerLast(wrapper);
        return;
      }
    }
    finally {
      this.lock.unlock();
    }

    Log.INTERCEPTORS.fine("A credencial interceptada " + wrapper
      + " não está no cache.");

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
        + " é válida.");
      this.lock.lock();
      try {
        if (this.credentials.size() == MAXIMUM_CREDENTIALS_CACHE_SIZE) {
          Log.INTERCEPTORS.info("O cache está cheio.");
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
        + " não é válida.");
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
   * Tarefa de validação das credenciais armazenadas em um cache.
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
            .info("Executando a tarefa de validação de credenciais.");
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
                + credentialWrapperArray[i] + " não é mais válida.");
              credentials.remove(credentialWrapperArray[i]);
            }
            else {
              Log.INTERCEPTORS.finest("A credencial "
                + credentialWrapperArray[i] + " ainda é válida.");
            }
          }
        }
        else {
          Log.INTERCEPTORS
            .info("Tarefa de validação de credenciais não foi executada pois não existem credenciais no cache.");
        }
      }
      finally {
        lock.unlock();
      }
    }
  }
}
