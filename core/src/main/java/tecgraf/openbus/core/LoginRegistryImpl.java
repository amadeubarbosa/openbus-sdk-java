package tecgraf.openbus.core;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tecgraf.openbus.core.v2_1.OctetSeqHolder;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.UnauthorizedOperation;
import tecgraf.openbus.core.v2_1.services.access_control.*;
import tecgraf.openbus.*;
import tecgraf.openbus.Connection;
import tecgraf.openbus.LoginObserver;
import tecgraf.openbus.LoginRegistry;
import tecgraf.openbus.retry.RetryContext;
import tecgraf.openbus.retry.RetryTaskPool;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Representação local do registro de logins
 * 
 * @author Tecgraf
 */
// Essa classe faz multiplexação de um observador de logins para vários
// observadores do usuário
class LoginRegistryImpl extends LoginObserverPOA implements LoginRegistry {
  private final OpenBusContextImpl context;
  private final ConnectionImpl conn;
  private final POA poa;
  /** Mutex que gerencia o acesso ao estado do registro de logins. */
  private final Object lock = new Object();
  private tecgraf.openbus.core.v2_1.services.access_control.LoginRegistry
    registry;
  /** Mecanismo de execução de tarefas com suporte a retentativas */
  private final RetryTaskPool pool;
  private tecgraf.openbus.core.v2_1.services.access_control.LoginObserver
    observer;
  private ListenableFuture<LoginObserverSubscription> futureSub;
  private ListenableFuture<Void> futureRemove;
  private ListenableFuture<LoginObserverSubscription> futureReLogin;
  private LoginObserverSubscription sub;
  private List<LoginSubscriptionImpl> subs;
  private final long retryDelay;
  private final TimeUnit delayUnit;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  protected LoginRegistryImpl(OpenBusContextImpl context, ConnectionImpl conn,
                              POA poa, RetryTaskPool pool, long interval, TimeUnit unit) {
    this.context = context;
    this.conn = conn;
    this.poa = poa;
    this.pool = pool;
    this.retryDelay = interval;
    this.delayUnit = unit;
  }

  @Override
  public Connection connection() {
    return conn;
  }

  @Override
  public List<LoginInfo> allLogins() throws ServiceFailure,
    UnauthorizedOperation {
    tecgraf.openbus.core.v2_1.services.access_control.LoginRegistry registry
      = registry();
    if (registry == null) {
      return new ArrayList<>();
    }
    Connection prev = context.getCurrentConnection();
    try {
      context.setCurrentConnection(conn);
      return convertLoginArrayToList(registry.getAllLogins());
    } finally {
      context.setCurrentConnection(prev);
    }
  }

  @Override
  public List<LoginInfo> entityLogins(String entity) throws ServiceFailure,
    UnauthorizedOperation {
    tecgraf.openbus.core.v2_1.services.access_control.LoginRegistry registry
      = registry();
    if (registry == null) {
      return new ArrayList<>();
    }
    Connection prev = context.getCurrentConnection();
    try {
      context.setCurrentConnection(conn);
      return convertLoginArrayToList(registry.getEntityLogins(entity));
    } finally {
      context.setCurrentConnection(prev);
    }
  }

  @Override
  public boolean invalidateLogin(String loginId) throws ServiceFailure,
    UnauthorizedOperation {
    tecgraf.openbus.core.v2_1.services.access_control.LoginRegistry registry
      = registry();
    Connection prev = context.getCurrentConnection();
    try {
      context.setCurrentConnection(conn);
      return registry != null && registry.invalidateLogin(loginId);
    } finally {
      context.setCurrentConnection(prev);
    }
  }

  @Override
  public LoginInfo loginInfo(String loginId, OctetSeqHolder pubkey) throws
    InvalidLogins, ServiceFailure {
    tecgraf.openbus.core.v2_1.services.access_control.LoginRegistry registry
      = registry();
    if (registry == null) {
      return new LoginInfo();
    }
    Connection prev = context.getCurrentConnection();
    try {
      context.setCurrentConnection(conn);
      return registry.getLoginInfo(loginId, pubkey);
    } finally {
      context.setCurrentConnection(prev);
    }
  }

  @Override
  public int loginValidity(String loginId) throws ServiceFailure {
    tecgraf.openbus.core.v2_1.services.access_control.LoginRegistry registry
      = registry();
    if (registry == null) {
      return -1;
    }
    Connection prev = context.getCurrentConnection();
    try {
      context.setCurrentConnection(conn);
      return registry.getLoginValidity(loginId);
    } finally {
      context.setCurrentConnection(prev);
    }
  }

  @Override
  public LoginSubscription subscribeObserver(LoginObserver callback) throws ServantNotActive, WrongPolicy {
    tecgraf.openbus.core.v2_1.services.access_control.LoginRegistry
    registry = registry();
    if (registry == null) {
      return null;
    }
    Connection prev = context.getCurrentConnection();
    try {
      context.setCurrentConnection(conn);
      LoginSubscriptionImpl ret = new LoginSubscriptionImpl(callback, this);
      synchronized (lock) {
        List<LoginSubscriptionImpl> subs = subs();
        if (subs == null) {
          return null;
        }
        subs.add(ret);
        if (subs.size() == 1) {
          this.observer = LoginObserverHelper.narrow(poa.servant_to_reference
            (this));
          ReLoginTask task = new ReLoginTask(registry, observer, null);
          futureSub = pool.doTask(task, new RetryContext(retryDelay,
            delayUnit));
          Futures.addCallback(futureSub, new
              FutureCallback<LoginObserverSubscription>() {
                @Override
                public void onFailure(Throwable ex) {
                  // so deve entrar aqui se a aplicação escolheu fazer um logout, ou se
                  // pararam as retentativas do RetryTask.
                  logger.error("Erro ao inserir o observador de logins no " +
                    "barramento.", ex);
                  lock.notifyAll();
                }

                @Override
                public void onSuccess(LoginObserverSubscription sub) {
                  synchronized (lock) {
                    // não há como ter dois sucessos ao mesmo tempo, portanto
                    // posso passar por cima do valor direto sem testar.
                    LoginRegistryImpl.this.sub = sub;
                    futureSub = null;
                    // aviso quem estiver dormindo para dar chance de obter sub
                    // novamente
                    lock.notifyAll();
                    logger.info("Observador de logins cadastrado no barramento.");
                  }
                }
              },
            pool.pool());
        }
      }
      return ret;
    } finally {
      context.setCurrentConnection(prev);
    }
  }

  @Override
  public void entityLogout(final LoginInfo login) {
    final List<LoginSubscriptionImpl> subs;
    synchronized (lock) {
      if (this.subs == null) {
        return;
      }
      // cópia rasa, só para liberar o mutex
      subs = new ArrayList<>(this.subs);
    }
    Thread t = new Thread(() -> {
      context.setCurrentConnection(conn);
      for (LoginSubscriptionImpl sub1 : subs) {
        try {
          sub1.observer().entityLogout(login);
        } catch (Exception e) {
          logger.error("Erro ao avisar um observador da aplicação de que o" +
            " login " + login.id + " da entidade " + login.entity + " foi " +
            "desfeito.", e);
        }
      }
    });
    t.start();
  }

  protected void fireEvent(LoginEvent e, LoginInfo newLogin) {
    switch (e) {
      case LOGGED_IN:
        onLogin();
        break;
      case LOGGED_OUT:
        onLogout();
        break;
      case RELOGIN:
        onRelogin(newLogin);
        break;
    }
  }

  protected boolean watchLogin(String loginId) throws ServiceFailure {
    LoginObserverSubscription sub = sub();
    Connection prev = context.getCurrentConnection();
    try {
      context.setCurrentConnection(conn);
      while (true) {
        try {
          return sub != null && sub.watchLogin(loginId);
        } catch (OBJECT_NOT_EXIST e) {
          // ao tentar pegar novamente a referência, detecta se foi feito logout.
          sub = sub();
        }
      }
    } finally {
      context.setCurrentConnection(prev);
    }
  }

  protected void forgetLogin(String loginId) throws ServiceFailure {
    LoginObserverSubscription sub = sub();
    Connection prev = context.getCurrentConnection();
    try {
      context.setCurrentConnection(conn);
      while (true) {
        try {
          if (sub == null) {
            return;
          }
          sub.forgetLogin(loginId);
          break;
        } catch (OBJECT_NOT_EXIST e) {
          // ao tentar pegar novamente a referência, detecta se foi feito
          // relogin e refaz a chamada. Caso tenha sido uma remoção forçada por
          // um admin, um novo OBJECT_NOT_EXIST será lançado e repassado para a
          // aplicação.
          sub = sub();
        }
      }
    } finally {
      context.setCurrentConnection(prev);
    }
  }

  protected void watchLogins(List<String> loginIds) throws ServiceFailure,
    InvalidLogins {
    LoginObserverSubscription sub = sub();
    Connection prev = context.getCurrentConnection();
    try {
      context.setCurrentConnection(conn);
      while (true) {
        try {
          if (sub == null) {
            return;
          }
          sub.watchLogins(loginIds.toArray(new String[loginIds.size()]));
          break;
        } catch (OBJECT_NOT_EXIST e) {
          // ao tentar pegar novamente a referência, detecta se foi feito
          // relogin e refaz a chamada. Caso tenha sido uma remoção forçada por
          // um admin, um novo OBJECT_NOT_EXIST será lançado e repassado para a
          // aplicação.
          sub = sub();
        }
      }
    } finally {
      context.setCurrentConnection(prev);
    }
  }

  protected void forgetLogins(List<String> loginIds) throws ServiceFailure {
    LoginObserverSubscription sub = sub();
    Connection prev = context.getCurrentConnection();
    try {
      context.setCurrentConnection(conn);
      while (true) {
        try {
          if (sub == null) {
            return;
          }
          sub.forgetLogins(loginIds.toArray(new String[loginIds.size()]));
          break;
        } catch (OBJECT_NOT_EXIST e) {
          // ao tentar pegar novamente a referência, detecta se foi feito
          // relogin e refaz a chamada. Caso tenha sido uma remoção forçada por
          // um admin, um novo OBJECT_NOT_EXIST será lançado e repassado para a
          // aplicação.
          sub = sub();
        }
      }
    } finally {
      context.setCurrentConnection(prev);
    }
  }

  protected List<LoginInfo> getWatchedLogins() {
    LoginObserverSubscription sub = sub();
    Connection prev = context.getCurrentConnection();
    try {
      context.setCurrentConnection(conn);
      while (true) {
        try {
          if (sub == null) {
            return new ArrayList<>();
          }
          return convertLoginArrayToList(sub.getWatchedLogins());
        } catch (OBJECT_NOT_EXIST e) {
          // ao tentar pegar novamente a referência, detecta se foi feito
          // relogin e refaz a chamada. Caso tenha sido uma remoção forçada por
          // um admin, um novo OBJECT_NOT_EXIST será lançado e repassado para a
          // aplicação.
          sub = sub();
        }
      }
    } finally {
      context.setCurrentConnection(prev);
    }
  }

  protected void remove(LoginSubscriptionImpl localSub) {
    synchronized (lock) {
      List<LoginSubscriptionImpl> subs = subs();
      if (subs == null) {
        return;
      }
      subs.remove(localSub);
      if (subs.isEmpty()) {
        final AtomicBoolean needRemove = new AtomicBoolean(false);
        if (futureSub != null) {
          // tenta cancelar subscrição
          if (!futureSub.cancel(false)) {
            // a subscrição já havia terminado mas entrou aqui no cancelamento
            // antes de registrar o sucesso da chamada
            needRemove.set(true);
          } else {
            // como o cancel não interrompe a tarefa, ela pode conseguir terminar.
            Futures.addCallback(futureSub, new
              FutureCallback<LoginObserverSubscription> () {
                @Override
                public void onFailure(Throwable ex) {
                  // a tarefa foi cancelada corretamente ou falhou, então não é
                  // necessário fazer nada.
                }

                @Override
                public void onSuccess(LoginObserverSubscription remote) {
                  // a tarefa não foi cancelada a tempo. Cancelar manualmente.
                  needRemove.set(true);
                }
              },
              pool.pool());
          }
        } else {
          needRemove.set(true);
        }
        if (needRemove.get()) {
          LoginObserverSubscription sub = sub();
          if (sub == null) {
            return;
          }
          LoginSubRemovalTask task = new LoginSubRemovalTask(sub);
          futureRemove = pool.doTask(task, new RetryContext(retryDelay,
            delayUnit));
          Futures.addCallback(futureRemove, new
              FutureCallback<Void>() {
                @Override
                public void onFailure(Throwable ex) {
                  // so deve entrar aqui se a aplicação escolheu fazer um logout, ou se
                  // pararam as retentativas do RetryTask.
                  logger.error("Erro ao remover o observador de logins do " +
                    "barramento.", ex);
                }

                @Override
                public void onSuccess(Void nothing) {
                  synchronized (lock) {
                    futureSub = null;
                    futureRemove = null;
                  }
                  logger.info("Observador de login removido do barramento.");
                }
              },
            pool.pool());
          deactivateObserver();
        }
      }
    }
  }

  private void onLogin() {
    synchronized (lock) {
      Connection prev = context.getCurrentConnection();
      try {
        context.setCurrentConnection(conn);
        registry = context.getLoginRegistry();
        if (subs == null) {
          subs = new ArrayList<>();
        }
        lock.notifyAll();
      } finally {
        context.setCurrentConnection(prev);
      }
    }
  }

  /***
   * Chamado apenas em caso de logout por escolha da aplicação
   */
  private void onLogout() {
    synchronized (lock) {
      clearLoginState();
      if (this.subs != null) {
        this.subs.clear();
      }
      this.subs = null;
      deactivateObserver();
      this.lock.notifyAll();
    }
  }

  private void deactivateObserver() {
    synchronized (lock) {
      if (observer != null) {
        try {
          byte[] oid = poa.reference_to_id(observer);
          poa.deactivate_object(oid);
          observer = null;
        } catch (Exception e) {
          logger.warn("Erro ao desativar o objeto observador de logins.", e);
        }
      }
    }
  }

  private void clearLoginState() {
    synchronized (lock) {
      this.registry = null;
      this.sub = null;
      // não interrompo pois pode estar em uma chamada remota/JacORB. Se o
      // cancelamento proceder, gera um evento fail da callback do objeto futuro.
      if (futureSub != null) {
        this.futureSub.cancel(false);
      }
      this.futureSub = null;

      if (futureRemove != null) {
        this.futureRemove.cancel(false);
      }
      this.futureRemove = null;

      // não nulifico futureReLogin pois isso é usado como critério para
      // acordar as threads de relogin
      if (futureReLogin != null) {
        this.futureReLogin.cancel(false);
      }
    }
  }

  private void onRelogin(LoginInfo newLogin) {
    // O relogin deve ser feito de forma síncrona (pode ser feito com
    // tarefas mas deve-se sincronizar ao fim). Dado o funcionamento da
    // conexão do SDK, só há como ter duas chamadas concorrentes desse método
    // caso enquanto uma thread estiver esperando pela tarefa de subscrição
    // de observador, o login for novamente perdido e refeito por outra thread.
    // Etapas:
    ListenableFuture<LoginObserverSubscription> future;
    synchronized (lock) {
      // sem subscrições, não há nada a fazer.
      if (subs == null || subs.size() == 0) {
        return;
      }
      // 0) testar se há um relogin em andamento. Se houver, cancelar pois
      // nesse caso houve perda de login enquanto a subscrição ainda estava
      // sendo feita. Isso é importante pois caso a subscrição já tenha sido
      // refeita, ela não valerá mais e as tarefas de watch nunca terminarão
      // pois ficarão num loop de OBJ_NOT_EXISTS.
      while (futureReLogin != null) {
        // o método abaixo já cancela o relogin anterior em andamento
        clearLoginState();
        try {
          // aguardo tarefa de relogin anterior terminar
          lock.wait();
        } catch (InterruptedException e) {
          logInterruptError(e);
        }
        // devo testar de novo pois posso ter sido acordado tanto por ter
        // feito o relogin como por um logout. Se foi logout, não terei login
        // e então desisto. Se houve um novo relogin, o login atual não será
        // o mesmo que o que recebi por parâmetro, então desisto tb.
        LoginInfo currLogin = conn.login();
        if ((currLogin == null) || (!newLogin.id.equals(currLogin.id))) {
          return;
        }
      }
      // refaço os passos de um login pois chamei clearstate.
      onLogin();
      // 1) lançar assincronamente uma retrytask para refazer o observador e
      // watchs de logins
      Set<LoginInfo> watchedLogins = new HashSet<>();
      // insere todos os logins observados sem repetições
      for (LoginSubscriptionImpl sub : subs) {
        watchedLogins.addAll(sub.watchedLogins());
      }
      futureReLogin = future = pool.doTask(new ReLoginTask(registry, observer,
        watchedLogins), new RetryContext(retryDelay, delayUnit));
    }
    // 2) ressincronizar fora da região crítica, para dar chance a outros
    // relogins de cancelarem essa tarefa.
    LoginObserverSubscription sub;
    try {
      sub = Uninterruptibles.getUninterruptibly(future);
    } catch (ExecutionException | CancellationException e) {
      // Só entra aqui se a aplicação fez logout ou um outro relogin cancelou
      // esse. Basta desistir que o SDK cuida do login atual depois.
      logger.warn("Erro ao reinserir o observador de logins do " +
        "barramento devido a um logout ou relogin. Esse erro provavelmente " +
        "pode ser ignorado.", e);
      synchronized (lock) {
        lock.notifyAll();
      }
      return;
    }
    // 3) redefinir sub já com logins watched, nulificar futureReLogin e
    // notificar quem estiver esperando.
    synchronized (lock) {
      if (futureReLogin != null && !futureReLogin.isCancelled()) {
        // se a sub recebida for null é porque o objeto foi removido no
        // barramento por perda de login antes que os watch pudessem ser
        // concluídos.
        if (sub != null) {
          this.sub = sub;
        }
        futureReLogin = null;
      }
      lock.notifyAll();
    }
  }

  private tecgraf.openbus.core.v2_1.services.access_control.LoginRegistry
    registry() {
    synchronized (lock) {
      while (registry == null) {
        if (futureReLogin == null) {
          logger.error("Não há login para realizar a chamada.");
          throw new NO_PERMISSION(NoLoginCode.value, CompletionStatus
            .COMPLETED_NO);
        }
        try {
          lock.wait();
        } catch (InterruptedException e) {
          logInterruptError(e);
          Thread.currentThread().interrupt();
          return null;
        }
      }
      return registry;
    }
  }

  private LoginObserverSubscription sub() {
    synchronized (lock) {
      while (sub == null) {
        if (futureReLogin == null) {
          if (futureSub.isCancelled()) {
            //situação de logout
            throw new NO_PERMISSION(NoLoginCode.value, CompletionStatus
              .COMPLETED_NO);
          } else if (futureSub.isDone()) {
            // não há relogin nem tarefa de subscrição em andamento, então
            // a subscrição foi removida do servidor
            throw new OBJECT_NOT_EXIST("A subscrição foi removida do " +
              "barramento por uma ação diferente de um logout.");
          }
        }
        try {
          // aguardo tarefa obter a subscrição
          lock.wait();
        } catch (InterruptedException e) {
          logInterruptError(e);
          Thread.currentThread().interrupt();
          return null;
        }
      }
      return sub;
    }
  }

  private List<LoginSubscriptionImpl> subs() {
    synchronized (lock) {
      while (subs == null) {
        if (futureReLogin == null) {
          logger.error("Não há login para realizar a chamada.");
          throw new NO_PERMISSION(NoLoginCode.value, CompletionStatus
            .COMPLETED_NO);
        }
        try {
          lock.wait();
        } catch (InterruptedException e) {
          logInterruptError(e);
          Thread.currentThread().interrupt();
          return null;
        }
      }
      return subs;
    }
  }

  private void logInterruptError(Exception e) {
    logger.error("Interrupção não esperada ao refazer um login. " +
      "Verifique se sua aplicação está tentando interromper a thread " +
      "quando esta está executando código alheio, como do SDK OpenBus " +
      "ou do JacORB.", e);
  }

  private static List<LoginInfo> convertLoginArrayToList(LoginInfo[] infos) {
    List<LoginInfo> ret = new ArrayList<>();
    Collections.addAll(ret, infos);
    return ret;
  }

  private static String[] convertLoginCollectionToIdArray(Collection<LoginInfo>
                                                            logins) {
    String[] ids = new String[logins.size()];
    int i = 0;
    for (LoginInfo login : logins) {
      ids[i] = login.id;
      i++;
    }
    return ids;
  }

  private class LoginSubRemovalTask implements Callable<Void> {
    private LoginObserverSubscription sub;

    public LoginSubRemovalTask(LoginObserverSubscription sub) {
      this.sub = sub;
    }

    @Override
    public Void call() throws ServiceFailure {
      try {
        context.setCurrentConnection(conn);
        sub.remove();
      } catch (OBJECT_NOT_EXIST ignored) {}
      return null;
    }
  }

  private class ReLoginTask implements Callable<LoginObserverSubscription> {
    private tecgraf.openbus.core.v2_1.services.access_control.LoginRegistry
      registry;
    private final tecgraf.openbus.core.v2_1.services.access_control
      .LoginObserver observer;
    private final Set<LoginInfo> watchedLogins;

    public ReLoginTask(tecgraf.openbus.core.v2_1.services.access_control
      .LoginRegistry registry, tecgraf.openbus.core.v2_1.services
      .access_control.LoginObserver observer, Set<LoginInfo> watchedLogins) {
      this.registry = registry;
      this.observer = observer;
      this.watchedLogins = watchedLogins;
    }

    @Override
    public LoginObserverSubscription call() throws ServiceFailure {
      context.setCurrentConnection(conn);
      LoginObserverSubscription sub = registry.subscribeObserver(observer);
      if (watchedLogins != null) {
        String[] logins;
        synchronized (lock) {
          logins = convertLoginCollectionToIdArray(watchedLogins);
        }
        try {
          sub.watchLogins(logins);
        } catch (final InvalidLogins e) {
          synchronized (lock) {
            List<LoginSubscriptionImpl> subs = subs();
            if (subs != null) {
              for (final LoginSubscriptionImpl loginSubscription : subs) {
                // arrisco criar várias threads para não compartilhar subs
                Thread t = new Thread(() -> {
                  loginSubscription.nonExistentLogins(e.loginIds);
                });
                t.start();
              }
            }
          }
          logger.warn("Alguns logins não puderam ser re-observados pois saíram " +
            "do barramento.", e);
        } catch (OBJECT_NOT_EXIST e) {
          // significa que minha subscrição não existe mais. Caso isto tenha
          // ocorrido por perda de login um novo relogin será feito e tratará
          // do problema novamente. Caso a sub tenha sido removida por um
          // admin, a sub local será nula e se a aplicação tentar obtê-la vai
          // receber uma exceção OBJECT_NOT_EXIST lançada pelo método sub().
          sub = null;
        } catch (Exception e) {
          // caso outro erro ocorra, tento como best-effort remover a
          // subscrição e deixo a tarefa toda ser repetida.
          try {
            sub.remove();
          } catch (Exception ignored) {}
          throw e;
        }
      }
      return sub;
    }
  }
}
