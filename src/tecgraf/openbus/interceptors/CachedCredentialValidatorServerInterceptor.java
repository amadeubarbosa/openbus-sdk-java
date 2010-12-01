/*
 * $Id $
 */
package tecgraf.openbus.interceptors;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tecgraf.openbus.Openbus;
import tecgraf.openbus.access_control_service.CredentialWrapper;
import tecgraf.openbus.core.v1_05.access_control_service.Credential;
import tecgraf.openbus.core.v1_05.access_control_service.IAccessControlService;

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
  private LinkedList<CredentialWrapper> credentials;
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

    Logger logger = LoggerFactory.getLogger(ServerInterceptor.class);
    logger.debug(
      "Cache de credenciais com capacidade máxima de {} credenciais.",
      MAXIMUM_CREDENTIALS_CACHE_SIZE);
    logger.debug("Revalidação do cache realizada a cada {} milisegundos.",
      TASK_DELAY);
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
  public void receive_request(ServerRequestInfo ri) throws ForwardRequest {
    Logger logger = LoggerFactory.getLogger(ServerInterceptor.class);

    String interceptedServant = ri.target_most_derived_interface();
    String interceptedOperation = ri.operation();

    Openbus bus = Openbus.getInstance();
    if (!bus.isInterceptable(interceptedServant, interceptedOperation)) {
      return;
    }

    Credential interceptedCredential = bus.getInterceptedCredential();
    if (interceptedCredential == null) {
      throw new NO_PERMISSION(100, CompletionStatus.COMPLETED_NO);
    }

    CredentialWrapper interceptedWrapper =
      new CredentialWrapper(interceptedCredential);
    this.lock.lock();
    try {
      if (this.credentials.remove(interceptedWrapper)) {
        logger.debug("A credencial {} já está no cache.", interceptedWrapper);
        this.credentials.addLast(interceptedWrapper);
        return;
      }
    }
    finally {
      this.lock.unlock();
    }
    logger.debug("A credencial {} não está no cache.", interceptedWrapper);

    IAccessControlService acs = bus.getAccessControlService();
    boolean isValid;
    try {
      isValid = acs.isValid(interceptedCredential);
    }
    catch (SystemException e) {
      logger.error("Erro ao tentar validar a credencial " + interceptedWrapper
        + ".", e);
      throw new NO_PERMISSION(0, CompletionStatus.COMPLETED_NO);
    }

    if (isValid) {
      logger.info("A credencial {} é válida.", interceptedWrapper);
      this.lock.lock();
      try {
        if (this.credentials.size() == MAXIMUM_CREDENTIALS_CACHE_SIZE) {
          logger.debug("O cache está cheio.");
          this.credentials.removeFirst();
        }
        this.credentials.addLast(new CredentialWrapper(interceptedCredential));
      }
      finally {
        this.lock.unlock();
      }
    }
    else {
      logger.info("A credencial {} não é válida.", interceptedWrapper);
      throw new NO_PERMISSION(0, CompletionStatus.COMPLETED_NO);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void receive_request_service_contexts(ServerRequestInfo ri)
    throws ForwardRequest {
    // Nada a ser feito.
  }

  /**
   * {@inheritDoc}
   */
  public void send_exception(ServerRequestInfo ri) throws ForwardRequest {
    // Nada a ser feito.
  }

  /**
   * {@inheritDoc}
   */
  public void send_other(ServerRequestInfo ri) throws ForwardRequest {
    // Nada a ser feito.
  }

  /**
   * {@inheritDoc}
   */
  public void send_reply(ServerRequestInfo ri) {
    // Nada a ser feito.
  }

  /**
   * {@inheritDoc}
   */
  public void destroy() {
    this.timer.cancel();
  }

  /**
   * {@inheritDoc}
   */
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
      Logger logger = LoggerFactory.getLogger(ServerInterceptor.class);

      lock.lock();
      try {
        if (credentials.size() > 0) {
          logger.debug("Executando a tarefa de validação de credenciais.");
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
            logger.error("Erro ao tentar validar as credenciais.", e);
            return;
          }
          for (int i = 0; i < results.length; i++) {
            if (results[i] == false) {
              logger.debug("A credencial {} não é mais válida.",
                credentialWrapperArray[i]);
              credentials.remove(credentialWrapperArray[i]);
            }
            else {
              logger.debug("A credencial {} ainda é válida.",
                credentialWrapperArray[i]);
            }
          }
        }
        else {
          logger
            .debug("Tarefa de validação de credenciais não foi executada pois não existem credenciais no cache.");
        }
      }
      finally {
        lock.unlock();
      }
    }
  }
}
