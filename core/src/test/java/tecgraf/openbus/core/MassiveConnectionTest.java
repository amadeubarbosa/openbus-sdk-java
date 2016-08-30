package tecgraf.openbus.core;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.ArrayListMultimap;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;

import scs.core.ComponentContext;
import tecgraf.openbus.*;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.util.Builder;
import tecgraf.openbus.utils.Configs;
import tecgraf.openbus.utils.Utils;

public class MassiveConnectionTest {

  private static Object busref;
  private static String domain;
  private static ORB orb;
  private static OpenBusContext context;
  private static final int LOOP_SIZE = 20;
  private static int THREAD_POOL_SIZE = 8;

  @BeforeClass
  public static void oneTimeSetUp() throws Exception {
    Configs configs = Configs.readConfigsFile();
    Utils.setLibLogLevel(configs.log);
    domain = configs.domain;
    orb =
      ORBInitializer.initORB(null, Utils.readPropertyFile(configs.orbprops));
    busref = orb.string_to_object(new String(Utils.readFile(configs.busref)));
    context = (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
  }

  @Test
  public void massiveTest() throws Exception {
    String entity = "dispatcher";
    final Connection conn = context.connectByReference(busref);
    conn.loginByPassword(entity, entity.getBytes(), domain);
    context.onCallDispatch((context1, busid, loginId, object_id, operation)
      -> conn);
    ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    AtomicBoolean failed = new AtomicBoolean(false);
    for (int i = 0; i < LOOP_SIZE; i++) {
      threadPool.execute(new ConnectThread(context, i, failed));
    }
    threadPool.shutdown();
    try {
      if (!threadPool.awaitTermination(LOOP_SIZE / 2, TimeUnit.MINUTES)) {
        failed.set(true);
      }
    }
    finally {
      conn.logout();
    }
    assertFalse(failed.get());
  }

  private class ConnectThread implements Runnable {

    private OpenBusContext context;
    private String entity;
    private AtomicBoolean failed;
    private long remoteSleepTime = THREAD_POOL_SIZE * 1000;
    private long sleepTime = THREAD_POOL_SIZE * 2 / 3 * 1000;
    private long logoutSleepTime = 1000;

    public ConnectThread(OpenBusContext multiplexer, int id, AtomicBoolean
      failed) {
      this.context = multiplexer;
      this.entity = "Task-" + id;
      this.failed = failed;
    }

    @Override
    public void run() {
      try {
        // register
        Connection conn = context.connectByReference(busref);
        conn.loginByPassword(entity, entity.getBytes(), domain);
        final Connection tempConn = conn;
        context.onCallDispatch((openBusContext, s, s1, bytes, s2) -> tempConn);
        ComponentContext component = Builder.buildComponent(orb);
        ArrayListMultimap<String, String> props = ArrayListMultimap.create();
        props.put("offer.domain", "Massive Test");
        props.put("thread.id", entity);
        props.put("time", "" + System.currentTimeMillis());
        LocalOffer local = conn.offerRegistry().registerService(component
          .getIComponent(), props);
        assertNotNull(local.remoteOffer(remoteSleepTime, 0));
        List<RemoteOffer> services = conn.offerRegistry().findServices(props);
        if (services.size() != 1) {
          failed.set(true);
        }
        RemoteOffer offer = services.remove(0);
        if (offer == null) {
          failed.set(true);
        } else {
          offer.remove();
        }

        // login observer
        Semaphore mutex = new Semaphore(0);
        Connection conn2 = context.connectByReference(busref);
        conn2.loginByPassword(entity, entity.getBytes(), domain);
        LoginRegistry logins = conn.loginRegistry();
        final LoginSubscription sub;
        IdEqualityChecker ids = new IdEqualityChecker();
        LoginObserverTest observer = new LoginObserverTest();
        observer.ids = ids;
        observer.mutex = mutex;
        ids.id2 = conn2.login().id;
        sub = logins.subscribeObserver(observer);
        observer.sub = sub;
        assertSame(observer, sub.observer());
        assertFalse(sub.watchLogin("invalid"));

        List<String> toWatch = new ArrayList<>();
        toWatch.add(conn.login().id);
        String conn2Id = conn2.login().id;
        toWatch.add(conn2Id);
        sub.watchLogins(toWatch);
        List<LoginInfo> watched = sub.watchedLogins();
        List<String> watchedLogins = new ArrayList<>();
        for (LoginInfo info : watched) {
          watchedLogins.add(info.id);
        }
        assertTrue(watchedLogins.containsAll(toWatch));
        conn2.logout();
        mutex.acquire();
        assertTrue(ids.check());
        toWatch.remove(conn2Id);
        sub.forgetLogin(conn.login().id);
        assertTrue(sub.watchedLogins().size() == 0);

        sub.watchLogins(toWatch);
        assertTrue(sub.watchedLogins().size() == 1);
        sub.forgetLogins(toWatch);
        assertTrue(sub.watchedLogins().size() == 0);
        sub.remove();

        // offer registry observer
        final IdEqualityChecker ids2 = new
          IdEqualityChecker();
        final Semaphore tempMutex = mutex;
        OfferRegistryObserver regObserver = offer1 -> {
          ids2.id1 = OfferRegistryImpl.getOfferIdFromProperties((
            (RemoteOfferImpl) offer1).offer());
          tempMutex.release();
      };
        OfferRegistrySubscription regSub = conn.offerRegistry()
          .subscribeObserver(regObserver, props);
        // aguarda ocorrer a subscrição
        Thread.sleep(sleepTime);
        assertSame(regObserver, regSub.observer());
        assertTrue(regSub.properties().equals(props));

        local = conn.offerRegistry().registerService(component
          .getIComponent(), props);

        RemoteOffer remote = local.remoteOffer(remoteSleepTime, 0);
        ids2.id2 = OfferRegistryImpl.getOfferIdFromProperties((
          (RemoteOfferImpl)remote).offer());
        mutex.acquire();
        assertTrue(ids2.check());
        regSub.remove();

        // offer observer
        conn2.loginByPassword(entity, entity.getBytes(), domain);
        RemoteOffer remoteConn1 = local.remoteOffer(remoteSleepTime, 0);
        assertNotNull(remoteConn1);
        final Connection tempConn2 = conn2;
        context.onCallDispatch((openBusContext, s, s1, bytes, s2) -> tempConn2);
        final RemoteOffer remoteConn2 = conn2.offerRegistry().findServices
          (props).get(0);
        assertNotNull(remoteConn2);
        final IdEqualityChecker ids3 = new IdEqualityChecker();
        OfferObserver offerObserver = new OfferObserver() {
          @Override
          public void propertiesChanged(RemoteOffer offer) {
            assertTrue(Boolean.parseBoolean(offer.properties(false).get(
              "offer.altered").get(0)));
            // testa props locais se foram atualizadas
            assertTrue(remoteConn2.properties(false).entries().containsAll
              (offer.properties(false).entries()));
            // atualiza props locais manualmente
            ArrayListMultimap<String, String> updatedProps =
              ArrayListMultimap.create(remoteConn2.properties(false));
            remoteConn2.propertiesLocal(updatedProps);
            assertTrue(remoteConn2.properties(false).entries().containsAll(offer
              .properties(false).entries()));
            // testa props locais atualizadas de acordo com o estado do barramento
            assertTrue(remoteConn2.properties(true).entries().containsAll(offer
              .properties(false).entries()));
            // seta informação de offer id para checagem posterior
            ids3.id1 = OfferRegistryImpl.getOfferIdFromProperties((
              (RemoteOfferImpl)offer).offer());
            tempMutex.release();
          }

          @Override
          public void removed(RemoteOffer offer) {
            ids3.id2 = OfferRegistryImpl.getOfferIdFromProperties((
              (RemoteOfferImpl)offer).offer());
            tempMutex.release();
          }
        };
        OfferSubscription offerSub = remoteConn2.subscribeObserver
          (offerObserver);
        assertSame(offerObserver, offerSub.observer());
        //aguarda ocorrer a subscrição
        Thread.sleep(sleepTime);

        props.put("offer.altered", "true");
        remoteConn1.propertiesRemote(props);
        mutex.acquire();
        ids3.id2 = OfferRegistryImpl.getOfferIdFromProperties((
          (RemoteOfferImpl)remoteConn1).offer());
        assertTrue(ids3.check());
        remoteConn1.remove();
        mutex.acquire();
        assertTrue(ids3.check());

        offerSub.remove();
        Thread.sleep(logoutSleepTime);
        conn.logout();
        conn2.logout();
      }
      catch (Exception e) {
        failed.set(true);
        e.printStackTrace();
      }
    }

    private class IdEqualityChecker {
      public String id1;
      public String id2;

      public boolean check() {
        return id1.equals(id2);
      }
    }

    private class LoginObserverTest implements tecgraf.openbus.LoginObserver {
      public LoginSubscription sub;
      public IdEqualityChecker ids;
      public Semaphore mutex;

      @Override
      public void entityLogout(LoginInfo login) {
        ids.id1 = login.id;
        mutex.release();
      }

      @Override
      public void nonExistentLogins(String[] loginIds) {
        if (sub != null) {
          List<String> forget = new ArrayList<>(loginIds.length);
          Collections.addAll(forget, loginIds);
          try {
            sub.forgetLogins(forget);
          } catch (ServiceFailure ignored) {}
        }
      }
    }
  }
}
