package tecgraf.openbus.core;

import com.google.common.collect.ArrayListMultimap;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CORBA.TRANSIENT;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import scs.core.ComponentContext;
import scs.core.IComponent;
import scs.core.IMetaInterfaceHelper;
import scs.core.exception.SCSException;
import tecgraf.openbus.Connection;
import tecgraf.openbus.LocalOffer;
import tecgraf.openbus.LoginObserver;
import tecgraf.openbus.LoginRegistry;
import tecgraf.openbus.LoginSubscription;
import tecgraf.openbus.OfferObserver;
import tecgraf.openbus.OfferRegistry;
import tecgraf.openbus.OfferRegistryObserver;
import tecgraf.openbus.OfferRegistrySubscription;
import tecgraf.openbus.OfferSubscription;
import tecgraf.openbus.OnReloginCallback;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.RemoteOffer;
import tecgraf.openbus.core.v2_1.OctetSeqHolder;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.UnauthorizedOperation;
import tecgraf.openbus.core.v2_1.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidLogins;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_1.services.access_control.TooManyAttempts;
import tecgraf.openbus.core.v2_1.services.access_control.UnknownDomain;
import tecgraf.openbus.core.v2_1.services.access_control.WrongEncoding;
import tecgraf.openbus.core.v2_1.services.offer_registry.InvalidProperties;
import tecgraf.openbus.core.v2_1.services.offer_registry.InvalidService;
import tecgraf.openbus.core.v2_1.services.offer_registry.UnauthorizedFacets;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.util.Builder;
import tecgraf.openbus.utils.Configs;
import tecgraf.openbus.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("javadoc")
public class LocalAPITest {

  //private static String host;
  //private static int port;
  private static String ref;
  private static String entity;
  private static byte[] password;
  private static String admin;
  private static byte[] adminPsw;
  private static String domain;
  private static long sleepTime;
  private final TimeUnit sleepTimeUnit = TimeUnit.MILLISECONDS;

  @BeforeClass
  public static void oneTimeSetUp() throws Exception {
    Configs configs = Configs.readConfigsFile();
    Utils.setLibLogLevel(configs.log);
    //host = configs.bushost;
    //port = configs.busport;
    ref = configs.busref;
    entity = configs.user;
    password = configs.password;
    domain = configs.domain;
    admin = configs.admin;
    adminPsw = configs.admpsw;
    sleepTime = configs.sleepMsTime;
  }

  private Connection loginByPassword(boolean beAdmin) throws InvalidName,
    AdapterInactive, WrongEncoding, AlreadyLoggedIn, ServiceFailure,
    UnknownDomain, TooManyAttempts, AccessDenied, IOException {
    ORB orb = ORBInitializer.initORB();
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    Connection localConn = context.connectByReference(orb.string_to_object(new String(Utils.readFile(ref))));
    context.onCallDispatch((context1, busid, loginId, object_id, operation)
      -> localConn);
    if (beAdmin) {
      localConn.loginByPassword(admin, adminPsw, domain);
    } else {
      localConn.loginByPassword(entity, password, domain);
    }
    return localConn;
  }

  private LocalOffer buildAndRegister(Connection conn) throws
    InvalidName, AdapterInactive, SCSException {
    ComponentContext component = Builder.buildComponent(conn.context().ORB());
    ArrayListMultimap<String, String> properties = ArrayListMultimap.create();
    properties.put("offer.domain", "testing");
    return conn.offerRegistry().registerService(component.getIComponent(),
      properties);
  }

  @Test
  public void loginObserverTest() throws WrongPolicy, AdapterInactive,
    AlreadyLoggedIn, ServiceFailure, AccessDenied, ServantNotActive,
    TooManyAttempts, UnknownDomain, WrongEncoding, InvalidName, InvalidLogins, InterruptedException, IOException {
    Connection conn1 = loginByPassword(false);
    Connection conn2 = loginByPassword(false);
    LoginRegistry logins = conn1.loginRegistry();
    final LoginSubscription sub;
    final IdEqualityChecker ids = new IdEqualityChecker();
    LoginObserverTest observer = new LoginObserverTest();
    observer.ids = ids;
    ids.id2 = conn2.login().id;
    sub = logins.subscribeObserver(observer);
    observer.sub = sub;
    Thread.sleep(1000);
    assertSame(observer, sub.observer());
    assertSame(conn1, sub.connection());
    assertFalse(sub.watchLogin("invalid"));

    List<String> toWatch = new ArrayList<>();
    toWatch.add(conn1.login().id);
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
    toWatch.remove(conn2Id);
    observer.latchLogout.await(sleepTime, sleepTimeUnit);
    assertTrue(ids.check());
    sub.forgetLogin(conn1.login().id);
    assertTrue(sub.watchedLogins().size() == 0);

    sub.watchLogins(toWatch);
    assertTrue(sub.watchedLogins().size() == 1);
    sub.forgetLogins(toWatch);
    assertTrue(sub.watchedLogins().size() == 0);

    sub.remove();
    conn1.logout();
  }

  @Test
  public void registerTest() throws WrongPolicy, InvalidName,
    ServantNotActive, AdapterInactive, InvalidService, SCSException,
    UnauthorizedFacets, InvalidProperties, ServiceFailure,
    UnauthorizedOperation, InterruptedException, TooManyAttempts,
    AlreadyLoggedIn, AccessDenied, WrongEncoding, UnknownDomain, IOException,
    TimeoutException {
    Connection conn = loginByPassword(false);
    LocalOffer offer = buildAndRegister(conn);
    assertSame(conn, offer.connection());
    RemoteOffer remote = offer.remoteOffer(sleepTime);
    assertSame(conn, remote.connection());
    remote.remove();
    conn.logout();
  }

  @Test
  public void cancelledRegisterTest() throws WrongPolicy, InvalidName,
    ServantNotActive, AdapterInactive, InvalidService, SCSException,
    UnauthorizedFacets, InvalidProperties, ServiceFailure,
    UnauthorizedOperation, InterruptedException, TooManyAttempts,
    AlreadyLoggedIn, AccessDenied, WrongEncoding, UnknownDomain, IOException,
    TimeoutException {
    Connection conn = loginByPassword(false);
    LocalOffer offer = buildAndRegister(conn);
    offer.remove();
    assertNull(offer.remoteOffer(sleepTime));
    conn.logout();
  }

  @Test
  public void registerWithInvalidPropsTest() throws IOException,
    AdapterInactive, AlreadyLoggedIn, ServiceFailure, AccessDenied,
    TooManyAttempts, UnknownDomain, WrongEncoding, InvalidName, SCSException {
    Connection conn = loginByPassword(false);
    ComponentContext component = Builder.buildComponent(conn.context().ORB());
    ArrayListMultimap<String, String> properties = ArrayListMultimap.create();
    properties.put("openbus.offer.id", "-1");
    LocalOffer local = conn.offerRegistry().registerService(component
      .getIComponent(), properties);
    assertNotNull(local);
    assertSame(conn, local.connection());
    RemoteOffer remote = null;
    try {
      remote = local.remoteOffer(sleepTime);
    } catch (InvalidProperties ignored) {
      // recebeu exceção correta
    } catch (Exception e) {
      fail("Exceção incorreta recebida: " + e);
    }
    assertNull(remote);
    local.remove();
    conn.logout();
  }

  @Test
  public void allLoginsTest() throws IOException, WrongPolicy,
    AdapterInactive, AlreadyLoggedIn, ServiceFailure, AccessDenied,
    ServantNotActive, TooManyAttempts, UnknownDomain, WrongEncoding,
    InvalidName, UnauthorizedOperation {
    Connection conn = loginByPassword(true);
    LoginRegistry loginRegistry = conn.loginRegistry();
    List<LoginInfo> logins = loginRegistry.allLogins();
    assertNotNull(logins);
    assertTrue(logins.size() > 0);
  }

  @Test
  public void loginInfoTest() throws IOException, WrongPolicy,
    AdapterInactive, AlreadyLoggedIn, ServiceFailure, AccessDenied,
    ServantNotActive, TooManyAttempts, UnknownDomain, WrongEncoding,
    InvalidName, UnauthorizedOperation, InvalidLogins {
    Connection conn = loginByPassword(false);
    LoginRegistry loginRegistry = conn.loginRegistry();
    OctetSeqHolder pubKey = new OctetSeqHolder();
    LoginInfo login = loginRegistry.loginInfo(conn.login().id, pubKey);
    assertNotNull(login);
    assertEquals(conn.login().id, login.id);
    assertEquals(conn.login().entity, login.entity);
  }

  @Test
  public void findTest() throws WrongPolicy, InvalidName, ServantNotActive,
    AdapterInactive, InvalidService, SCSException, UnauthorizedFacets,
    InvalidProperties, ServiceFailure, InterruptedException, TooManyAttempts,
    AlreadyLoggedIn, AccessDenied, WrongEncoding, UnknownDomain,
    UnauthorizedOperation, IOException, TimeoutException {
    Connection conn = loginByPassword(false);
    OfferRegistry offers = conn.offerRegistry();
    LocalOffer anOffer = buildAndRegister(conn);
    assertSame(conn, anOffer.connection());
    RemoteOffer remote = anOffer.remoteOffer(sleepTime);
    assertSame(conn, remote.connection());

    ArrayListMultimap<String, String> findProps = ArrayListMultimap.create();
    findProps.put("offer.domain", "testing");
    List<RemoteOffer> foundServices = offers.findServices(findProps);
    assertTrue(foundServices.size() >= 1);
    boolean foundOne = false;
    // conexão corrente para a chamada getFacet abaixo
    conn.context().currentConnection(conn);
    for (RemoteOffer offer : foundServices) {
      assertSame(conn, offer.connection());
      IComponent ic = offer.service();
      try {
        IMetaInterfaceHelper.narrow(ic.getFacet(IMetaInterfaceHelper.id()));
        foundOne = true;
        break;
      } catch (TRANSIENT ignored) {}
    }
    assertTrue(foundOne);
    conn.logout();
  }

  @Test
  public void registryObserverTest() throws WrongPolicy, AdapterInactive,
    AlreadyLoggedIn, ServiceFailure, AccessDenied, ServantNotActive,
    TooManyAttempts, UnknownDomain, WrongEncoding, InvalidName,
    InvalidService, SCSException, UnauthorizedFacets, InvalidProperties,
    InterruptedException, IOException, TimeoutException {
    Connection conn = loginByPassword(false);
    OfferRegistry offers = conn.offerRegistry();
    ArrayListMultimap<String, String> properties = ArrayListMultimap.create();
    properties.put("offer.domain", "testing");
    final CountDownLatch latchRegister = new CountDownLatch(1);
    final IdEqualityChecker ids = new
      IdEqualityChecker();
    OfferRegistryObserver observer = offer -> {
      ids.id1 = OfferRegistryImpl
        .getOfferIdFromProperties(((RemoteOfferImpl)offer).offer());
      latchRegister.countDown();
    };
    OfferRegistrySubscription sub = offers.subscribeObserver(observer,
      properties);
    assertTrue(sub.subscribed(sleepTime));
    assertSame(observer, sub.observer());
    assertTrue(sub.properties().equals(properties));
    assertSame(conn, sub.connection());

    LocalOffer anOffer = buildAndRegister(conn);
    RemoteOffer remote = anOffer.remoteOffer(sleepTime);
    ids.id2 = OfferRegistryImpl.getOfferIdFromProperties((
      (RemoteOfferImpl)remote).offer());
    assertTrue(latchRegister.await(sleepTime, sleepTimeUnit));
    assertTrue(ids.check());

    sub.remove();
    conn.logout();
  }

  @Test
  public void cancelledRegistryObserverTest() throws WrongPolicy, InvalidName,
    ServantNotActive, AdapterInactive, InvalidService, SCSException,
    UnauthorizedFacets, InvalidProperties, ServiceFailure,
    UnauthorizedOperation, InterruptedException, TooManyAttempts,
    AlreadyLoggedIn, AccessDenied, WrongEncoding, UnknownDomain, IOException,
    TimeoutException {
    Connection conn = loginByPassword(false);
    OfferRegistry offers = conn.offerRegistry();
    ArrayListMultimap<String, String> properties = ArrayListMultimap.create();
    properties.put("offer.domain", "testing");
    OfferRegistryObserver observer = offer -> {};
    OfferRegistrySubscription sub = offers.subscribeObserver(observer,
      properties);
    sub.remove();
    assertFalse(sub.subscribed(sleepTime));
    sub = offers.subscribeObserver(observer, properties);
    assertTrue(sub.subscribed(sleepTime));
    sub.remove();
    assertFalse(sub.subscribed(sleepTime));
    conn.logout();
  }

  @Test
  public void offerObserverTest() throws WrongPolicy, AdapterInactive,
    AlreadyLoggedIn, ServiceFailure, AccessDenied, ServantNotActive,
    TooManyAttempts, UnknownDomain, WrongEncoding, InvalidName,
    InvalidService, SCSException, UnauthorizedFacets, InvalidProperties,
    InterruptedException, UnauthorizedOperation, IOException,
    TimeoutException {
    Connection conn = loginByPassword(false);
    LocalOffer anOffer = buildAndRegister(conn);
    final RemoteOffer remote = anOffer.remoteOffer(sleepTime);
    final CountDownLatch latchPropsChanged = new CountDownLatch(1);
    final CountDownLatch latchRemoved = new CountDownLatch(1);
    final IdEqualityChecker ids = new
      IdEqualityChecker();
    OfferObserver observer = new OfferObserver() {
      @Override
      public void propertiesChanged(RemoteOffer offer) {
        RemoteOfferImpl offerImpl = (RemoteOfferImpl)offer;
        RemoteOfferImpl remoteImpl = (RemoteOfferImpl)remote;
        assertTrue(Boolean.parseBoolean(offerImpl.properties(false).get(
          "offer.altered").get(0)));
        // testa props locais se foram atualizadas
        assertTrue(remoteImpl.properties(false).entries().containsAll
          (offerImpl.properties(false).entries()));
        // atualiza props locais manualmente
        ArrayListMultimap<String, String> updatedProps = ArrayListMultimap
          .create(remoteImpl.properties(false));
        remoteImpl.updateProperties(updatedProps);
        assertTrue(remoteImpl.properties(false).entries().containsAll
          (offerImpl.properties(false).entries()));
        // testa props locais atualizadas de acordo com o estado do barramento
        assertTrue(remoteImpl.properties(true).entries().containsAll(offerImpl
          .properties(false).entries()));
        // seta informação de offer id para checagem posterior
        ids.id1 = OfferRegistryImpl.getOfferIdFromProperties((
          (RemoteOfferImpl)offer).offer());
        latchPropsChanged.countDown();
      }

      @Override
      public void removed(RemoteOffer offer) {
        ids.id2 = OfferRegistryImpl.getOfferIdFromProperties((
          (RemoteOfferImpl)offer).offer());
        latchRemoved.countDown();
      }
    };
    OfferSubscription sub = remote.subscribeObserver(observer);
    assertTrue(sub.subscribed(sleepTime));
    assertSame(observer, sub.observer());
    assertSame(conn, sub.connection());

    ArrayListMultimap<String, String> properties = ArrayListMultimap.create();
    properties.put("offer.domain", "testing");
    properties.put("offer.altered", "true");
    remote.properties(properties);
    assertTrue(latchPropsChanged.await(sleepTime, sleepTimeUnit));
    ids.id2 = OfferRegistryImpl.getOfferIdFromProperties((
      (RemoteOfferImpl)remote).offer());
    assertTrue(ids.check());
    remote.remove();
    assertTrue(latchRemoved.await(sleepTime, sleepTimeUnit));
    assertTrue(ids.check());

    sub.remove();
    conn.logout();
  }

  @Test
  public void cancelledOfferObserverTest() throws WrongPolicy, InvalidName,
    ServantNotActive, AdapterInactive, InvalidService, SCSException,
    UnauthorizedFacets, InvalidProperties, ServiceFailure,
    UnauthorizedOperation, InterruptedException, TooManyAttempts,
    AlreadyLoggedIn, AccessDenied, WrongEncoding, UnknownDomain, IOException,
    TimeoutException {
    Connection conn1 = loginByPassword(false);
    ComponentContext component = Builder.buildComponent(conn1.context().ORB());
    ArrayListMultimap<String, String> properties = ArrayListMultimap.create();
    properties.put("offer.domain", "testing");
    properties.put("time", "" + System.currentTimeMillis());
    LocalOffer anOffer = conn1.offerRegistry().registerService(component
      .getIComponent(), properties);
    anOffer.remoteOffer(sleepTime);

    Connection conn2 = loginByPassword(false);
    RemoteOffer remote = conn2.offerRegistry().findServices(properties).get(0);
    assertSame(conn2, remote.connection());
    CountDownLatch latchOfferRemoved = new CountDownLatch(1);
    OfferObserver observer = new OfferObserver() {
      @Override
      public void propertiesChanged(RemoteOffer offer) {}

      @Override
      public void removed(RemoteOffer offer) {
        latchOfferRemoved.countDown();
      }
    };
    OfferSubscription sub = remote.subscribeObserver(observer);
    sub.remove();
    assertFalse(sub.subscribed(sleepTime));
    sub = remote.subscribeObserver(observer);
    assertTrue(sub.subscribed(sleepTime));
    // remove através do objeto da conn1
    anOffer.remoteOffer(sleepTime).remove();
    // Subscrição tem que saber que foi removida e se remover localmente e do
    // barramento.
    assertTrue(latchOfferRemoved.await(sleepTime, sleepTimeUnit));
    assertFalse(sub.subscribed(sleepTime));
    conn1.logout();
    conn2.logout();
  }

  @Test
  public void onInvalidLoginCallbackTest() throws Exception {
    Connection conn1 = loginByPassword(false);
    assertNotNull(conn1);
    assertSame(conn1, conn1.loginRegistry().connection());
    assertSame(conn1, conn1.offerRegistry().connection());
    Connection conn2 = loginByPassword(false);
    assertNotNull(conn2);
    assertSame(conn2, conn2.loginRegistry().connection());
    assertSame(conn2, conn2.offerRegistry().connection());

    // registra observador de login da conn2 na conn1
    final String conn2LoginId = conn2.login().id;
    final CountDownLatch latchLoginRemoved1 = new CountDownLatch(1);
    LoginSubscription loginSub1 = conn1.loginRegistry().subscribeObserver(new
      LoginObserver() {
      @Override
      public void entityLogout(LoginInfo login) {
        if (login.id.equals(conn2LoginId)) {
          latchLoginRemoved1.countDown();
        }
      }

      @Override
      public void nonExistentLogins(String[] loginIds) {}
    });
    assertNotNull(loginSub1);
    assertSame(conn1, loginSub1.connection());
    loginSub1.watchLogin(conn2LoginId);

    // registra observador de login da conn1 na conn2
    final String conn1LoginId = conn1.login().id;
    final CountDownLatch latchLoginRemoved2 = new CountDownLatch(1);
    LoginSubscription loginSub2 = conn2.loginRegistry().subscribeObserver(new
      LoginObserver() {
      @Override
      public void entityLogout(LoginInfo login) {
        if (login.id.equals(conn1LoginId)) {
          latchLoginRemoved2.countDown();
        }
      }

     @Override
     public void nonExistentLogins(String[] loginIds) {}
   });
    assertNotNull(loginSub2);
    assertSame(conn2, loginSub2.connection());
    loginSub2.watchLogin(conn1LoginId);

    // registra oferta na conn1
    OfferRegistry offers1 = conn1.offerRegistry();
    ComponentContext component1 = Builder.buildComponent(offers1.connection()
      .context().ORB());
    ArrayListMultimap<String, String> properties1 = ArrayListMultimap.create();
    properties1.put("offer.domain", "testing");
    properties1.put("time", "" + System.currentTimeMillis());
    LocalOffer localOffer1 = offers1.registerService(component1.getIComponent(),
      properties1);
    assertSame(conn1, localOffer1.connection());
    RemoteOffer remote1 = localOffer1.remoteOffer(sleepTime);
    assertSame(conn1, remote1.connection());

    // registra oferta na conn2
    OfferRegistry offers2 = conn2.offerRegistry();
    ComponentContext component2 = Builder.buildComponent(offers2.connection()
      .context().ORB());
    ArrayListMultimap<String, String> properties2 = ArrayListMultimap.create();
    properties2.put("offer.domain", "testing");
    properties2.put("time", "" + System.currentTimeMillis());
    LocalOffer localOffer2 = offers2.registerService(component2.getIComponent(),
      properties2);
    assertSame(conn2, localOffer2.connection());
    RemoteOffer remote2 = localOffer2.remoteOffer(sleepTime);
    assertSame(conn2, remote2.connection());

    // cadastra observador da oferta1 na conn2
    RemoteOffer remoteFromOffer2 = conn1.offerRegistry().findServices
      (properties2).get(0);
    assertSame(conn1, remoteFromOffer2.connection());
    final CountDownLatch latchPropsChanged = new CountDownLatch(1);
    final IdEqualityChecker ids1 = new IdEqualityChecker();
    // vai comparar o id encontrado da oferta2 com o id que tiver as
    // propriedades alteradas
    OfferObserver observer1 = new OfferObserver() {
      @Override
      public void propertiesChanged(RemoteOffer offer) {
        RemoteOfferImpl offerImpl = (RemoteOfferImpl)offer;
        RemoteOfferImpl remoteImpl = null;
        try {
          remoteImpl = (RemoteOfferImpl)localOffer2.remoteOffer(sleepTime);
        } catch (Exception e) {
          e.printStackTrace();
          fail("Erro ao obter a oferta remota.");
        }
        assertTrue(Boolean.parseBoolean(offerImpl.properties(false).get(
          "offer.altered").get(0)));
        // testa props locais desatualizadas
        assertFalse(remoteImpl.properties(false).entries().containsAll
          (offerImpl.properties(false).entries()));
        // atualiza props locais manualmente
        ArrayListMultimap<String, String> updatedProps = ArrayListMultimap
          .create(offerImpl.properties(false));
        remoteImpl.updateProperties(updatedProps);
        assertTrue(remoteImpl.properties(false).entries().containsAll
          (offerImpl.properties(false).entries()));
        // testa props locais atualizadas de acordo com o estado do barramento
        assertTrue(remoteImpl.properties(true).entries().containsAll(offerImpl
          .properties(false).entries()));
        // seta informação de offer id para checagem posterior
        ids1.id1 = OfferRegistryImpl.getOfferIdFromProperties((
          (RemoteOfferImpl)offer).offer());
        latchPropsChanged.countDown();
      }

      @Override
      public void removed(RemoteOffer offer) {
      }
    };
    OfferSubscription sub1 = remoteFromOffer2.subscribeObserver
      (observer1);
    assertTrue(sub1.subscribed(sleepTime));
    assertSame(observer1, sub1.observer());
    assertSame(conn1, sub1.connection());

    // cadastra observador da oferta1 na conn2
    RemoteOffer remoteFromOffer1 = conn2.offerRegistry().findServices
      (properties1).get(0);
    assertSame(conn2, remoteFromOffer1.connection());
    final CountDownLatch latchOfferRemoved = new CountDownLatch(1);
    final IdEqualityChecker ids2 = new IdEqualityChecker();
    // vai comparar o id encontrado da oferta1 com o id que for removido
    ids2.id1 = OfferRegistryImpl.getOfferIdFromProperties((
      (RemoteOfferImpl)remoteFromOffer1).offer());
    OfferObserver observer2 = new OfferObserver() {
      @Override
      public void propertiesChanged(RemoteOffer offer) {
      }

      @Override
      public void removed(RemoteOffer offer) {
        ids2.id2 = OfferRegistryImpl.getOfferIdFromProperties((
          (RemoteOfferImpl)offer).offer());
        latchOfferRemoved.countDown();
      }
    };
    OfferSubscription sub2 = remoteFromOffer1.subscribeObserver
      (observer2);
    assertTrue(sub2.subscribed(sleepTime));
    assertSame(observer2, sub2.observer());
    assertSame(conn2, sub2.connection());

    // cadastra callback de relogin na conn1 para verificar se foi chamada
    final CountDownLatch latchReloginCallback = new CountDownLatch(1);
    OnReloginCallback onRelogin = (connection, oldLogin) ->
      latchReloginCallback.countDown();
    conn1.onReloginCallback(onRelogin);
    // inutiliza o login da conn1
    String id = conn1.login().id;
    Connection adminconn = loginByPassword(true);
    adminconn.loginRegistry().invalidateLogin(id);
    int validity = adminconn.loginRegistry().loginValidity(id);
    assertTrue(validity <= 0);
    adminconn.logout();

    // observador de login da conn2 tem que ter recebido a notificação de
    // remoção do login
    assertTrue(latchLoginRemoved2.await(sleepTime, TimeUnit
      .MILLISECONDS));
    // observador de oferta da conn2 tem que ter recebido a notificação de
    // remoção da oferta
    assertTrue(latchOfferRemoved.await(sleepTime, sleepTimeUnit));
    assertTrue(ids2.check());
    // alem disso, observador tem que ter sido removido.
    assertFalse(sub2.subscribed(sleepTime));

    // refaz login da conn1
    conn1.loginRegistry().entityLogins(entity);
    validity = conn1.loginRegistry().loginValidity(conn1.login().id);
    assertTrue(validity > 0);

    // oferta e observador têm que estar de volta
    RemoteOffer newRemote1 = localOffer1.remoteOffer(sleepTime);
    assertNotNull(newRemote1);
    assertTrue(sub1.subscribed(sleepTime));

    // callback onRelogin da aplicação tem que ter sido chamada
    assertTrue(latchReloginCallback.await(sleepTime, TimeUnit.MILLISECONDS));

    // altera propriedades da conn2
    ArrayListMultimap<String, String> properties = ArrayListMultimap.create();
    properties.put("offer.domain", "testing");
    properties.put("time", "" + System.currentTimeMillis());
    properties.put("offer.altered", "true");
    remote2.properties(properties);
    assertTrue(latchPropsChanged.await(sleepTime, sleepTimeUnit));
    ids1.id2 = OfferRegistryImpl.getOfferIdFromProperties((
      (RemoteOfferImpl)remoteFromOffer2).offer());
    assertTrue(ids1.check());

    conn2.logout();

    // observador de login da conn1 tem que estar de volta e ter recebido
    // notificação de logout da conn2
    assertTrue(latchLoginRemoved1.await(sleepTime, TimeUnit.MILLISECONDS));
    conn1.logout();
  }

  private class IdEqualityChecker {
    public String id1;
    public String id2;

    public boolean check() {
      return id1.equals(id2);
    }
  }

  private class LoginObserverTest implements LoginObserver {
    public LoginSubscription sub;
    public CountDownLatch latchLogout = new CountDownLatch(1);
    public IdEqualityChecker ids;

    @Override
    public void entityLogout(LoginInfo login) {
      ids.id1 = login.id;
      latchLogout.countDown();
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
