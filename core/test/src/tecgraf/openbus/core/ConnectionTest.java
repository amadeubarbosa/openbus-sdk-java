package tecgraf.openbus.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.interfaces.RSAPrivateKey;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TRANSIENT;

import scs.core.ComponentContext;
import tecgraf.openbus.CallDispatchCallback;
import tecgraf.openbus.Connection;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.SharedAuthSecret;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_1.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.offer_registry.OfferRegistry;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.exception.InvalidPropertyValue;
import tecgraf.openbus.security.Cryptography;
import tecgraf.openbus.util.Utils;

public final class ConnectionTest {

	private static String host;
	private static int port;
	private static String entity;
	private static String password;
	private static String serverEntity;
	private static String privateKeyFile;
	private static RSAPrivateKey privateKey;
	private static RSAPrivateKey wrongPrivateKey;
	private static String entityWithoutCert;
	private static String admin;
	private static String adminpwd;
	private static ORB orb;
	private static OpenBusContext context;

	@BeforeClass
	public static void oneTimeSetUp() throws Exception {
		Cryptography crypto = Cryptography.getInstance();
		Properties properties = Utils.readPropertyFile("/test.properties");
		host = properties.getProperty("openbus.host.name");
		port = Integer.valueOf(properties.getProperty("openbus.host.port"));
		entity = properties.getProperty("entity.name");
		password = properties.getProperty("entity.password");
		serverEntity = properties.getProperty("server.entity.name");
		privateKeyFile = properties.getProperty("server.private.key");
		privateKey = crypto.readKeyFromFile(privateKeyFile);
		entityWithoutCert = properties.getProperty("entity.withoutcert");
		String wrongPrivateKeyFile = properties.getProperty("wrongkey");
		wrongPrivateKey = crypto.readKeyFromFile(wrongPrivateKeyFile);
		admin = properties.getProperty("admin.name");
		adminpwd = properties.getProperty("admin.password");
		orb = ORBInitializer.initORB();
		context = (OpenBusContext) orb
				.resolve_initial_references("OpenBusContext");
	}

	@After
	public void afterEachTest() {
		context.setCurrentConnection(null);
		context.setDefaultConnection(null);
		context.onCallDispatch(null);
		context.exitChain();
	}

	@Test
	public void orbTest() throws Exception {
		Connection conn = context.connectByAddress(host, port);
		assertNotNull(conn.orb());
		assertEquals(orb, context.orb());
	}

	@Test
	public void offerRegistryNoLoginTest() {
		Connection conn = context.connectByAddress(host, port);
		try {
			OfferRegistry registryService = context.getOfferRegistry();
			ServiceProperty[] props = new ServiceProperty[] { new ServiceProperty(
					"a", "b") };
			registryService.findServices(props);
		} catch (NO_PERMISSION e) {
			assertEquals(e.minor, NoLoginCode.value);
		} catch (ServiceFailure e) {
			fail(e.message);
		}
	}

	@Test
	public void busIdTest() throws Exception {
		Connection conn = context.connectByAddress(host, port);
		assertNull(conn.busid());
		conn.loginByPassword(entity, entity.getBytes());
		assertNotNull(conn.busid());
		assertFalse(conn.busid().isEmpty());
		assertTrue(conn.logout());
		assertNull(conn.busid());
		assertNull(conn.login());
	}

	@Test
	public void loginTest() throws Exception {
		Connection conn = context.connectByAddress(host, port);
		assertNull(conn.login());
		conn.loginByPassword(entity, entity.getBytes());
		assertNotNull(conn.login());
		conn.logout();
		assertNull(conn.login());
	}

	@Test
	public void accessKeyPropTest() throws Exception {
		Properties properties = new Properties();
		properties.put(OpenBusProperty.ACCESS_KEY.getKey(), privateKeyFile);
		Connection conn = context.connectByAddress(host, port, properties);
		assertNull(conn.login());
		conn.loginByPassword(entity, entity.getBytes());
		assertNotNull(conn.login());
		conn.logout();
		assertNull(conn.login());
	}

	@Test(expected = InvalidPropertyValue.class)
	public void invalidAccessKeyPropTest() throws Exception {
		Properties properties = new Properties();
		properties.put(OpenBusProperty.ACCESS_KEY.getKey(),
				"/invalid/path/to/access.key");
		Connection conn = context.connectByAddress(host, port, properties);
	}

	@Test
	public void invalidHostPortLoginTest() throws Exception {
		Connection conn = context.connectByAddress("unknown-host", port);
		assertNull(conn.login());
		try {
			conn.loginByPassword(entity, entity.getBytes());
		} catch (TRANSIENT e) {
			// erro esperado
		} catch (Exception e) {
			fail("A exceção deveria ser TRANSIENT. Exceção recebida: " + e);
		}
		assertNull(conn.login());
		// chutando uma porta inválida
		conn = context.connectByAddress(host, port + 111);
		assertNull(conn.login());
		try {
			conn.loginByPassword(entity, entity.getBytes());
		} catch (TRANSIENT e) {
			// erro esperado
		} catch (Exception e) {
			fail("A exceção deveria ser TRANSIENT. Exceção recebida: " + e);
		}
		assertNull(conn.login());
	}

	@Test
	public void loginByPasswordTest() throws Exception {
		Connection conn = context.connectByAddress(host, port);

		// entidade errada
		boolean failed = false;
		try {
			conn.loginByPassword("", password.getBytes());
		} catch (AccessDenied e) {
			failed = true;
		} catch (Exception e) {
			fail("A exceção deveria ser AccessDenied. Exceção recebida: " + e);
		}
		assertTrue("O login com entidade vazia foi bem-sucedido.", failed);

		// senha errada
		failed = false;
		try {
			conn.loginByPassword("invalid-entity-1", new byte[0]);
		} catch (AccessDenied e) {
			failed = true;
		} catch (Exception e) {
			fail("A exceção deveria ser AccessDenied. Exceção recebida: " + e);
		}
		assertTrue("O login com senha vazia foi bem-sucedido.", failed);

		// login válido
		assertNull(conn.login());
		conn.loginByPassword(entity, password.getBytes());
		assertNotNull(conn.login());
		conn.logout();
		assertNull(conn.login());

		// login repetido
		failed = false;
		try {
			conn.loginByPassword(entity, password.getBytes());
			conn.loginByPassword(entity, password.getBytes());
		} catch (AlreadyLoggedIn e) {
			failed = true;
		} catch (Exception e) {
			fail("A exceção deveria ser AlreadyLoggedInException. Exceção recebida: "
					+ e);
		}
		assertTrue("O login com entidade já autenticada foi bem-sucedido.",
				failed);
		conn.logout();
		assertNull(conn.login());
	}

	@Test
	public void loginByCertificateTest() throws Exception {
		Connection conn = context.connectByAddress(host, port);
		// entidade sem certificado cadastrado
		boolean failed = false;
		try {
			conn.loginByCertificate(entityWithoutCert, privateKey);
		} catch (MissingCertificate e) {
			failed = true;
		} catch (Exception e) {
			fail("A exceção deveria ser MissingCertificate. Exceção recebida: "
					+ e);
		}
		assertTrue(
				"O login de entidade sem certificado cadastrado foi bem-sucedido.",
				failed);

		// chave privada corrompida
		failed = false;
		try {
			RSAPrivateKey key = Cryptography.getInstance().readKeyFromBytes(
					new byte[0]);
			conn.loginByCertificate(serverEntity, key);
		} catch (Exception e) {
			failed = true;
		}
		assertTrue(
				"O login de entidade com chave corrompida foi bem-sucedido.",
				failed);

		// chave privada inválida
		failed = false;
		try {
			conn.loginByCertificate(serverEntity, wrongPrivateKey);
		} catch (AccessDenied e) {
			failed = true;
		} catch (Exception e) {
			fail("A exceção deveria ser WrongPrivateKeyException. Exceção recebida: "
					+ e);
		}
		assertTrue("O login de entidade com chave errada foi bem-sucedido.",
				failed);

		// login válido
		assertNull(conn.login());
		conn.loginByCertificate(serverEntity, privateKey);
		assertNotNull(conn.login());
		assertTrue(conn.logout());
		assertNull(conn.login());

		// login repetido
		failed = false;
		try {
			conn.loginByCertificate(serverEntity, privateKey);
			conn.loginByCertificate(serverEntity, privateKey);
		} catch (AlreadyLoggedIn e) {
			failed = true;
		} catch (Exception e) {
			fail("A exceção deveria ser AlreadyLoggedInException. Exceção recebida: "
					+ e);
		}
		assertTrue("O login com entidade já autenticada foi bem-sucedido.",
				failed);
		assertTrue(conn.logout());
		assertNull(conn.login());
	}

	@Test
	public void loginBySharedAuthenticationFakeTest() throws Exception {
		Connection conn = context.connectByAddress(host, port);
		Connection conn2 = context.connectByAddress(host, port);
		conn.loginByPassword(entity, password.getBytes());

		// segredo errado
		boolean failed = false;
		try {
			context.setCurrentConnection(conn);
			SharedAuthSecretImpl secret = (SharedAuthSecretImpl) conn
					.startSharedAuth();
			context.setCurrentConnection(null);
			conn2.loginBySharedAuth(new SharedAuthSecretImpl(conn.busid(),
					secret.attempt(), new byte[0], (OpenBusContextImpl) context));
		} catch (AccessDenied e) {
			failed = true;
		} catch (Exception e) {
			fail("A exceção deveria ser AccessDenied. Exceção recebida: " + e);
		}
		assertTrue("O login com segredo errado foi bem-sucedido.", failed);
		conn.logout();
		assertNull(conn.login());
		context.setCurrentConnection(null);
	}

	@Test
	public void loginBySharedAuthenticationTest() throws Exception {
		Connection conn = context.createConnection(host, port);
		Connection conn2 = context.createConnection(host, port);
		conn.loginByPassword(entity, password.getBytes());

		// login válido
		assertNull(conn2.login());
		context.setCurrentConnection(conn);
		SharedAuthSecret secret = conn.startSharedAuth();
		context.setCurrentConnection(null);
		conn2.loginBySharedAuth(secret);
		assertNotNull(conn2.login());
		conn2.logout();
		assertNull(conn2.login());
		context.setCurrentConnection(null);

		// login repetido
		boolean failed = false;
		try {
			context.setCurrentConnection(conn);
			secret = conn.startSharedAuth();
			context.setCurrentConnection(null);
			conn2.loginBySharedAuth(secret);
			assertNotNull(conn2.login());
			conn2.loginBySharedAuth(secret);
		} catch (AlreadyLoggedIn e) {
			failed = true;
		} catch (Exception e) {
			fail("A exceção deveria ser AlreadyLoggedInException. Exceção recebida: "
					+ e);
		}
		assertTrue("O login com entidade já autenticada foi bem-sucedido.",
				failed);
		conn2.logout();
		assertNull(conn2.login());
		conn.logout();
		assertNull(conn.login());
		context.setCurrentConnection(null);
	}

	@Test
	public void logoutTest() throws Exception {
		final Connection conn = context.connectByAddress(host, port);
		assertTrue(conn.logout());
		conn.loginByPassword(entity, password.getBytes());
		final String busId = conn.busid();
		context.onCallDispatch(new CallDispatchCallback() {

			@Override
			public Connection dispatch(OpenBusContext context, String busid,
					String loginId, byte[] object_id, String operation) {
				if (busId.equals(busid)) {
					return conn;
				}
				return null;
			}
		});
		assertTrue(conn.logout());
		assertNull(conn.busid());
		assertNull(conn.login());
		boolean failed = false;
		try {
			context.setCurrentConnection(conn);
			context.getOfferRegistry().findServices(new ServiceProperty[0]);
		} catch (NO_PERMISSION e) {
			failed = true;
			if (e.minor != NoLoginCode.value) {
				fail("A exceção é NO_PERMISSION mas o minor code não é NoLoginCode. Minor code recebido: "
						+ e.minor);
			}
		} catch (Exception e) {
			fail("A exceção deveria ser NO_PERMISSION. Exceção recebida: " + e);
		}
		assertTrue("Uma busca sem login foi bem-sucedida.", failed);
	}

	@Test
	public void onInvalidLoginCallbackTest() throws Exception {
		Connection conn = context.connectByAddress(host, port);
		assertNull(conn.onInvalidLoginCallback());
		final AtomicBoolean called = new AtomicBoolean(false);
		InvalidLoginCallback callback = new InvalidLoginCallback() {
			@Override
			public void invalidLogin(Connection conn, LoginInfo login) {
				try {
					conn.loginByPassword(entity, password.getBytes());
					called.set(true);
				} catch (Exception e) {
					// failed
				}
			}
		};
		conn.onInvalidLoginCallback(callback);
		assertEquals(callback, conn.onInvalidLoginCallback());
		conn.loginByPassword(entity, password.getBytes());
		String id = conn.login().id;

		Connection adminconn = context.createConnection(host, port);
		adminconn.loginByPassword(admin, adminpwd.getBytes());
		try {
			context.setCurrentConnection(adminconn);
			context.getLoginRegistry().invalidateLogin(id);
			context.setCurrentConnection(null);
			adminconn.logout();

			context.setCurrentConnection(conn);
			int validity = context.getLoginRegistry().getLoginValidity(id);
			context.setCurrentConnection(null);
			assertTrue(validity <= 0);
			assertTrue(called.get());
		} finally {
			context.setCurrentConnection(null);
		}
	}

	@Test
	public void registerAndFindTest() throws Exception {
		Connection conn1 = context.connectByAddress(host, port);
		Connection conn2 = context.connectByAddress(host, port);
		try {
			String sys1 = "test_entity_registration_first";
			String sys2 = "test_entity_registration_second";
			conn1.loginByCertificate(sys1, privateKey);
			assertNotNull(conn1.login());
			conn2.loginByCertificate(sys2, privateKey);
			assertNotNull(conn2.login());
			ServiceProperty[] props = new ServiceProperty[] { new ServiceProperty(
					"offer.domain", "testing") };
			ComponentContext comp1 = Utils
					.buildTestCallerChainInspectorComponent(context);

			// conn1
			context.setDefaultConnection(conn1);
			context.getOfferRegistry().registerService(comp1.getIComponent(),
					props);
			// conn2
			context.setCurrentConnection(conn2);
			context.getOfferRegistry().registerService(comp1.getIComponent(),
					props);
			context.setCurrentConnection(null);

			props = new ServiceProperty[] {
					new ServiceProperty("offer.domain", "testing"),
					new ServiceProperty("openbus.offer.entity", sys1) };
			ServiceOfferDesc[] services = context.getOfferRegistry()
					.findServices(props);
			assertEquals(1, services.length);
			// invert property list order
			props = new ServiceProperty[] {
					new ServiceProperty("openbus.offer.entity", sys1),
					new ServiceProperty("offer.domain", "testing") };
			services = context.getOfferRegistry().findServices(props);
			assertEquals(1, services.length);
		} finally {
			context.setDefaultConnection(null);
			conn1.logout();
			conn2.logout();
		}
	}

	@Test
	public void logoutOnInvalidLoginCallbackTest() throws Exception {
		Connection conn = context.createConnection(host, port);
		assertNull(conn.onInvalidLoginCallback());
		final AtomicBoolean called = new AtomicBoolean(false);
		InvalidLoginCallback callback = new InvalidLoginCallback() {
			@Override
			public void invalidLogin(Connection conn, LoginInfo login) {
				try {
					conn.loginByPassword(entity, password.getBytes());
					called.set(true);
				} catch (Exception e) {
					// failed
				}
			}
		};
		conn.onInvalidLoginCallback(callback);
		assertEquals(callback, conn.onInvalidLoginCallback());
		conn.loginByPassword(entity, password.getBytes());
		String id = conn.login().id;

		Connection adminconn = context.createConnection(host, port);
		adminconn.loginByPassword(admin, adminpwd.getBytes());
		try {
			context.setCurrentConnection(adminconn);
			boolean ok = context.getLoginRegistry().invalidateLogin(id);
			int validity = context.getLoginRegistry().getLoginValidity(id);
			context.setCurrentConnection(null);
			adminconn.logout();
			assertTrue(ok);
			assertTrue(validity <= 0);

			boolean logout = conn.logout();
			assertTrue(logout);
			assertFalse(called.get());
		} finally {
			context.setCurrentConnection(null);
		}
	}
}
