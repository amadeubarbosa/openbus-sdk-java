package tecgraf.openbus.core;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import tecgraf.openbus.Connection;
import tecgraf.openbus.LoginSubscription;
import tecgraf.openbus.core.v2_1.OctetSeqHolder;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.UnauthorizedOperation;
import tecgraf.openbus.LoginObserver;
import tecgraf.openbus.LoginRegistry;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidLogins;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_1.services.access_control.LoginObserverHelper;
import tecgraf.openbus.core.v2_1.services.access_control.LoginObserverPOA;
import tecgraf.openbus.core.v2_1.services.access_control.LoginObserverSubscription;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.retry.RetryContext;
import tecgraf.openbus.retry.RetryTaskPool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Representa��o local do registro de logins
 * 
 * @author Tecgraf
 */
// Essa classe faz multiplexa��o de um observador de logins para v�rios
// observadores do usu�rio
class LoginRegistryImpl extends LoginObserverPOA implements LoginRegistry {
  private final OpenBusContextImpl context;
  private final ConnectionImpl conn;
  private final POA poa;
  /** Mutex que gerencia o acesso ao estado do registro de logins. */
  private final Object lock = new Object();
  private tecgraf.openbus.core.v2_1.services.access_control.LoginRegistry
    registry;
  /** Mecanismo de execu��o de tarefas com suporte a retentativas */
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
  private static final Logger logger = Logger.getLogger(LoginRegistryImpl.class
    .getName());

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
    Connection prev = context.currentConnection();
    try {
      context.currentConnection(conn);
      return convertLoginArrayToList(registry.getAllLogins());
    } finally {
      context.currentConnection(prev);
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
    Connection prev = context.currentConnection();
    try {
      context.currentConnection(conn);
      return convertLoginArrayToList(registry.getEntityLogins(entity));
    } finally {
      context.currentConnection(prev);
    }
  }

  @Override
  public boolean invalidateLogin(String loginId) throws ServiceFailure,
    UnauthorizedOperation {
    tecgraf.openbus.core.v2_1.services.access_control.LoginRegistry registry
      = registry();
    Connection prev = context.currentConnection();
    try {
      context.currentConnection(conn);
      return registry != null && registry.invalidateLogin(loginId);
    } finally {
      context.currentConnection(prev);
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
    Connection prev = context.currentConnection();
    try {
      context.currentConnection(conn);
      return registry.getLoginInfo(loginId, pubkey);
    } finally {
      context.currentConnection(prev);
    }
  }

  @Override
  public int loginValidity(String loginId) throws ServiceFailure {
    tecgraf.openbus.core.v2_1.services.access_control.LoginRegistry registry
      = registry();
    if (registry == null) {
      return -1;
    }
    Connection prev = context.currentConnection();
    try {
      context.currentConnection(conn);
      return registry.getLoginValidity(loginId);
    } finally {
      context.currentConnection(prev);
    }
  }

  @Override
  public LoginSubscription subscribeObserver(LoginObserver callback) throws
    ServantNotActive, WrongPolicy {
    tecgraf.openbus.core.v2_1.services.access_control.LoginRegistry registry
      = registry();
    if (registry == null) {
      return null;
    }
    Connection prev = context.currentConnection();
    try {
      context.currentConnection(conn);
      LoginSubscriptionImpl ret = new LoginSubscriptionImpl(callback, this);
      synchronized (lock) {
        List<LoginSubscriptionImpl> subs = subs();
        if (subs == null) {
          return null;
        }
        subs.add(ret);
        if (sub == null) {
          if (observer == null) {
            this.observer = LoginObserverHelper.narrow(poa
              .servant_to_reference(this));
          }
          ReLoginTask task = new ReLoginTask(registry, observer, null);
          futureSub = pool.doTask(task, new RetryContext(retryDelay,
            delayUnit));
          Futures.addCallback(futureSub, new
              FutureCallback<LoginObserverSubscription>() {
                @Override
                public void onFailure(Throwable ex) {
                  // so deve entrar aqui se a aplica��o escolheu fazer um
                  // logout, ou se pararam as retentativas do RetryTask.
                  logger.log(Level.SEVERE, "Erro ao inserir o observador de " +
                    "logins no barramento.", ex);
                  lock.notifyAll();
                }

                @Override
                public void onSuccess(LoginObserverSubscription sub) {
                  synchronized (lock) {
                    // n�o h� como ter dois sucessos ao mesmo tempo, portanto
                    // posso passar por cima do valor direto sem testar.
                    LoginRegistryImpl.this.sub = sub;
                    futureSub = null;
                    // aviso quem estiver dormindo para dar chance de obter sub
                    // novamente
                    lock.notifyAll();
                    logger.info("Observador de logins cadastrado no " +
                      "barramento.");
                  }
                }
              },
            pool.pool());
        }
      }
      return ret;
    } finally {
      context.currentConnection(prev);
    }
  }

  @Override
  public void entityLogout(final LoginInfo login) {
    final List<LoginSubscriptionImpl> subs;
    synchronized (lock) {
      if (this.subs == null) {
        return;
      }
      // c�pia rasa, s� para liberar o mutex
      subs = new ArrayList<>(this.subs);
    }
    Thread t = new Thread(() -> {
      context.currentConnection(conn);
      for (LoginSubscriptionImpl sub1 : subs) {
        try {
          sub1.observer().entityLogout(login);
          sub1.forgetLogin(login.id);
        } catch (Exception e) {
          logger.log(Level.SEVERE, "Erro ao avisar um observador da aplica��o" +
            " de que o login " + login.id + " da entidade " + login.entity +
            " foi desfeito.", e);
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
    Connection prev = context.currentConnection();
    try {
      context.currentConnection(conn);
      while (true) {
        try {
          return sub != null && sub.watchLogin(loginId);
        } catch (OBJECT_NOT_EXIST e) {
          // ao tentar pegar novamente a refer�ncia, detecta se foi feito logout.
          sub = sub();
        }
      }
    } finally {
      context.currentConnection(prev);
    }
  }

  protected void forgetLogin(String loginId) throws ServiceFailure {
    LoginObserverSubscription sub = sub();
    Connection prev = context.currentConnection();
    try {
      context.currentConnection(conn);
      while (true) {
        try {
          if (sub == null) {
            return;
          }
          sub.forgetLogin(loginId);
          break;
        } catch (OBJECT_NOT_EXIST e) {
          // ao tentar pegar novamente a refer�ncia, detecta se foi feito
          // relogin e refaz a chamada. Caso tenha sido uma remo��o for�ada por
          // um admin, um novo OBJECT_NOT_EXIST ser� lan�ado e repassado para a
          // aplica��o.
          sub = sub();
        }
      }
    } finally {
      context.currentConnection(prev);
    }
  }

  protected void watchLogins(List<String> loginIds) throws ServiceFailure,
    InvalidLogins {
    LoginObserverSubscription sub = sub();
    Connection prev = context.currentConnection();
    try {
      context.currentConnection(conn);
      while (true) {
        try {
          if (sub == null) {
            return;
          }
          sub.watchLogins(loginIds.toArray(new String[loginIds.size()]));
          break;
        } catch (OBJECT_NOT_EXIST e) {
          // ao tentar pegar novamente a refer�ncia, detecta se foi feito
          // relogin e refaz a chamada. Caso tenha sido uma remo��o for�ada por
          // um admin, um novo OBJECT_NOT_EXIST ser� lan�ado e repassado para a
          // aplica��o.
          sub = sub();
        }
      }
    } finally {
      context.currentConnection(prev);
    }
  }

  protected void forgetLogins(List<String> loginIds) throws ServiceFailure {
    LoginObserverSubscription sub = sub();
    Connection prev = context.currentConnection();
    try {
      context.currentConnection(conn);
      while (true) {
        try {
          if (sub == null) {
            return;
          }
          sub.forgetLogins(loginIds.toArray(new String[loginIds.size()]));
          break;
        } catch (OBJECT_NOT_EXIST e) {
          // ao tentar pegar novamente a refer�ncia, detecta se foi feito
          // relogin e refaz a chamada. Caso tenha sido uma remo��o for�ada por
          // um admin, um novo OBJECT_NOT_EXIST ser� lan�ado e repassado para a
          // aplica��o.
          sub = sub();
        }
      }
    } finally {
      context.currentConnection(prev);
    }
  }

  protected List<LoginInfo> getWatchedLogins() {
    LoginObserverSubscription sub = sub();
    Connection prev = context.currentConnection();
    try {
      context.currentConnection(conn);
      while (true) {
        try {
          if (sub == null) {
            return new ArrayList<>();
          }
          return convertLoginArrayToList(sub.getWatchedLogins());
        } catch (OBJECT_NOT_EXIST e) {
          // ao tentar pegar novamente a refer�ncia, detecta se foi feito
          // relogin e refaz a chamada. Caso tenha sido uma remo��o for�ada por
          // um admin, um novo OBJECT_NOT_EXIST ser� lan�ado e repassado para a
          // aplica��o.
          sub = sub();
        }
      }
    } finally {
      context.currentConnection(prev);
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
          // tenta cancelar subscri��o
          if (!futureSub.cancel(false)) {
            // a subscri��o j� havia terminado mas entrou aqui no cancelamento
            // antes de registrar o sucesso da chamada
            needRemove.set(true);
          } else {
            // como o cancel n�o interrompe a tarefa, ela pode conseguir terminar.
            Futures.addCallback(futureSub, new
              FutureCallback<LoginObserverSubscription> () {
                @Override
                public void onFailure(Throwable ex) {
                  // a tarefa foi cancelada corretamente ou falhou, ent�o n�o �
                  // necess�rio fazer nada.
                }

                @Override
                public void onSuccess(LoginObserverSubscription remote) {
                  // a tarefa n�o foi cancelada a tempo. Cancelar manualmente.
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
          futureRemove = pool.doTask(task, new OpenBusRetryContext(retryDelay,
            delayUnit));
          Futures.addCallback(futureRemove, new
              FutureCallback<Void>() {
                @Override
                public void onFailure(Throwable ex) {
                  // so deve entrar aqui se a aplica��o escolheu fazer um
                  // logout, ou se pararam as retentativas do RetryTask. Como
                  // o tipo de retentativas � o OpenBusRetryContext, se essa
                  // chamada receber uma UserException, COMM_FAILURE ou
                  // OBJECT_NOT_EXIST, desistir� e chegar� aqui.
                  logger.log(Level.SEVERE, "Erro ao remover o observador de " +
                    "logins do barramento.", ex);
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

  protected RetryTaskPool pool() {
    return pool;
  }

  protected long interval() {
    return retryDelay;
  }

  protected TimeUnit intervalUnit() {
    return delayUnit;
  }

  private void onLogin() {
    synchronized (lock) {
      Connection prev = context.currentConnection();
      try {
        context.currentConnection(conn);
        registry = context.getLoginRegistry();
        if (subs == null) {
          subs = new ArrayList<>();
        }
        lock.notifyAll();
      } finally {
        context.currentConnection(prev);
      }
    }
  }

  /***
   * Chamado apenas em caso de logout por escolha da aplica��o
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
          logger.log(Level.WARNING, "Erro ao desativar o objeto observador de" +
            " logins.", e);
        }
      }
    }
  }

  private void clearLoginState() {
    synchronized (lock) {
      this.registry = null;
      this.sub = null;
      // n�o interrompo pois pode estar em uma chamada remota/JacORB. Se o
      // cancelamento proceder, gera um evento fail da callback do objeto futuro.
      if (futureSub != null) {
        this.futureSub.cancel(false);
      }
      this.futureSub = null;

      if (futureRemove != null) {
        this.futureRemove.cancel(false);
      }
      this.futureRemove = null;

      // n�o nulifico futureReLogin pois isso � usado como crit�rio para
      // acordar as threads de relogin
      if (futureReLogin != null) {
        this.futureReLogin.cancel(false);
      }
    }
  }

  private void onRelogin(LoginInfo newLogin) {
    // O relogin deve ser feito de forma s�ncrona (pode ser feito com
    // tarefas mas deve-se sincronizar ao fim). Dado o funcionamento da
    // conex�o do SDK, s� h� como ter duas chamadas concorrentes desse m�todo
    // caso enquanto uma thread estiver esperando pela tarefa de subscri��o
    // de observador, o login for novamente perdido e refeito por outra thread.
    synchronized (lock) {
      // sem subscri��es, n�o h� nada a fazer.
      if (subs == null || subs.size() == 0) {
        return;
      }
      // testar se h� um relogin em andamento. Se houver, cancelar pois
      // nesse caso houve perda de login enquanto a subscri��o ainda estava
      // sendo feita. Isso � importante pois caso a subscri��o j� tenha sido
      // refeita, ela n�o valer� mais e as tarefas de watch nunca terminar�o
      // pois ficar�o num loop de OBJ_NOT_EXISTS.
      while (futureReLogin != null) {
        // o m�todo abaixo j� cancela o relogin anterior em andamento
        clearLoginState();
        try {
          // aguardo tarefa de relogin anterior terminar
          lock.wait();
        } catch (InterruptedException e) {
          logInterruptError(e);
        }
        // devo testar de novo pois posso ter sido acordado tanto por ter
        // feito o relogin como por um logout. Se foi logout, n�o terei login
        // e ent�o desisto. Se houve um novo relogin, o login atual n�o ser�
        // o mesmo que o que recebi por par�metro, ent�o desisto tb.
        LoginInfo currLogin = conn.login();
        if ((currLogin == null) || (!newLogin.id.equals(currLogin.id))) {
          return;
        }
      }
      // refa�o os passos de um login pois chamei clearstate.
      onLogin();
      // lan�ar assincronamente uma retrytask para refazer o observador e
      // watchs de logins
      Set<String> watchedLogins = new HashSet<>();
      // insere todos os logins observados sem repeti��es
      for (LoginSubscriptionImpl sub : subs) {
        watchedLogins.addAll(sub.loginsCopy());
      }
      futureReLogin = pool.doTask(new ReLoginTask(registry, observer,
        watchedLogins), new RetryContext(retryDelay, delayUnit));
      Futures.addCallback(futureReLogin, new FutureCallback<LoginObserverSubscription>() {
        @Override
        public void onSuccess(LoginObserverSubscription result) {
          // redefinir sub j� com logins watched, nulificar futureReLogin e
          // notificar quem estiver esperando.
          synchronized (lock) {
            if (futureReLogin != null && !futureReLogin.isCancelled()) {
              // se a sub recebida for null � porque o objeto foi removido no
              // barramento por perda de login antes que os watch pudessem ser
              // conclu�dos.
              if (result != null) {
                sub = result;
              }
              futureReLogin = null;
            }
            lock.notifyAll();
          }
        }

        @Override
        public void onFailure(Throwable t) {
          // S� entra aqui se a aplica��o fez logout ou um outro relogin cancelou
          // esse. Basta desistir que o SDK cuida do login atual depois.
          logger.log(Level.WARNING, "Erro ao reinserir o observador de logins" +
            " do barramento devido a um logout ou relogin. Esse erro " +
            "provavelmente pode ser ignorado.", t);
          synchronized (lock) {
            lock.notifyAll();
          }
        }
      }, pool.pool());
    }
  }

  private tecgraf.openbus.core.v2_1.services.access_control.LoginRegistry
    registry() {
    synchronized (lock) {
      while (registry == null) {
        if (futureReLogin == null) {
          logger.severe("N�o h� login para realizar a chamada.");
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
          if (futureSub != null) {
            if (futureSub.isCancelled()) {
              //situa��o de logout
              throw new NO_PERMISSION(NoLoginCode.value, CompletionStatus
                .COMPLETED_NO);
            } else if (futureSub.isDone()) {
              // n�o h� relogin e a tarefa de subscri��o terminou
              // corretamente mas manteve sub null, ent�o a subscri��o foi
              // removida do servidor
              throw new OBJECT_NOT_EXIST("A subscri��o foi removida do " +
                "barramento por uma a��o diferente de um logout ou relogin.");
            }
          } else {
            // n�o h� relogin nem tarefa de subscri��o em andamento, ent�o
            // a subscri��o foi removida do servidor
            throw new OBJECT_NOT_EXIST("A subscri��o foi removida do " +
              "barramento por uma a��o diferente de um logout ou relogin.");
          }
        }
        try {
          // aguardo tarefa obter a subscri��o
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
          logger.severe("N�o h� login para realizar a chamada.");
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
    logger.log(Level.SEVERE, "Interrup��o n�o esperada ao refazer um login. " +
      "Verifique se sua aplica��o est� tentando interromper a thread " +
      "quando esta est� executando c�digo alheio, como do SDK OpenBus " +
      "ou do JacORB.", e);
  }

  private static List<LoginInfo> convertLoginArrayToList(LoginInfo[] infos) {
    List<LoginInfo> ret = new ArrayList<>();
    Collections.addAll(ret, infos);
    return ret;
  }

  private class LoginSubRemovalTask implements Callable<Void> {
    private final LoginObserverSubscription sub;

    public LoginSubRemovalTask(LoginObserverSubscription sub) {
      this.sub = sub;
    }

    @Override
    public Void call() throws ServiceFailure {
      try {
        context.currentConnection(conn);
        sub.remove();
      } catch (OBJECT_NOT_EXIST ignored) {}
      return null;
    }
  }

  private class ReLoginTask implements Callable<LoginObserverSubscription> {
    private final tecgraf.openbus.core.v2_1.services.access_control
      .LoginRegistry registry;
    private final tecgraf.openbus.core.v2_1.services.access_control
      .LoginObserver observer;
    private final Set<String> watchedLogins;

    public ReLoginTask(tecgraf.openbus.core.v2_1.services.access_control
      .LoginRegistry registry, tecgraf.openbus.core.v2_1.services
      .access_control.LoginObserver observer, Set<String> watchedLogins) {
      this.registry = registry;
      this.observer = observer;
      this.watchedLogins = watchedLogins;
    }

    @Override
    public LoginObserverSubscription call() throws ServiceFailure {
      context.currentConnection(conn);
      LoginObserverSubscription sub = registry.subscribeObserver(observer);
      // as chamadas remotas abaixo precisam ser feitas dentro do bloco
      // synchronized, para n�o arriscar de perder algum pedido de watch
      // login do usu�rio. Em caso de erro, a tarefa deixar� estourar para
      // que o mutex seja liberado antes de uma nova tentativa.
      synchronized (lock) {
        if (watchedLogins != null) {
          try {
            sub.watchLogins(watchedLogins.toArray(new String[watchedLogins
              .size()]));
          } catch (final InvalidLogins e) {
            synchronized (lock) {
              List<LoginSubscriptionImpl> subs = subs();
              if (subs != null) {
                for (final LoginSubscriptionImpl loginSubscription : subs) {
                  // arrisco criar v�rias threads para n�o compartilhar subs
                  Thread t = new Thread(() -> {
                    loginSubscription.nonExistentLogins(e.loginIds);
                  });
                  t.start();
                }
              }
            }
            logger.log(Level.WARNING, "Alguns logins n�o puderam ser " +
              "re-observados pois sa�ram do barramento.", e);
            // atualmente com o catch de OBJECT_NOT_EXIST abaixo, se um
            // administrador remover o meu observador, o mesmo n�o ser� mais
            // cadastrado e a API lan�ar� OBJECT_NOT_EXIST para o usu�rio. O
            // mesmo ter� de fazer logout e login para cadastrar novamente.
            // Uma outra op��o seria deixar o erro estourar (como fa�o nos
            // outros erros), o que levaria a refazer o login automaticamente
            // e refazer os observadores. O problema � que caso haja algo
            // removendo o observador imediatamente ap�s ser cadastrado (como
            // uma regra) esse efeito levaria a um loop e o usu�rio n�o teria
            // o login de volta nem teria como saber do erro.
          } catch (OBJECT_NOT_EXIST e) {
            // significa que minha subscri��o n�o existe mais. Caso isto tenha
            // ocorrido por perda de login um novo relogin ser� feito e tratar�
            // do problema novamente. Caso a sub tenha sido removida por um
            // admin, a sub local ser� nula e se a aplica��o tentar obt�-la vai
            // receber uma exce��o OBJECT_NOT_EXIST lan�ada pelo m�todo sub().
            sub = null;
          } catch (Exception e) {
            // caso outro erro ocorra, tento como best-effort remover a
            // subscri��o e deixo a tarefa toda ser repetida.
            try {
              sub.remove();
            } catch (Exception ignored) {}
            throw e;
          }
        }
      }
      return sub;
    }
  }
}
