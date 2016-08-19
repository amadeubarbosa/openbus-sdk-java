package tecgraf.openbus.core;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
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
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.UnauthorizedOperation;
import tecgraf.openbus.core.v2_1.services.access_control.*;
import tecgraf.openbus.core.v2_1.services.offer_registry.InvalidProperties;
import tecgraf.openbus.core.v2_1.services.offer_registry.InvalidService;
import tecgraf.openbus.core.v2_1.services.offer_registry.UnauthorizedFacets;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.*;
import tecgraf.openbus.LoginObserver;
import tecgraf.openbus.LoginRegistry;
import tecgraf.openbus.util.Builder;
import tecgraf.openbus.utils.Configs;
import tecgraf.openbus.utils.Utils;

public class LocalAPITest {

  //private static String host;
  //private static int port;
  private static String ref;
  private static String entity;
  private static byte[] password;
  private static String domain;

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
  }

  private Connection loginByPassword() throws ServantNotActive,
    WrongPolicy, InvalidName, AdapterInactive, WrongEncoding, AlreadyLoggedIn, ServiceFailure, UnknownDomain, TooManyAttempts, AccessDenied, IOException {
    ORB orb = ORBInitializer.initORB();
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    Connection localConn = context.connectByReference(orb.string_to_object(new String(Utils.readFile(ref))));
    context.onCallDispatch((context1, busid, loginId, object_id, operation)
      -> localConn);
    localConn.loginByPassword(entity, password, domain);
    return localConn;
  }

  private LocalOffer buildAndRegister(OfferRegistry offers) throws
    InvalidName, AdapterInactive, SCSException, ServantNotActive,
    UnauthorizedFacets, WrongPolicy, InvalidService, InvalidProperties,
    ServiceFailure {
    ComponentContext component = Builder.buildComponent(offers.conn().context()
      .orb());
    Map<String, String> properties = new HashMap<>();
    properties.put("offer.domain", "testing");
    return offers.registerService(component.getIComponent(), properties);
  }

  @Test
  public void loginObserverTest() throws WrongPolicy, AdapterInactive,
    AlreadyLoggedIn, ServiceFailure, AccessDenied, ServantNotActive,
    TooManyAttempts, UnknownDomain, WrongEncoding, InvalidName, InvalidLogins, InterruptedException, IOException {
    Connection conn = loginByPassword();
    Connection conn2 = loginByPassword();
    LoginRegistry logins = conn.loginRegistry();
    final LoginSubscription sub;
    final IdEqualityChecker ids = new IdEqualityChecker();
    LoginObserverTest observer = new LoginObserverTest();
    observer.ids = ids;
    ids.id2 = conn2.login().id;
    sub = logins.subscribeObserver(observer);
    observer.sub = sub;
    Thread.sleep(1000);
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
    toWatch.remove(conn2Id);
    Thread.sleep(1000);
    assertTrue(ids.check());
    sub.forgetLogin(conn.login().id);
    assertTrue(sub.watchedLogins().size() == 0);

    sub.watchLogins(toWatch);
    assertTrue(sub.watchedLogins().size() == 1);
    sub.forgetLogins(toWatch);
    assertTrue(sub.watchedLogins().size() == 0);

    sub.remove();
    conn.logout();
  }

  @Test
  public void registerTest() throws WrongPolicy, InvalidName,
    ServantNotActive, AdapterInactive, InvalidService, SCSException,
    UnauthorizedFacets, InvalidProperties, ServiceFailure,
    UnauthorizedOperation, InterruptedException, TooManyAttempts, AlreadyLoggedIn, AccessDenied, WrongEncoding, UnknownDomain, IOException {
    Connection conn = loginByPassword();
    LocalOffer offer = buildAndRegister(conn.offerRegistry());
    RemoteOffer remote = offer.remoteOffer(1000, 0);
    remote.remove();
    conn.logout();
  }

  @Test
  public void findTest() throws WrongPolicy, InvalidName, ServantNotActive,
    AdapterInactive, InvalidService, SCSException, UnauthorizedFacets,
    InvalidProperties, ServiceFailure, InterruptedException, TooManyAttempts,
    AlreadyLoggedIn, AccessDenied, WrongEncoding, UnknownDomain,
    UnauthorizedOperation, IOException {
    Connection conn = loginByPassword();
    OfferRegistry offers = conn.offerRegistry();
    LocalOffer anOffer = buildAndRegister(offers);
    anOffer.remoteOffer(1000, 0);

    Map<String, String> findProps = new HashMap<>();
    findProps.put("offer.domain", "testing");
    List<RemoteOffer> foundServices = offers.findServices(findProps);
    assertTrue(foundServices.size() >= 1);
    boolean foundOne = false;
    // conexão corrente para a chamada getFacet abaixo
    conn.context().setCurrentConnection(conn);
    for (RemoteOffer offer : foundServices) {
      IComponent ic = offer.service_ref();
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
      InterruptedException, IOException {
      Connection conn = loginByPassword();
      OfferRegistry offers = conn.offerRegistry();
      Map<String, String> properties = new HashMap<>();
      properties.put("offer.domain", "testing");
      final IdEqualityChecker ids = new
        IdEqualityChecker();
      OfferRegistryObserver observer = offer -> ids.id1 = OfferRegistryImpl
        .getOfferIdFromProperties(((RemoteOfferImpl)offer).offer());
      OfferRegistrySubscription sub = offers.subscribeObserver(observer,
        properties);
      assertSame(observer, sub.observer());
      assertTrue(sub.properties().equals(properties));

      LocalOffer anOffer = buildAndRegister(offers);
      RemoteOffer remote = anOffer.remoteOffer(1000, 0);
      ids.id2 = OfferRegistryImpl.getOfferIdFromProperties((
        (RemoteOfferImpl)remote).offer());
      Thread.sleep(1000);
      assertTrue(ids.check());

      sub.remove();
      conn.logout();
    }

    @Test
    public void offerObserverTest() throws WrongPolicy, AdapterInactive,
      AlreadyLoggedIn, ServiceFailure, AccessDenied, ServantNotActive,
      TooManyAttempts, UnknownDomain, WrongEncoding, InvalidName,
      InvalidService, SCSException, UnauthorizedFacets, InvalidProperties,
      InterruptedException, UnauthorizedOperation, IOException {
      Connection conn = loginByPassword();
      OfferRegistry offers = conn.offerRegistry();
      LocalOffer anOffer = buildAndRegister(offers);
      final RemoteOffer remote = anOffer.remoteOffer(3000, 0);
      final IdEqualityChecker ids = new
        IdEqualityChecker();
      OfferObserver observer = new OfferObserver() {
        @Override
        public void propertiesChanged(RemoteOffer offer) {
          assertTrue(Boolean.parseBoolean(offer.properties(false).get(
            "offer.altered")));
          // testa props locais se foram atualizadas
          assertTrue(remote.properties(false).entrySet().containsAll(offer
            .properties(false).entrySet()));
          // atualiza props locais manualmente
          Map<String, String> updatedProps = new HashMap<>(remote.properties
            (false));
          remote.propertiesLocal(updatedProps);
          assertTrue(remote.properties(false).entrySet().containsAll(offer
            .properties(false).entrySet()));
          // testa props locais atualizadas de acordo com o estado do barramento
          assertTrue(remote.properties(true).entrySet().containsAll(offer
            .properties(false).entrySet()));
          // seta informação de offer id para checagem posterior
          ids.id1 = OfferRegistryImpl.getOfferIdFromProperties((
            (RemoteOfferImpl)offer).offer());
        }

        @Override
        public void removed(RemoteOffer offer) {
          ids.id2 = OfferRegistryImpl.getOfferIdFromProperties((
            (RemoteOfferImpl)offer).offer());
        }
      };
      OfferSubscription sub = remote.subscribeObserver(observer);
      assertSame(observer, sub.observer());

      Map<String, String> properties = new HashMap<>();
      properties.put("offer.domain", "testing");
      properties.put("offer.altered", "true");
      remote.propertiesRemote(properties);
      Thread.sleep(1000);
      ids.id2 = OfferRegistryImpl.getOfferIdFromProperties((
        (RemoteOfferImpl)remote).offer());
      assertTrue(ids.check());
      remote.remove();
      Thread.sleep(1000);
      assertTrue(ids.check());

      sub.remove();
      conn.logout();
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
    public IdEqualityChecker ids;

    @Override
    public void entityLogout(LoginInfo login) {
      ids.id1 = login.id;
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
