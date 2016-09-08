package tecgraf.openbus.core;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scs.core.IComponent;
import tecgraf.openbus.Connection;
import tecgraf.openbus.LocalOffer;
import tecgraf.openbus.OfferObserver;
import tecgraf.openbus.OfferRegistry;
import tecgraf.openbus.OfferRegistryObserver;
import tecgraf.openbus.OfferRegistrySubscription;
import tecgraf.openbus.OfferSubscription;
import tecgraf.openbus.RemoteOffer;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.UnauthorizedOperation;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.offer_registry.InvalidProperties;
import tecgraf.openbus.core.v2_1.services.offer_registry.InvalidService;
import tecgraf.openbus.core.v2_1.services.offer_registry.OfferObserverHelper;
import tecgraf.openbus.core.v2_1.services.offer_registry.OfferObserverSubscription;
import tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistryObserverHelper;
import tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistryObserverSubscription;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOffer;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
import tecgraf.openbus.core.v2_1.services.offer_registry.UnauthorizedFacets;
import tecgraf.openbus.offers.ServiceProperties;
import tecgraf.openbus.retry.RetryContext;
import tecgraf.openbus.retry.RetryTaskPool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
  private Map<OfferRegistrySubscriptionImpl,
    ListenableFuture<OfferRegistryObserverSubscription>> registrySubs;
  private Map<OfferSubscriptionImpl,
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
  public Connection connection() {
    return conn;
  }

  @Override
  public LocalOffer registerService(IComponent service,
    ArrayListMultimap<String, String> properties) {
    tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistry registry
      = registry();
    if (registry == null) {
      return null;
    }
    // criar a oferta local
    final LocalOfferImpl localOffer = new LocalOfferImpl(this, service,
      convertMapToProperties(properties));
    // disparar a tarefa de registro
    if (!doRegisterTask(registry, localOffer)) {
      return null;
    }
    // retornar a oferta local
    return localOffer;
  }

  @Override
  public List<RemoteOffer> findServices(ArrayListMultimap<String, String>
    properties) throws ServiceFailure {
    tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistry registry
      = registry();
    if (registry == null) {
      return new ArrayList<>();
    }
    Connection prev = context.currentConnection();
    try {
      context.currentConnection(conn);
      ServiceOfferDesc[] descs = registry.findServices(convertMapToProperties
        (properties));
      List<RemoteOffer> offers = new ArrayList<>(descs.length);
      for (ServiceOfferDesc desc : descs) {
        offers.add(new RemoteOfferImpl(this, desc));
      }
      return offers;
    } finally {
      context.currentConnection(prev);
    }
  }

  @Override
  public List<RemoteOffer> allServices() throws ServiceFailure {
    tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistry registry
      = registry();
    if (registry == null) {
      return new ArrayList<>();
    }
    Connection prev = context.currentConnection();
    try {
      context.currentConnection(conn);
      ServiceOfferDesc[] descs = registry.getAllServices();
      List<RemoteOffer> offers = new ArrayList<>(descs.length);
      for (ServiceOfferDesc desc : descs) {
        offers.add(new RemoteOfferImpl(this, desc));
      }
      return offers;
    } finally {
      context.currentConnection(prev);
    }
  }

  @Override
  public OfferRegistrySubscription subscribeObserver(OfferRegistryObserver
    observer, ArrayListMultimap<String, String> properties) throws
    ServantNotActive, WrongPolicy {
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
    OfferRegistrySubscriptionImpl localSub = new
      OfferRegistrySubscriptionImpl(this, busObserver, proxy, properties);
    if (!doSubscribeToRegistryTask(registry, localSub)) {
      return null;
    }
    return localSub;
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
          // preciso aguardar a finaliza��o
          try {
            future.get();
          } catch (Exception ignored) {}
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
          try {
            // aguarda a tarefa terminar. Se ok, obtem remote para remover;
            // sen�o, deu erro ent�o ignora e segue em frente.
            remote = (RemoteOfferImpl) offer.remoteOffer();
          } catch (Exception ignored) {}
        }
        if (remote != null) {
          ListenableFuture<Void> futureRemoval = pool.doTask(new
            OfferRemovalTask(remote), new OpenBusRetryContext(retryDelay,
            delayUnit));
          Futures.addCallback(futureRemoval, new FutureCallback<Void>() {
                @Override
                public void onFailure(Throwable ex) {
                  // so deve entrar aqui se a aplica��o escolheu fazer um
                  // logout, ou se pararam as retentativas do RetryTask. Como
                  // o tipo de retentativas � o OpenBusRetryContext, se essa
                  // chamada receber uma UserException, COMM_FAILURE ou
                  // OBJECT_NOT_EXIST, desistir� e chegar� aqui.
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

  protected void removeRegistrySubscription(OfferRegistrySubscriptionImpl
    localSub) {
    synchronized (lock) {
      Map<OfferRegistrySubscriptionImpl,
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
          // preciso aguardar a finaliza��o
          try {
            future.get();
          } catch (Exception ignored) {}
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
          sub = localSub.sub();
        }
        if (sub != null) {
          ListenableFuture<Void> futureRemoval = pool.doTask(new
            OfferRegistrySubRemovalTask(sub), new OpenBusRetryContext
            (retryDelay, delayUnit));
          Futures.addCallback(futureRemoval, new FutureCallback<Void>() {
              @Override
              public void onFailure(Throwable ex) {
                // so deve entrar aqui se a aplica��o escolheu fazer um
                // logout, ou se pararam as retentativas do RetryTask. Como
                // o tipo de retentativas � o OpenBusRetryContext, se essa
                // chamada receber uma UserException, COMM_FAILURE ou
                // OBJECT_NOT_EXIST, desistir� e chegar� aqui.
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
      throw new ServantNotActive("A oferta aparentemente foi removida.");
    }
    final Map<OfferSubscriptionImpl,
      ListenableFuture<OfferObserverSubscription>> offerSubs = offerSubs();
    if (offerSubs == null) {
      return null;
    }
    // cria o observador interno
    OfferObserverImpl internalObserver = new OfferObserverImpl(this,
      observer, remoteOffer);
    tecgraf.openbus.core.v2_1.services.offer_registry.OfferObserver proxy =
      OfferObserverHelper.narrow(poa.servant_to_reference(internalObserver));
    OfferSubscriptionImpl localSub = new OfferSubscriptionImpl
      (this, remoteOffer, internalObserver, proxy, offerDesc);
    if (!doSubscribeToOfferTask(offerSubs, localSub)) {
      return null;
    }
    return localSub;
  }

  protected void removeOfferSubscription(OfferSubscriptionImpl localSub) {
    synchronized (lock) {
      Map<OfferSubscriptionImpl,
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
          // preciso aguardar a finaliza��o
          try {
            future.get();
          } catch (Exception ignored) {}
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
          sub = localSub.sub();
        }
        if (sub != null) {
          ListenableFuture<Void> futureRemoval = pool.doTask(new
            OfferSubRemovalTask(sub), new OpenBusRetryContext(retryDelay,
            delayUnit));
          Futures.addCallback(futureRemoval, new FutureCallback<Void>() {
              @Override
              public void onFailure(Throwable ex) {
                // so deve entrar aqui se a aplica��o escolheu fazer um
                // logout, ou se pararam as retentativas do RetryTask. Como
                // o tipo de retentativas � o OpenBusRetryContext, se essa
                // chamada receber uma UserException, COMM_FAILURE ou
                // OBJECT_NOT_EXIST, desistir� e chegar� aqui.
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
      try {
        byte[] oid = poa.reference_to_id(localSub.proxy);
        poa.deactivate_object(oid);
      } catch (Exception e) {
        logger.warn("Erro ao desativar um objeto observador de oferta.", e);
      }
      logger.info("Subscri��o de oferta cancelada com sucesso.");
    }
  }

  protected void onOfferRemove(RemoteOfferImpl offer) {
    synchronized (lock) {
      Map<OfferSubscriptionImpl,
        ListenableFuture<OfferObserverSubscription>> offerSubs = offerSubs();
      if (offerSubs == null) {
        return;
      }
      for (Map.Entry<OfferSubscriptionImpl,
        ListenableFuture<OfferObserverSubscription>> entry : offerSubs.entrySet
        ()) {
        String receivedId = offer.properties(false).get(ServiceProperties.ID)
          .get(0);
        OfferSubscriptionImpl sub = entry.getKey();
        String iterId = getOfferIdFromProperties(sub.offerDesc);
        if (receivedId.equals(iterId)) {
          clearOfferSubscription(sub);
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
      Connection prev = context.currentConnection();
      try {
        context.currentConnection(conn);
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
        context.currentConnection(prev);
      }
    }
  }

  private void onLogout() {
    synchronized (lock) {
      clearLoginState();

      Connection prev = context.currentConnection();
      try {
        context.currentConnection(conn);
        if (this.maintainedOffers != null) {
          for (LocalOfferImpl offer : maintainedOffers.keySet()) {
            offer.loggedOut();
          }
          this.maintainedOffers.clear();
        }
        this.maintainedOffers = null;

        if (this.registrySubs != null) {
          for (OfferRegistrySubscriptionImpl localSub : registrySubs.keySet()) {
            // best effort
            try {
              localSub.remove();
            } catch (Exception ignored) {}
          }
          this.registrySubs.clear();
        }
        this.registrySubs = null;

        if (this.offerSubs != null) {
          for (OfferSubscriptionImpl localSub : offerSubs.keySet()) {
            clearOfferSubscription(localSub);
          }
          this.offerSubs.clear();
        }
        this.offerSubs = null;

        this.lock.notifyAll();
      } finally {
        context.currentConnection(prev);
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
        for (Map.Entry<OfferRegistrySubscriptionImpl,
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
        for (Map.Entry<OfferSubscriptionImpl,
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
    synchronized (lock) {
      // sem subscri��es, n�o h� nada a fazer.
      if ((maintainedOffers == null || maintainedOffers.size() == 0) &&
        (registrySubs == null || registrySubs.size() == 0) && (offerSubs ==
        null || offerSubs.size() == 0)) {
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
      // lan�ar assincronamente uma retrytask para refazer registros e
      // observadores de registros e de ofertas
      futureReLogin = pool.doTask(new ReLoginTask(context
        .getOfferRegistry()), new RetryContext(retryDelay, delayUnit));
      Futures.addCallback(futureReLogin, new FutureCallback<Void>() {
        @Override
        public void onSuccess(Void result) {
          // nulificar futureReLogin e notificar quem estiver esperando.
          synchronized (lock) {
            if (futureReLogin != null && !futureReLogin.isCancelled()) {
              futureReLogin = null;
            }
            lock.notifyAll();
          }
        }

        @Override
        public void onFailure(Throwable t) {
          // S� entra aqui se a aplica��o fez logout ou um outro relogin cancelou
          // esse. Basta desistir que o SDK cuida do login atual depois.
          logger.warn("Erro ao reinserir registros e observadores de oferta no " +
            "barramento devido a um logout ou relogin. Esse erro provavelmente " +
            "pode ser ignorado.", t);
          synchronized (lock) {
            lock.notifyAll();
          }
        }
      }, pool.pool());
    }
  }

  private void clearOfferSubscription(OfferSubscriptionImpl localSub) {
    Connection prev = this.context.currentConnection();
    try {
      this.context.currentConnection(conn);
      // best effort
      try {
        localSub.remove();
      } catch (Exception ignored) {}
    } finally {
      this.context.currentConnection(prev);
    }
  }

  private boolean doRegisterTask(tecgraf.openbus.core.v2_1.services
                                   .offer_registry.OfferRegistry registry,
                                 final LocalOfferImpl localOffer) {
    synchronized (lock) {
      OfferRegistryTask task = new OfferRegistryTask(localOffer, registry);
      OfferRegistryRetryContext retry = new OfferRegistryRetryContext
        (retryDelay, delayUnit, localOffer);
      ListenableFuture<RemoteOfferImpl> futureRegistry = pool.doTask(task,
        retry);
      // inserir a oferta a ser registrada no mapa
      maintainedOffers.put(localOffer, futureRegistry);
      Futures.addCallback(futureRegistry, new
          FutureCallback<RemoteOfferImpl>() {
            @Override
            public void onFailure(Throwable ex) {
              // so deve entrar aqui se a aplica��o escolheu fazer um
              // logout, ou se pararam as retentativas do RetryTask. Como
              // o tipo de retentativas � o OfferRegistryRetryContext que
              // retenta infinitamente, s� vai chegar aqui se o registro for
              // cancelado pelo usu�rio.
              synchronized (lock) {
                localOffer.remove();
                maintainedOffers.remove(localOffer);
              }
              logger.error("Erro ao registrar oferta no barramento, esse " +
                "pedido n�o ser� mais mantido pelo SDK.", ex);
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
    OfferRegistrySubscriptionImpl localSub) {
    synchronized (lock) {
      final Map<OfferRegistrySubscriptionImpl,
        ListenableFuture<OfferRegistryObserverSubscription>> registrySubs =
        registrySubs();
      if (registrySubs == null) {
        return false;
      }
      OfferRegistrySubscriptionTask task = new OfferRegistrySubscriptionTask
        (registry, localSub.proxy, convertMapToProperties(localSub.properties
          ()));
      ListenableFuture<OfferRegistryObserverSubscription> futureRegistrySub =
        pool.doTask(task, new OfferRegistrySubscriptionRetryContext
          (retryDelay, delayUnit, localSub));
      registrySubs.put(localSub, futureRegistrySub);
      Futures.addCallback(futureRegistrySub, new
          FutureCallback<OfferRegistryObserverSubscription>() {
            @Override
            public void onFailure(Throwable ex) {
              // so deve entrar aqui se a aplica��o escolheu fazer um
              // logout, ou se pararam as retentativas do RetryTask. Como
              // o tipo de retentativas � o InfiniteRetryContext que
              // retenta infinitamente, s� vai chegar aqui se a subscri��o for
              // cancelada pelo usu�rio.
              synchronized (lock) {
                localSub.remove();
              }
              logger.error("Erro ao inserir um observador de registro de " +
                "oferta no barramento.", ex);
            }

            @Override
            public void onSuccess(OfferRegistryObserverSubscription sub) {
              synchronized (lock) {
                // n�o h� como ter dois sucessos ao mesmo tempo, portanto
                // posso passar por cima do valor direto sem testar.
                localSub.sub(sub);
                registrySubs.put(localSub, null);
                logger.info("Observador de registro de ofertas cadastrado no" +
                  " barramento.");
              }
            }
          },
        pool.pool());
    }
    return true;
  }

  private boolean doSubscribeToOfferTask(final Map<OfferSubscriptionImpl,
      ListenableFuture<OfferObserverSubscription>> offerSubs, final
  OfferSubscriptionImpl localSub) {
    synchronized (lock) {
      if (localSub.offerDesc == null) {
        return false;
      }
      OfferSubscriptionTask task = new OfferSubscriptionTask(localSub.offerDesc
        .ref, localSub.proxy);
      ListenableFuture<OfferObserverSubscription> futureOfferSub =
        pool.doTask(task, new OfferSubscriptionRetryContext(retryDelay,
          delayUnit, localSub));
      offerSubs.put(localSub, futureOfferSub);
      Futures.addCallback(futureOfferSub, new
          FutureCallback<OfferObserverSubscription>() {
            @Override
            public void onFailure(Throwable ex) {
              // so deve entrar aqui se a aplica��o escolheu fazer um
              // logout, ou se pararam as retentativas do RetryTask. Como
              // o tipo de retentativas � o InfiniteRetryContext que
              // retenta infinitamente, s� vai chegar aqui se a subscri��o for
              // cancelada pelo usu�rio.
              synchronized (lock) {
                localSub.remove();
              }
              logger.error("Erro ao inserir um observador de oferta no " +
                "barramento", ex);
            }

            @Override
            public void onSuccess(OfferObserverSubscription sub) {
              synchronized (lock) {
                // n�o h� como ter dois sucessos ao mesmo tempo, portanto
                // posso passar por cima do valor direto sem testar.
                localSub.sub(sub);
                offerSubs.put(localSub, null);
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

  private Map<OfferRegistrySubscriptionImpl,
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

  private Map<OfferSubscriptionImpl,
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
  protected static ServiceProperty[] convertMapToProperties(
    ArrayListMultimap<String, String> properties) {
    ServiceProperty[] props = new ServiceProperty[properties.size()];
    int i = 0;
    for (Map.Entry<String, String> entry : properties.entries()) {
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
      context.currentConnection(conn);
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
        context.currentConnection(conn);
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
        context.currentConnection(conn);
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
        context.currentConnection(conn);
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
      context.currentConnection(conn);
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
        context.currentConnection(conn);
        return offer.subscribeObserver(observer);
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
      context.currentConnection(conn);
      synchronized (lock) {
        for (Map.Entry<LocalOfferImpl, ListenableFuture<RemoteOfferImpl>> entry
          : maintainedOffers.entrySet()) {
          LocalOfferImpl offer = entry.getKey();
          offer.removeOffer();
          ListenableFuture<RemoteOfferImpl> future = entry.getValue();
          if (future == null || future.isCancelled()) {
            doRegisterTask(registry, offer);
          }
        }
        for (Map.Entry<OfferRegistrySubscriptionImpl,
          ListenableFuture<OfferRegistryObserverSubscription>> entry :
          registrySubs.entrySet()) {
          OfferRegistrySubscriptionImpl localSub = entry.getKey();
          localSub.removeSub();
          ListenableFuture<OfferRegistryObserverSubscription> future = entry
            .getValue();
          if (future == null || future.isCancelled()) {
            doSubscribeToRegistryTask(OfferRegistryImpl.this.context
              .getOfferRegistry(), localSub);
          }
        }
        for (Map.Entry<OfferSubscriptionImpl,
          ListenableFuture<OfferObserverSubscription>> entry : offerSubs
          .entrySet()) {
          OfferSubscriptionImpl localSub = entry.getKey();
          ListenableFuture<OfferObserverSubscription> future = entry.getValue();
          if (future == null || future.isCancelled()) {
            if (!doSubscribeToOfferTask(offerSubs, localSub)) {
              // a oferta n�o existe mais, ent�o removemos a manuten��o do
              // observador
              localSub.remove();
            }
          }
        }
      }
      return null;
    }
  }
}
