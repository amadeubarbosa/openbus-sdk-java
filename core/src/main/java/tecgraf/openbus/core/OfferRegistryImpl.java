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
import scs.core.IComponent;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.UnauthorizedOperation;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.offer_registry.*;
import tecgraf.openbus.*;
import tecgraf.openbus.OfferObserver;
import tecgraf.openbus.OfferRegistry;
import tecgraf.openbus.OfferRegistryObserver;
import tecgraf.openbus.offers.ServiceProperties;
import tecgraf.openbus.retry.LocalRetryContext;
import tecgraf.openbus.retry.RetryTaskPool;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

// TODO pensar numa forma de oferecer ao usu�rio funcionalidades dos
// LocalRetryContexts (na classe de login tb) para que possam consultar os objetos
// futuros e seus erros. Outra possibilidade interessante seria permitir que
// o usu�rio fornecesse a pol�tica do shouldRetry. Talvez os eventos de
// sucesso/falha de registro j� sejam suficientes s� com logs.

//TODO otimizar a concorr�ncia utilizando v�rios locks se poss�vel, nessa e
// em outras classes (possivelmente usando a guava Striped).

// N�o basta ser um OfferRegistryObserver pois preciso ter v�rios
// observadores, n�o apenas um como no caso do login, pois n�o h�
// multiplexa��o de observadores. � necess�rio ter um array de observadores
// de ofertas e receber o aviso desse observador quando a oferta for registrada.
class OfferRegistryImpl implements OfferRegistry {
  private final Object lock = new Object();
  private final OpenBusContextImpl context;
  private final Connection conn;
  private final POA poa;
  private tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistry
    registry;
  /** Mecanismo de execu��o de tarefas com suporte a retentativas */
  private final RetryTaskPool pool;
  /** Guarda as ofertas locais mantidas nas chaves. Cada valor s� ser� diferente
   *  de null se houver um registro em andamento. */
  private Map<LocalOfferImpl, ListenableFuture<RemoteOfferImpl>> maintainedOffers;
  private Map<OfferRegistrySubscriptionContext,
    ListenableFuture<OfferRegistryObserverSubscription>> registrySubs;
  private Map<OfferSubscriptionContext,
    ListenableFuture<OfferObserverSubscription>> offerSubs;
  private ListenableFuture<Void> futureReLogin;
  private final long retryDelay;
  private final TimeUnit delayUnit;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  protected OfferRegistryImpl(OpenBusContextImpl context, Connection conn,
                              POA poa, RetryTaskPool pool, long interval,
                              TimeUnit unit) {
    this.context = context;
    this.conn = conn;
    this.poa = poa;
    this.pool = pool;
    this.retryDelay = interval;
    this.delayUnit = unit;
  }

  @Override
  public Connection conn() {
    return conn;
  }

  @Override
  public LocalOffer registerService(IComponent service_ref,
                                    Map<String, String> properties) {
    tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistry registry
      = registry();
    if (registry == null) {
      return null;
    }
    // criar a oferta local
    final LocalOfferImpl localOffer = new LocalOfferImpl(this, service_ref,
      convertMapToProperties(properties));
    // disparar a tarefa de registro
    if (!doRegisterTask(registry, localOffer)) {
      return null;
    }
    // retornar a oferta local
    return localOffer;
  }

  @Override
  public List<RemoteOffer> findServices(Map<String, String> properties)
    throws ServiceFailure {
    tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistry registry
      = registry();
    if (registry == null) {
      return new ArrayList<>();
    }
    Connection prev = context.getCurrentConnection();
    try {
      ServiceOfferDesc[] descs = registry.findServices(convertMapToProperties
        (properties));
      List<RemoteOffer> offers = new ArrayList<>(descs.length);
      for (ServiceOfferDesc desc : descs) {
        offers.add(new RemoteOfferImpl(this, desc));
      }
      return offers;
    } finally {
      context.setCurrentConnection(prev);
    }
  }

  @Override
  public List<RemoteOffer> getAllServices() throws ServiceFailure {
    tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistry registry
      = registry();
    if (registry == null) {
      return new ArrayList<>();
    }
    Connection prev = context.getCurrentConnection();
    try {
      ServiceOfferDesc[] descs = registry.getAllServices();
      List<RemoteOffer> offers = new ArrayList<>(descs.length);
      for (ServiceOfferDesc desc : descs) {
        offers.add(new RemoteOfferImpl(this, desc));
      }
      return offers;
    } finally {
      context.setCurrentConnection(prev);
    }
  }

  @Override
  public OfferRegistrySubscription subscribeObserver(OfferRegistryObserver
    observer, Map<String, String> properties) throws ServantNotActive,
    WrongPolicy {
    tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistry registry
      = registry();
    if (registry == null) {
      return null;
    }
    OfferRegistryObserverImpl busObserver = new OfferRegistryObserverImpl
      (observer, this);
    tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistryObserver
      proxy = OfferRegistryObserverHelper.narrow(poa.servant_to_reference
      (busObserver));
    OfferRegistrySubscriptionContext context = new
      OfferRegistrySubscriptionContext(busObserver, proxy, properties);
    if (!doSubscribeToRegistryTask(registry, context)) {
      return null;
    }
    return new OfferRegistrySubscriptionImpl(this, context);
  }

  protected void cancelRegisterTask(LocalOfferImpl offer) {
    synchronized (lock) {
      Map<LocalOfferImpl, ListenableFuture<RemoteOfferImpl>> maintainedOffers
        = maintainedOffers();
      if (maintainedOffers == null) {
        return;
      }
      // o boolean abaixo � um atomic boolean somente para ser um objeto e
      // ser poss�vel declarar final para o closure do FutureCallback.
      final AtomicBoolean needRemove = new AtomicBoolean(false);
      ListenableFuture<RemoteOfferImpl> future = maintainedOffers.get(offer);
      if (future != null) {
        if (!future.cancel(false)) {
          // o registro j� havia terminado mas entrou aqui no cancelamento
          // antes de registrar o sucesso da chamada
          needRemove.set(true);
        } else {
          // como o cancel n�o interrompe a tarefa, ela pode conseguir terminar.
          Futures.addCallback(future, new FutureCallback<RemoteOfferImpl>() {
              @Override
              public void onFailure(Throwable ex) {
                // a tarefa foi cancelada corretamente ou falhou, ent�o n�o �
                // necess�rio fazer nada.
              }

              @Override
              public void onSuccess(RemoteOfferImpl remote) {
                // a tarefa n�o foi cancelada a tempo. Cancelar manualmente.
                needRemove.set(true);
              }
            },
            pool.pool());
        }
      } else {
        // um registro j� havia sido feito ou n�o havia sido disparado ainda
        if (maintainedOffers.containsKey(offer)) {
          needRemove.set(true);
        }
      }
      if (needRemove.get()) {
        // a oferta j� havia sido registrada, cancelar manualmente
        RemoteOfferImpl remote = null;
        if (future != null) {
          try {
            remote = future.isDone() ? future.get() : null;
          } catch (InterruptedException | ExecutionException ignored) {}
        }
        if (remote == null) {
          remote = (RemoteOfferImpl) offer.remoteOffer(0, 1);
        }
        if (remote != null) {
          ListenableFuture<Void> futureRemoval = pool.doTask(new
            OfferRemovalTask(remote), new LocalRetryContext(retryDelay,
            delayUnit));
          Futures.addCallback(futureRemoval, new FutureCallback<Void>() {
                @Override
                public void onFailure(Throwable ex) {
                  // so deve entrar aqui se a aplica��o escolheu fazer um logout, ou se
                  // pararam as retentativas do RetryTask.
                  logger.error("Erro ao remover oferta do barramento.", ex);
                }

                @Override
                public void onSuccess(Void nothing) {
                  logger.info("Oferta removida do barramento.");
                }
              },
            pool.pool());
        }
      }
      maintainedOffers.remove(offer);
      logger.info("Registro de oferta cancelado com sucesso.");
    }
  }

  protected void removeRegistrySubscription(OfferRegistrySubscriptionContext
    localSub) {
    synchronized (lock) {
      Map<OfferRegistrySubscriptionContext,
        ListenableFuture<OfferRegistryObserverSubscription>> registrySubs =
        registrySubs();
      if (registrySubs == null) {
        return;
      }
      // o boolean abaixo � um atomic boolean somente para ser um objeto e
      // ser poss�vel declarar final para o closure do FutureCallback.
      final AtomicBoolean needRemove = new AtomicBoolean(false);
      ListenableFuture<OfferRegistryObserverSubscription> future =
        registrySubs.get(localSub);
      if (future != null) {
        if (!future.cancel(false)) {
          // a subscri��o j� havia terminado mas entrou aqui no cancelamento
          // antes de registrar o sucesso da chamada
          needRemove.set(true);
        } else {
          // como o cancel n�o interrompe a tarefa, ela pode conseguir terminar.
          Futures.addCallback(future, new
            FutureCallback<OfferRegistryObserverSubscription>() {
              @Override
              public void onFailure(Throwable ex) {
                // a tarefa foi cancelada corretamente ou falhou, ent�o n�o �
                // necess�rio fazer nada.
              }

              @Override
              public void onSuccess(OfferRegistryObserverSubscription remote) {
                // a tarefa n�o foi cancelada a tempo. Cancelar manualmente.
                needRemove.set(true);
              }
            },
            pool.pool());
        }
      } else {
        // uma subscri��o j� havia sido feita ou n�o havia sido disparada ainda
        if (registrySubs.containsKey(localSub)) {
          needRemove.set(true);
        }
      }
      if (needRemove.get()) {
        // a subscri��o j� havia sido efetuada, cancelar manualmente
        OfferRegistryObserverSubscription sub = null;
        if (future != null) {
          try {
            sub = future.isDone() ? future.get() : null;
          } catch (InterruptedException | ExecutionException ignored) {}
        }
        if (sub == null) {
          sub = (OfferRegistryObserverSubscription) registrySubs.get
            (localSub);
        }
        if (sub != null) {
          ListenableFuture<Void> futureRemoval = pool.doTask(new
            OfferRegistrySubRemovalTask(sub), new LocalRetryContext
            (retryDelay, delayUnit));
          Futures.addCallback(futureRemoval, new FutureCallback<Void>() {
              @Override
              public void onFailure(Throwable ex) {
                // so deve entrar aqui se a aplica��o escolheu fazer um logout, ou se
                // pararam as retentativas do RetryTask.
                logger.error("Erro ao remover subscri��o de registro de " +
                  "oferta do barramento.", ex);
              }

              @Override
              public void onSuccess(Void nothing) {
                logger.info("Subscri��o de registro de oferta removida do " +
                  "barramento.");
              }
            },
            pool.pool());
        }
      }
      registrySubs.remove(localSub);
      try {
        byte[] oid = poa.reference_to_id(localSub.proxy);
        poa.deactivate_object(oid);
      } catch (Exception e) {
        logger.warn("Erro ao desativar um objeto observador de " +
          "registro de ofertas.", e);
      }
      logger.info("Subscri��o de registro de oferta cancelada com sucesso.");
    }
  }

  // Diferentemente dos observadores de logins onde h� apenas um objeto
  // remoto (o loginregistry), aqui fa�o dessa forma pois a RemoteOffer �
  // apenas uma refer�ncia e n�o tenho controle sobre quantas s�o geradas
  // para cada oferta remota do barramento. N�o valeria a pena o esfor�o.
  protected OfferSubscription subscribeToOffer(RemoteOfferImpl remoteOffer,
                                               OfferObserver observer) throws
    ServantNotActive, WrongPolicy {
    ServiceOfferDesc offerDesc = remoteOffer.offer();
    if (offerDesc == null || offerDesc.ref == null) {
      return null;
    }
    final Map<OfferSubscriptionContext,
      ListenableFuture<OfferObserverSubscription>> offerSubs = offerSubs();
    if (offerSubs == null) {
      return null;
    }
    // cria o observador interno
    OfferObserverImpl internalObserver = new OfferObserverImpl(this,
      observer, remoteOffer);
    tecgraf.openbus.core.v2_1.services.offer_registry.OfferObserver proxy =
      OfferObserverHelper.narrow(poa.servant_to_reference(internalObserver));
    OfferSubscriptionContext context = new OfferSubscriptionContext
      (internalObserver, proxy, offerDesc);
    if (!doSubscribeToOfferTask(offerSubs, context)) {
      return null;
    }
    return new OfferSubscriptionImpl(this, context, remoteOffer);
  }

  protected void removeOfferSubscription(OfferSubscriptionContext localSub) {
    synchronized (lock) {
      Map<OfferSubscriptionContext,
        ListenableFuture<OfferObserverSubscription>> offerSubs = offerSubs();
      if (offerSubs == null) {
        return;
      }
      // o boolean abaixo � um atomic boolean somente para ser um objeto e
      // ser poss�vel declarar final para o closure do FutureCallback.
      final AtomicBoolean needRemove = new AtomicBoolean(false);
      ListenableFuture<OfferObserverSubscription> future = offerSubs.get
        (localSub);
      if (future != null) {
        if (!future.cancel(false)) {
          // a subscri��o j� havia terminado mas entrou aqui no cancelamento
          // antes de registrar o sucesso da chamada
          needRemove.set(true);
        } else {
          // como o cancel n�o interrompe a tarefa, ela pode conseguir terminar.
          Futures.addCallback(future, new
              FutureCallback<OfferObserverSubscription>() {
                @Override
                public void onFailure(Throwable ex) {
                  // a tarefa foi cancelada corretamente ou falhou, ent�o n�o �
                  // necess�rio fazer nada.
                }

                @Override
                public void onSuccess(OfferObserverSubscription remote) {
                  // a tarefa n�o foi cancelada a tempo. Cancelar manualmente.
                  needRemove.set(true);
                }
              },
            pool.pool());
        }
      } else {
        // uma subscri��o j� havia sido feita ou n�o havia sido disparada ainda
        if (offerSubs.containsKey(localSub)) {
          needRemove.set(true);
        }
      }
      if (needRemove.get()) {
        // a subscri��o j� havia sido efetuada, cancelar manualmente
        OfferObserverSubscription sub = null;
        if (future != null) {
          try {
            sub = future.isDone() ? future.get() : null;
          } catch (InterruptedException | ExecutionException ignored) {}
        }
        if (sub == null) {
          sub = (OfferObserverSubscription) offerSubs.get(localSub);
        }
        if (sub != null) {
          ListenableFuture<Void> futureRemoval = pool.doTask(new
            OfferSubRemovalTask(sub), new LocalRetryContext(retryDelay,
            delayUnit));
          Futures.addCallback(futureRemoval, new FutureCallback<Void>() {
              @Override
              public void onFailure(Throwable ex) {
                // so deve entrar aqui se a aplica��o escolheu fazer um
                // logout, ou se pararam as retentativas do RetryTask.
                logger.error("Erro ao remover subscri��o de oferta do " +
                  "barramento.", ex);
              }

              @Override
              public void onSuccess(Void nothing) {
                logger.info("Subscri��o de oferta removida da oferta no " +
                  "barramento.");
              }
            },
            pool.pool());
        }
      }
      offerSubs.remove(localSub);
      logger.info("Subscri��o de oferta cancelada com sucesso.");
    }
  }

  protected void onOfferRemove(RemoteOfferImpl offer) {
    synchronized (lock) {
      Map<OfferSubscriptionContext,
        ListenableFuture<OfferObserverSubscription>> offerSubs = offerSubs();
      if (offerSubs == null) {
        return;
      }
      for (Map.Entry<OfferSubscriptionContext,
        ListenableFuture<OfferObserverSubscription>> entry : offerSubs.entrySet
        ()) {
        String receivedId = offer.properties(false).get(ServiceProperties.ID);
        OfferSubscriptionContext context = entry.getKey();
        String iterId = getOfferIdFromProperties(context.offerDesc);
        if (receivedId.equals(iterId)) {
          clearOfferSubscription(context);
          ListenableFuture<OfferObserverSubscription> future = entry.getValue();
          if (future != null) {
            future.cancel(false);
          }
          offerSubs.remove(context);
        }
      }
    }
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

  private void onLogin() {
    synchronized (lock) {
      Connection prev = context.getCurrentConnection();
      try {
        context.setCurrentConnection(conn);
        registry = context.getOfferRegistry();
        if (maintainedOffers == null) {
          maintainedOffers = new HashMap<>();
        }
        if (registrySubs == null) {
          registrySubs = new HashMap<>();
        }
        if (offerSubs == null) {
          offerSubs = new HashMap<>();
        }
      } finally {
        context.setCurrentConnection(prev);
      }
    }
  }

  private void onLogout() {
    synchronized (lock) {
      clearLoginState();

      Connection prev = context.getCurrentConnection();
      try {
        context.setCurrentConnection(conn);
        if (this.maintainedOffers != null) {
          for (LocalOfferImpl offer : maintainedOffers.keySet()) {
            offer.loggedOut();
          }
          this.maintainedOffers.clear();
        }
        this.maintainedOffers = null;

        if (this.registrySubs != null) {
          for (OfferRegistrySubscriptionContext context : registrySubs.keySet()) {
            // best effort
            try {
              context.sub.remove();
            } catch (Exception ignored) {}
            try {
              byte[] oid = poa.reference_to_id(context.sub);
              poa.deactivate_object(oid);
            } catch (Exception e) {
              logger.warn("Erro ao desativar um objeto observador de " +
                "registro de oferta.", e);
            }
          }
          this.registrySubs.clear();
        }
        this.registrySubs = null;

        if (this.offerSubs != null) {
          for (OfferSubscriptionContext context : offerSubs.keySet()) {
            clearOfferSubscription(context);
          }
          this.offerSubs.clear();
        }
        this.offerSubs = null;

        this.lock.notifyAll();
      } finally {
        context.setCurrentConnection(prev);
      }
    }
  }

  // quando ocorre um logout inesperado, apenas esse m�todo deve ser chamado.
  private void clearLoginState() {
    synchronized (lock) {
      // n�o interrompo pois pode estar em uma chamada remota/JacORB. Se o
      // cancelamento proceder, gera um evento fail da callback do objeto futuro.
      if (maintainedOffers != null) {
        for (Map.Entry<LocalOfferImpl, ListenableFuture<RemoteOfferImpl>> entry
          : maintainedOffers.entrySet()) {
          ListenableFuture<RemoteOfferImpl> future = entry.getValue();
          if (future != null) {
            future.cancel(false);
            maintainedOffers.put(entry.getKey(), null);
          }
        }
      }
      if (registrySubs != null) {
        for (Map.Entry<OfferRegistrySubscriptionContext,
          ListenableFuture<OfferRegistryObserverSubscription>> entry :
          registrySubs.entrySet()) {
          ListenableFuture<OfferRegistryObserverSubscription> future = entry
            .getValue();
          if (future != null) {
            future.cancel(false);
            registrySubs.put(entry.getKey(), null);
          }
        }
      }
      if (offerSubs != null) {
        for (Map.Entry<OfferSubscriptionContext,
          ListenableFuture<OfferObserverSubscription>> entry : offerSubs
          .entrySet()) {
          ListenableFuture<OfferObserverSubscription> future = entry.getValue();
          if (future != null) {
            future.cancel(false);
            offerSubs.put(entry.getKey(), null);
          }
        }
      }

      // n�o nulifico futureReLogin pois isso � usado como crit�rio para
      // acordar as threads de relogin
      if (futureReLogin != null) {
        this.futureReLogin.cancel(false);
      }

      registry = null;
    }
  }

  private void onRelogin(LoginInfo newLogin) {
    // O relogin deve ser feito de forma s�ncrona (pode ser feito com
    // tarefas mas deve-se sincronizar ao fim). Dado o funcionamento da
    // conex�o do SDK, s� h� como ter duas chamadas concorrentes desse m�todo
    // caso enquanto uma thread estiver esperando pela tarefa de subscri��o
    // de observador, o login for novamente perdido e refeito por outra thread.
    // Etapas:
    ListenableFuture<Void> future;
    synchronized (lock) {
      // sem subscri��es, n�o h� nada a fazer.
      if ((maintainedOffers == null || maintainedOffers.size() == 0) &&
        (registrySubs == null || registrySubs.size() == 0) && (offerSubs ==
        null || offerSubs.size() == 0)) {
        return;
      }
      // 0) testar se h� um relogin em andamento. Se houver, cancelar pois
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
      // 1) lan�ar assincronamente uma retrytask para refazer registros e
      // observadores de registros e de ofertas
      futureReLogin = future = pool.doTask(new ReLoginTask(context
        .getOfferRegistry()), new LocalRetryContext(retryDelay, delayUnit));
    }
    // 2) ressincronizar fora da regi�o cr�tica, para dar chance a outros
    // relogins de cancelarem essa tarefa.
    try {
      Uninterruptibles.getUninterruptibly(future);
    } catch (ExecutionException | CancellationException e) {
      // S� entra aqui se a aplica��o fez logout ou um outro relogin cancelou
      // esse. Basta desistir que o SDK cuida do login atual depois.
      logger.warn("Erro ao reinserir registros e observadores de oferta no " +
        "barramento devido a um logout ou relogin. Esse erro provavelmente " +
        "pode ser ignorado.", e);
      synchronized (lock) {
        lock.notifyAll();
      }
      return;
    }
    // 3) nulificar futureReLogin e notificar quem estiver esperando.
    synchronized (lock) {
      if (futureReLogin != null && !futureReLogin.isCancelled()) {
        futureReLogin = null;
      }
      lock.notifyAll();
    }
  }

  private void clearOfferSubscription(OfferSubscriptionContext context) {
    Connection prev = this.context.getCurrentConnection();
    try {
      // best effort
      try {
        context.sub.remove();
      } catch (Exception ignored) {}
    } finally {
      this.context.setCurrentConnection(prev);
    }
    try {
      byte[] oid = poa.reference_to_id(context.proxy);
      poa.deactivate_object(oid);
    } catch (Exception e) {
      logger.warn("Erro ao desativar um objeto observador de ofertas.",
        e);
    }
  }

  private boolean doRegisterTask(tecgraf.openbus.core.v2_1.services
                                   .offer_registry.OfferRegistry registry,
                                 final LocalOfferImpl localOffer) {
    synchronized (lock) {
      OfferRegistryTask task = new OfferRegistryTask(localOffer, registry);
      ListenableFuture<RemoteOfferImpl> futureRegistry = pool.doTask(task, new
        LocalRetryContext(retryDelay, delayUnit));
      // inserir a oferta a ser registrada no mapa
      maintainedOffers.put(localOffer, futureRegistry);
      Futures.addCallback(futureRegistry, new
          FutureCallback<RemoteOfferImpl>() {
            @Override
            public void onFailure(Throwable ex) {
              // so deve entrar aqui se a aplica��o escolheu fazer um logout, ou se
              // pararam as retentativas do RetryTask.
              logger.error("Erro ao registrar oferta no barramento.", ex);
            }

            @Override
            public void onSuccess(RemoteOfferImpl remote) {
              synchronized (lock) {
                // devo setar apenas se n�o foi cancelado. Se future for
                // null, houve cancelamento.
                ListenableFuture<RemoteOfferImpl> future = maintainedOffers
                  .get(localOffer);
                if (future != null) {
                  // n�o h� como ter dois sucessos ao mesmo tempo, portanto
                  // posso passar por cima do valor direto sem testar.
                  maintainedOffers.put(localOffer, null);
                  localOffer.remote(remote);
                  logger.info("Registro de oferta realizado com sucesso.");
                }
              }
            }
          },
        pool.pool());
    }
    return true;
  }

  private boolean doSubscribeToRegistryTask(tecgraf.openbus.core.v2_1
                                              .services.offer_registry
                                              .OfferRegistry registry, final
    OfferRegistrySubscriptionContext context) {
    synchronized (lock) {
      final Map<OfferRegistrySubscriptionContext,
        ListenableFuture<OfferRegistryObserverSubscription>> registrySubs =
        registrySubs();
      if (registrySubs == null) {
        return false;
      }
      OfferRegistrySubscriptionTask task = new OfferRegistrySubscriptionTask
        (registry, context.proxy, convertMapToProperties(context.properties));
      ListenableFuture<OfferRegistryObserverSubscription> futureRegistrySub =
        pool.doTask(task, new LocalRetryContext(retryDelay, delayUnit));
      registrySubs.put(context, futureRegistrySub);
      Futures.addCallback(futureRegistrySub, new
          FutureCallback<OfferRegistryObserverSubscription>() {
            @Override
            public void onFailure(Throwable ex) {
              // so deve entrar aqui se a aplica��o escolheu fazer um logout, ou se
              // pararam as retentativas do RetryTask.
              logger.error("Erro ao inserir um observador de registro de " +
                "oferta no barramento.", ex);
            }

            @Override
            public void onSuccess(OfferRegistryObserverSubscription sub) {
              synchronized (lock) {
                // n�o h� como ter dois sucessos ao mesmo tempo, portanto
                // posso passar por cima do valor direto sem testar.
                context.sub = sub;
                registrySubs.put(context, null);
                logger.info("Observador de registro de ofertas cadastrado no" +
                  " barramento.");
              }
            }
          },
        pool.pool());
    }
    return true;
  }

  private boolean doSubscribeToOfferTask(final Map<OfferSubscriptionContext,
      ListenableFuture<OfferObserverSubscription>> offerSubs, final
    OfferSubscriptionContext context) {
    synchronized (lock) {
      OfferSubscriptionTask task = new OfferSubscriptionTask(context.offerDesc
        .ref, context.proxy);
      ListenableFuture<OfferObserverSubscription> futureOfferSub =
        pool.doTask(task, new LocalRetryContext(retryDelay, delayUnit));
      offerSubs.put(context, futureOfferSub);
      Futures.addCallback(futureOfferSub, new
          FutureCallback<OfferObserverSubscription>() {
            @Override
            public void onFailure(Throwable ex) {
              // so deve entrar aqui se a aplica��o escolheu fazer um logout, ou se
              // pararam as retentativas do RetryTask.
              logger.error("Erro ao inserir um observador de oferta no " +
                "barramento", ex);
            }

            @Override
            public void onSuccess(OfferObserverSubscription sub) {
              synchronized (lock) {
                // n�o h� como ter dois sucessos ao mesmo tempo, portanto
                // posso passar por cima do valor direto sem testar.
                context.sub = sub;
                offerSubs.put(context, null);
                logger.info("Observador de oferta cadastrado no barramento.");
              }
            }
          },
        pool.pool());
    }
    return true;
  }

  private Map<LocalOfferImpl,ListenableFuture<RemoteOfferImpl>>
  maintainedOffers() {
    synchronized (lock) {
      while (maintainedOffers == null) {
        if (futureReLogin == null) {
          logger.error("N�o h� login para realizar a chamada.");
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
      return maintainedOffers;
    }
  }

  private Map<OfferRegistrySubscriptionContext,
    ListenableFuture<OfferRegistryObserverSubscription>> registrySubs() {
    synchronized (lock) {
      while (registrySubs == null) {
        if (futureReLogin == null) {
          logger.error("N�o h� login para realizar a chamada.");
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
      return registrySubs;
    }
  }

  private Map<OfferSubscriptionContext,
    ListenableFuture<OfferObserverSubscription>> offerSubs() {
    synchronized (lock) {
      while (offerSubs == null) {
        if (futureReLogin == null) {
          logger.error("N�o h� login para realizar a chamada.");
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
      return offerSubs;
    }
  }

  private void logInterruptError(Exception e) {
    logger.error("Interrup��o n�o esperada ao refazer um login. " +
      "Verifique se sua aplica��o est� tentando interromper a thread " +
      "quando esta est� executando c�digo alheio, como do SDK OpenBus " +
      "ou do JacORB.", e);
  }

  private tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistry
  registry() {
    synchronized (lock) {
      while (registry == null) {
        if (futureReLogin == null) {
          logger.error("N�o h� login para realizar a chamada.");
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

  /**
   */
  protected static LoginInfo getOwnerFromOffer(ServiceOfferDesc desc) {
    ServiceProperty[] props = desc.properties;
    LoginInfo info = new LoginInfo();
    boolean foundId = false, foundEntity = false;
    for (ServiceProperty prop : props) {
      if (prop.name.equals(ServiceProperties.LOGIN)) {
        info.id = prop.value;
        foundId = true;
      }
      if (prop.name.equals(ServiceProperties.ENTITY)) {
        info.entity = prop.value;
        foundEntity = true;
      }
      if (foundId && foundEntity) {
        break;
      }
    }
    return info;
  }

  protected static String getOfferIdFromProperties(ServiceOfferDesc desc) {
    ServiceProperty[] props = desc.properties;
    for (ServiceProperty prop : props) {
      if (prop.name.equals(ServiceProperties.ID)) {
        return prop.value;
      }
    }
    return "";
  }

  /**
   * Converts a String to String map to a ServiceProperty array. This array
   * will be typically used to create fault-tolerant proxies or to make a
   * find call.
   * @param properties The properties map.
   * @return The properties arranged in a ServiceProperty array.
   */
  protected static ServiceProperty[] convertMapToProperties(Map<String, String>
                                                           properties) {
    ServiceProperty[] props = new ServiceProperty[properties.size()];
    int i = 0;
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      props[i] = new ServiceProperty(entry.getKey(), entry.getValue());
      i++;
    }
    return props;
  }

  private class OfferRegistryTask implements Callable<RemoteOfferImpl> {
    private final LocalOfferImpl local;
    private final tecgraf.openbus.core.v2_1.services.offer_registry
      .OfferRegistry registry;

    public OfferRegistryTask(LocalOfferImpl local, tecgraf.openbus.core.v2_1
      .services.offer_registry.OfferRegistry registry) {
      this.local = local;
      this.registry = registry;
    }

    @Override
    public RemoteOfferImpl call() throws ServiceFailure, InvalidService,
      InvalidProperties, UnauthorizedFacets {
      context.setCurrentConnection(conn);
      ServiceOfferDesc offer = registry.registerService(local.service,
        local.props).describe();
      return new RemoteOfferImpl(OfferRegistryImpl.this, offer);
    }
  }

  private class OfferRemovalTask implements Callable<Void> {
    private final RemoteOfferImpl remote;

    public OfferRemovalTask (RemoteOfferImpl remote) {
      this.remote = remote;
    }

    @Override
    public Void call() throws ServiceFailure, UnauthorizedOperation {
      try {
        context.setCurrentConnection(conn);
        remote.remove();
      } catch (OBJECT_NOT_EXIST ignored) {}
      return null;
    }
  }

  private class OfferRegistrySubRemovalTask implements Callable<Void> {
    private final OfferRegistryObserverSubscription sub;

    public OfferRegistrySubRemovalTask(OfferRegistryObserverSubscription sub) {
      this.sub = sub;
    }

    @Override
    public Void call() throws ServiceFailure, UnauthorizedOperation {
      try {
        context.setCurrentConnection(conn);
        sub.remove();
      } catch (OBJECT_NOT_EXIST ignored) {}
      return null;
    }
  }

  private class OfferSubRemovalTask implements Callable<Void> {
    private final OfferObserverSubscription sub;

    public OfferSubRemovalTask(OfferObserverSubscription sub) {
      this.sub = sub;
    }

    @Override
    public Void call() throws ServiceFailure, UnauthorizedOperation {
      try {
        context.setCurrentConnection(conn);
        sub.remove();
      } catch (OBJECT_NOT_EXIST ignored) {}
      return null;
    }
  }

  private class OfferRegistrySubscriptionTask implements
    Callable<OfferRegistryObserverSubscription> {
    private final tecgraf.openbus.core.v2_1.services.offer_registry
      .OfferRegistry registry;
    private final tecgraf.openbus.core.v2_1.services.offer_registry
      .OfferRegistryObserver observer;
    private final ServiceProperty[] properties;

    public OfferRegistrySubscriptionTask(tecgraf.openbus.core.v2_1.services
                                           .offer_registry.OfferRegistry registry, tecgraf.openbus.core.v2_1.services
                                           .offer_registry.OfferRegistryObserver observer, ServiceProperty[]
                                           properties) {
      this.registry = registry;
      this.observer = observer;
      this.properties = properties;
    }

    @Override
    public OfferRegistryObserverSubscription call() throws ServiceFailure {
      context.setCurrentConnection(conn);
      return registry.subscribeObserver(observer, properties);
    }
  }

  private class OfferSubscriptionTask implements
    Callable<OfferObserverSubscription> {
    private final tecgraf.openbus.core.v2_1.services.offer_registry
      .OfferObserver observer;
    private final ServiceOffer offer;

    public OfferSubscriptionTask(ServiceOffer offer, tecgraf.openbus.core
      .v2_1.services.offer_registry.OfferObserver observer) {
      this.offer = offer;
      this.observer = observer;
    }

    @Override
    public OfferObserverSubscription call() throws ServiceFailure {
      try {
        context.setCurrentConnection(conn);
        offer.subscribeObserver(observer);
      } catch (OBJECT_NOT_EXIST ignored) {}
      return null;
    }
  }

  private class ReLoginTask implements Callable<Void> {
    private final tecgraf.openbus.core.v2_1.services.offer_registry
      .OfferRegistry registry;

    public ReLoginTask(tecgraf.openbus.core.v2_1.services.offer_registry
                         .OfferRegistry registry) {
      this.registry = registry;
    }
    @Override
    public Void call() throws Exception {
      context.setCurrentConnection(conn);
      for (Map.Entry<LocalOfferImpl, ListenableFuture<RemoteOfferImpl>> entry
        : maintainedOffers.entrySet()) {
        LocalOfferImpl offer = entry.getKey();
        ListenableFuture<RemoteOfferImpl> future = entry.getValue();
        RemoteOffer remote = offer.remoteOffer(0, 1);
        if (remote == null) {
          if (future == null || future.isCancelled()) {
            doRegisterTask(registry, offer);
          }
        }
      }
      for (Map.Entry<OfferRegistrySubscriptionContext,
        ListenableFuture<OfferRegistryObserverSubscription>> entry :
        registrySubs.entrySet()) {
        OfferRegistrySubscriptionContext context = entry.getKey();
        ListenableFuture<OfferRegistryObserverSubscription> future = entry
          .getValue();
        if (context.sub == null) {
          if (future == null || future.isCancelled()) {
            doSubscribeToRegistryTask(OfferRegistryImpl.this.context
              .getOfferRegistry(), context);
          }
        }
      }
      for (Map.Entry<OfferSubscriptionContext,
        ListenableFuture<OfferObserverSubscription>> entry : offerSubs
        .entrySet()) {
        OfferSubscriptionContext context = entry.getKey();
        ListenableFuture<OfferObserverSubscription> future = entry.getValue();
        if (context.sub == null) {
          if (future == null || future.isCancelled()) {
            doSubscribeToOfferTask(offerSubs, context);
          }
        }
      }
      return null;
    }
  }

  protected class OfferRegistrySubscriptionContext {
    public final OfferRegistryObserverImpl observer;
    public final tecgraf.openbus.core.v2_1.services.offer_registry
      .OfferRegistryObserver proxy;
    public final Map<String, String> properties;
    public volatile OfferRegistryObserverSubscription sub;

    public OfferRegistrySubscriptionContext(OfferRegistryObserverImpl
      observer, tecgraf.openbus.core.v2_1.services.offer_registry
      .OfferRegistryObserver proxy, Map<String, String> properties) {
      this.observer = observer;
      this.proxy = proxy;
      this.properties = properties;
    }
  }

  protected class OfferSubscriptionContext {
    public final OfferObserverImpl observer;
    private final tecgraf.openbus.core.v2_1.services.offer_registry
      .OfferObserver proxy;
    private final ServiceOfferDesc offerDesc;
    public volatile OfferObserverSubscription sub;

    public OfferSubscriptionContext(OfferObserverImpl observer, tecgraf
      .openbus.core.v2_1.services.offer_registry.OfferObserver proxy,
      ServiceOfferDesc offerDesc) {
      this.observer = observer;
      this.proxy = proxy;
      this.offerDesc = offerDesc;
    }
  }
}
