package tecgraf.openbus.assistant;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TRANSIENT;
import org.omg.CORBA.ORBPackage.InvalidName;

import scs.core.ComponentContext;
import scs.core.IComponent;
import tecgraf.openbus.Connection;
import tecgraf.openbus.InvalidLoginCallback;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.PrivateKey;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.core.v2_0.services.UnauthorizedOperation;
import tecgraf.openbus.core.v2_0.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidLoginCode;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_0.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_0.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_0.services.offer_registry.InvalidProperties;
import tecgraf.openbus.core.v2_0.services.offer_registry.InvalidService;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferRegistry;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOffer;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.core.v2_0.services.offer_registry.UnauthorizedFacets;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.exception.InvalidLoginProcess;
import tecgraf.openbus.exception.InvalidPropertyValue;

/**
 * Assistente que auxilia a integra��o de uma aplica��o a um barramento.
 * <p>
 * O assistente realiza tarefas de manuten��o da integra��o da aplica��o com o
 * barramento. Tais tarefas incluem:
 * <ul>
 * <li>Restabelecimento de login, mesmo em virtude de falhas tempor�rias.
 * <li>Registro de ofertas e observadores de servi�o, mesmo ap�s
 * restabelecimento de login.
 * <li>Busca de ofetas de servi�o dispon�veis no barramento.
 * </ul>
 * <p>
 * O assistente implementa a mesma interface do registro de ofertas, por�m nunca
 * lan�a exce��es, pois todas as opera��es s�o realizadas de forma ass�ncrona.
 * Eventuais falhas nessas opera��es ass�ncronas s�o notificadas atrav�s de
 * callbacks.
 * 
 * @author Tecgraf
 */
public abstract class Assistant {

  /** Inst�ncia do logger */
  private static final Logger logger = Logger.getLogger(Assistant.class
    .getName());

  /** Intervalo de espera entre tentativas */
  private int interval = 5;
  /** Host com o qual o assistente quer se conectar */
  private String host;
  /** Porta com a qual o assistente quer se conectar */
  private int port;
  /** ORB utilizado pelo assistente */
  private ORB orb;
  /** Contexto do ORB utilizado */
  private OpenBusContext context;
  /** Conex�o criada e utilizada pelo assistente */
  private Connection conn;
  /** Callback para informar os erros ocorridos no uso do assistente */
  private OnFailureCallback callback;
  /** Mapa de ofertas a serem mantidas pelo assistente */
  private Map<ComponentContext, Offer> offers = Collections
    .synchronizedMap(new HashMap<ComponentContext, Offer>());
  /** Identifica se o assistente deve finalizar */
  private volatile boolean shutdown = false;
  /** Controlador do pool de threads utilizadas pelo assistente */
  private ExecutorService threadPool = Executors.newCachedThreadPool();

  /**
   * Cria um assistente que efetua login no barramento utilizando autentica��o
   * definida pelo m�todo {@link Assistant#onLoginAuthentication()}.
   * <p>
   * Assistentes criados com essa opera��o realizam o login no barramento sempre
   * utilizando autentica��o definida pelo m�todo
   * {@link Assistant#onLoginAuthentication()} que informa a forma de
   * autentica��o, assim como os dados para realizar essa autentica��o.
   * 
   * @param host Endere�o ou nome de rede onde os servi�os n�cleo do barramento
   *        est�o executando.
   * @param port Porta onde os servi�os n�cleo do barramento est�o executando.
   */
  public Assistant(String host, int port) {
    this(host, port, null);
  }

  /**
   * Cria um assistente que efetua login no barramento utilizando autentica��o
   * definida pelo m�todo {@link Assistant#onLoginAuthentication()}.
   * <p>
   * Assistentes criados com essa opera��o realizam o login no barramento sempre
   * utilizando autentica��o definida pelo m�todo
   * {@link Assistant#onLoginAuthentication()} que informa a forma de
   * autentica��o, assim como os dados para realizar essa autentica��o.
   * 
   * @param host Endere�o ou nome de rede onde os servi�os n�cleo do barramento
   *        est�o executando.
   * @param port Porta onde os servi�os n�cleo do barramento est�o executando.
   * @param params Par�metros opicionais de configura��o do assistente
   */
  public Assistant(String host, int port, AssistantParams params) {
    this.host = host;
    this.port = port;
    if (params == null) {
      params = new AssistantParams();
    }
    orb = params.orb;
    if (orb == null) {
      orb = ORBInitializer.initORB();
    }
    try {
      this.context =
        (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    }
    catch (InvalidName e) {
      throw new IllegalArgumentException(
        "ORB utilizado n�o foi inicializado corretamente.", e);
    }
    try {
      this.conn = this.context.createConnection(host, port, params.connprops);
    }
    catch (InvalidPropertyValue e) {
      throw new IllegalArgumentException(
        "Propriedades definidas para a conex�o s�o inv�lidas", e);
    }
    if (context.getDefaultConnection() != null) {
      throw new IllegalArgumentException("ORB j� est� em uso.");
    }
    context.setDefaultConnection(conn);
    conn.onInvalidLoginCallback(new OnInvalidLogin());
    if (params.interval != null) {
      interval = params.interval;
    }
    if (params.callback != null) {
      this.callback = params.callback;
    }
    else {
      this.callback = new DefaultFailureCallback();
    }
    // realiza o login
    threadPool.execute(new DoLogin(this));
  }

  /**
   * Cria um assistente que efetua login no barramento utilizando autentica��o
   * por senha.
   * <p>
   * Assistentes criados com essa opera��o realizam o login no barramento sempre
   * utilizando autentica��o da entidade indicada pelo par�metro 'entity' e a
   * senha fornecida pelo par�metro 'password'.
   * 
   * @param host Endere�o ou nome de rede onde os servi�os n�cleo do barramento
   *        est�o executando.
   * @param port Porta onde os servi�os n�cleo do barramento est�o executando.
   * @param entity Identificador da entidade a ser autenticada.
   * @param password Senha de autentica��o no barramento da entidade.
   * 
   * @return um novo assistente.
   */
  public static Assistant createWithPassword(String host, int port,
    final String entity, final byte[] password) {
    return createWithPassword(host, port, entity, password, null);
  }

  /**
   * Cria um assistente que efetua login no barramento utilizando autentica��o
   * por senha.
   * <p>
   * Assistentes criados com essa opera��o realizam o login no barramento sempre
   * utilizando autentica��o da entidade indicada pelo par�metro 'entity' e a
   * senha fornecida pelo par�metro 'password'.
   * 
   * @param host Endere�o ou nome de rede onde os servi�os n�cleo do barramento
   *        est�o executando.
   * @param port Porta onde os servi�os n�cleo do barramento est�o executando.
   * @param entity Identificador da entidade a ser autenticada.
   * @param password Senha de autentica��o no barramento da entidade.
   * @param params Par�metros opicionais de configura��o do assistente
   * 
   * @return um novo assistente.
   */
  public static Assistant createWithPassword(String host, int port,
    final String entity, final byte[] password, AssistantParams params) {
    return new Assistant(host, port, params) {

      @Override
      public AuthArgs onLoginAuthentication() {
        return new AuthArgs(entity, password);
      }
    };
  }

  /**
   * Cria um assistente que efetua login no barramento utilizando autentica��o
   * por certificado.
   * <p>
   * Assistentes criados com essa opera��o realizam o login no barramento sempre
   * utilizando autentica��o da entidade indicada pelo par�metro 'entity' e a
   * chave privada fornecida pelo par�metro 'key'.
   * 
   * @param host Endere�o ou nome de rede onde os servi�os n�cleo do barramento
   *        est�o executando.
   * @param port Porta onde os servi�os n�cleo do barramento est�o executando.
   * @param entity Identificador da entidade a ser autenticada.
   * @param key Chave privada correspondente ao certificado registrado a ser
   *        utilizada na autentica��o.
   * 
   * @return um novo assistente.
   */
  public static Assistant createWithPrivateKey(String host, int port,
    final String entity, final PrivateKey key) {
    return createWithPrivateKey(host, port, entity, key, null);
  }

  /**
   * Cria um assistente que efetua login no barramento utilizando autentica��o
   * por certificado.
   * <p>
   * Assistentes criados com essa opera��o realizam o login no barramento sempre
   * utilizando autentica��o da entidade indicada pelo par�metro 'entity' e a
   * chave privada fornecida pelo par�metro 'key'.
   * 
   * @param host Endere�o ou nome de rede onde os servi�os n�cleo do barramento
   *        est�o executando.
   * @param port Porta onde os servi�os n�cleo do barramento est�o executando.
   * @param entity Identificador da entidade a ser autenticada.
   * @param key Chave privada correspondente ao certificado registrado a ser
   *        utilizada na autentica��o.
   * @param params Par�metros opicionais de configura��o do assistente
   * 
   * @return um novo assistente.
   */
  public static Assistant createWithPrivateKey(String host, int port,
    final String entity, final PrivateKey key, AssistantParams params) {
    return new Assistant(host, port, params) {

      @Override
      public AuthArgs onLoginAuthentication() {
        return new AuthArgs(entity, key);
      }
    };
  }

  /**
   * Consulta o ORB em utilizado pelo assistente.
   * 
   * @return ORB utilizado pelo assistante.
   */
  public ORB orb() {
    return this.orb;
  }

  /**
   * Solicita que o assitente registre um servi�o no barramento.
   * <p>
   * Esse m�todo notifica o assistente de que o servi�o fornecido deve se
   * mantido como uma oferta de servi�o v�lida no barramento. Para tanto, sempre
   * que o assistente restabelecer o login esse servi�o ser� novamente
   * registrado no barramento.
   * <p>
   * Para que o registro de servi�os seja bem sucedido � necess�rio que o ORB
   * utilizado pelo assistente esteja processando chamadas, por exemplo, fazendo
   * com que a aplica��o chame o m�todo 'ORB::run()'.
   * 
   * @param component Refer�ncia do servi�o sendo ofertado.
   * @param properties Propriedades do servi�o sendo ofertado.
   */
  public void registerService(ComponentContext component,
    ServiceProperty[] properties) {
    Offer offer = new Offer(this, component, properties);
    this.offers.put(component, offer);
    // dispara o registro da oferta de servi�o
    threadPool.execute(new DoRegister(this, offer));
  }

  /**
   * Busca por ofertas que apresentem um conjunto de propriedades definido.
   * <p>
   * Ser�o selecionadas apenas as ofertas de servi�o que apresentem todas as
   * propriedades especificadas. As propriedades utilizadas nas buscas podem ser
   * aquelas fornecidas no momento do registro da oferta de servi�o, assim como
   * as propriedades automaticamente geradas pelo barramento.
   * 
   * @param properties Propriedades que as ofertas de servi�os encontradas devem
   *        apresentar.
   * @param retries Par�metro opcional indicando o n�mero de novas tentativas de
   *        busca de ofertas em caso de falhas, como o barramento estar
   *        indispon�vel ou n�o for poss�vel estabelecer um login at� o momento.
   *        'retries' com o valor 0 implica que a opera��o retorna imediatamente
   *        ap�s uma �nica tentativa. Para tentar indefinidamente o valor de
   *        'retries' deve ser -1. Entre cada tentativa � feita uma pausa dada
   *        pelo par�metro 'interval' fornecido na cria��o do assistente (veja a
   *        interface 'AssistantFactory').
   * 
   * @return Sequ�ncia de descri��es de ofertas de servi�o encontradas.
   */
  public ServiceOfferDesc[] findServices(ServiceProperty[] properties,
    int retries) {
    int attempt = retries;
    do {
      if (conn.login() != null) {
        ServiceOfferDesc[] offerDescs = find(properties);
        if (offerDescs != null) {
          return offerDescs;
        }
      }
    } while (retryFind(retries, --attempt));
    return new ServiceOfferDesc[0];
  }

  /**
   * Devolve uma lista de todas as ofertas de servi�o registradas.
   * 
   * @param retries Par�metro opcional indicando o n�mero de novas tentativas de
   *        busca de ofertas em caso de falhas, como o barramento estar
   *        indispon�vel ou n�o for poss�vel estabelecer um login at� o momento.
   *        'retries' com o valor 0 implica que a opera��o retorna imediatamente
   *        ap�s uma �nica tentativa. Para tentar indefinidamente o valor de
   *        'retries' deve ser -1. Entre cada tentativa � feita uma pausa dada
   *        pelo par�metro 'interval' fornecido na cria��o do assistente (veja a
   *        interface 'AssistantFactory').
   * 
   * @return Sequ�ncia de descri��es de ofertas de servi�o registradas.
   */
  public ServiceOfferDesc[] getServices(int retries) {
    int attempt = retries;
    do {
      if (conn.login() != null) {
        ServiceOfferDesc[] offerDescs = getAllServices();
        if (offerDescs != null) {
          return offerDescs;
        }
      }
    } while (retryFind(retries, --attempt));
    return new ServiceOfferDesc[0];
  }

  /**
   * Verifica se deve realizar uma nova busca
   * 
   * @param retries n�mero de tentativas configuradas pelo usu�rio
   * @param attempt n�mero de tentativa restantes
   * @return <code>true</code> caso deva realizar uma nova busca, e
   *         <code>false</code> caso contr�rio.
   */
  private boolean retryFind(int retries, int attempt) {
    if (retries < 0 || attempt > 0) {
      try {
        Thread.sleep(interval * 1000);
      }
      catch (InterruptedException e) {
        logger.log(Level.SEVERE, "'Find' foi interrompido.", e);
      }
    }
    else {
      return false;
    }
    return true;
  }

  /**
   * Encerra o funcionamento do assistente liberando todos os recursos alocados
   * por ele.
   * <p>
   * Essa opera��o deve ser chamada antes do assistente ser descartado, pois
   * como o assistente tem um funcionamento ativo, ele continua funcionando e
   * consumindo recursos mesmo que a aplica��o n�o tenha mais refer�ncias a ele.
   * Em particular, alguns dos recursos gerenciados pelo assistente s�o:
   * <ul>
   * <li>Login no barramento;
   * <li>Ofertas de servi�o registradas no barramento;
   * <li>Observadores de servi�o registrados no barramento;
   * <li>Threads de manuten��o desses recursos no barramento;
   * <li>Conex�o default no ORB sendo utilizado;
   * </ul>
   * Em particular, o processamento de requisi��es do ORB (e.g. atrav�s da
   * opera��o 'ORB::run()') n�o � gerido pelo assistente, portanto �
   * responsabilidade da aplica��o iniciar e parar esse processamento (e.g.
   * atrav�s da opera��o 'ORB::shutdown()')
   */
  public void shutdown() {
    this.shutdown = true;
    threadPool.shutdownNow();
    // Aguarda o t�rmino da execu��o das threads
    try {
      long timeout = 3 * interval;
      TimeUnit timeUnit = TimeUnit.SECONDS;
      if (!threadPool.awaitTermination(timeout, timeUnit)) {
        logger.log(Level.WARNING, String.format(
          "pool de threads n�o finalizou. Timeout = %s s", timeout));
      }
    }
    catch (InterruptedException e) {
      logger.log(Level.SEVERE, "pool de threads foi interrompido.", e);
    }
    try {
      conn.logout();
    }
    // bus core
    catch (ServiceFailure e) {
      logger.log(Level.SEVERE, String.format(
        "falha severa no barramento em %s:%s : %s", host, port, e.message), e);
    }
    catch (TRANSIENT e) {
      logger.log(Level.WARNING, String.format(
        "o barramento em %s:%s esta inacess�vel no momento", host, port), e);
    }
    catch (COMM_FAILURE e) {
      logger.log(Level.WARNING,
        "falha de comunica��o ao acessar servi�os n�cleo do barramento", e);
    }
    catch (NO_PERMISSION e) {
      if (e.minor == NoLoginCode.value) {
        logger.log(Level.WARNING, "n�o h� um login v�lido no momento", e);
      }
      else {
        logger.log(Level.SEVERE, String.format(
          "erro de NO_PERMISSION n�o esperado: minor_code = %s", e.minor), e);
      }
    }
    context.setDefaultConnection(null);
  }

  /**
   * M�todo de obten��o de dados para autentica��o de login.
   * <p>
   * M�todo a ser implementado que ser� chamado quando o assistente precisar
   * autenticar uma entidade para restabelecer um login com o barramento.
   * 
   * @return Informa��es para autentica��o para estabelecimento de login.
   */
  public abstract AuthArgs onLoginAuthentication();

  /**
   * M�todo respons�vel por recuperar os argumentos necess�rios e realizar o
   * login junto ao barramento. O m�todo retorna uma indica��o se deveria
   * retentar o login , dado que ocorreu alguma falha durante o processo.
   * 
   * @return <code>true</code> caso o processo de login tenha falhado, e
   *         <code>false</code> caso seja bem sucedido.
   */
  private boolean login() {
    AuthArgs args = onLoginAuthentication();
    boolean failed = true;
    Exception ex = null;
    try {
      if (args != null) {
        switch (args.mode) {
          case AuthByPassword:
            conn.loginByPassword(args.entity, args.password);
          case AuthByCertificate:
            conn.loginByCertificate(args.entity, args.privkey);
          case AuthBySharing:
            conn.loginBySharedAuth(args.attempt, args.secret);
        }
        failed = false;
      }
      else {
        ex =
          new NullPointerException(
            "'onLoginAuthentication' retornou argumentos de login nulos.");
      }
    }
    // CHECK o que fazer em caso de erros inesperados? Capturo Exception?
    catch (AccessDenied e) {
      ex = e;
      logger.log(Level.SEVERE, "Erro ao realizar login.", e);
    }
    catch (AlreadyLoggedIn e) {
      // ignorando o erro
      failed = false;
    }
    catch (MissingCertificate e) {
      ex = e;
      logger.log(Level.SEVERE, "Erro ao realizar loginByCertificate.", e);
    }
    catch (InvalidLoginProcess e) {
      ex = e;
      logger.log(Level.SEVERE, "Erro ao realizar loginBySharedAuth.", e);
    }
    // bus core
    catch (ServiceFailure e) {
      ex = e;
      logger.log(Level.SEVERE, "Erro ao realizar login.", e);
    }
    catch (TRANSIENT e) {
      ex = e;
      logger.log(Level.WARNING, String.format(
        "o barramento em %s:%s esta inacess�vel no momento", host, port), e);
    }
    catch (COMM_FAILURE e) {
      ex = e;
      logger.log(Level.WARNING,
        "falha de comunica��o ao acessar servi�os n�cleo do barramento", e);
    }
    catch (NO_PERMISSION e) {
      ex = e;
      if (e.minor == NoLoginCode.value) {
        logger.log(Level.WARNING, "n�o h� um login v�lido no momento", e);
      }
      else {
        logger.log(Level.SEVERE, String.format(
          "erro de NO_PERMISSION n�o esperado: minor_code = %s", e.minor), e);
      }
    }
    finally {
      if (failed) {
        callback.onLoginFailure(this, ex);
      }
    }
    return failed;
  }

  /**
   * M�todo respons�vel por buscar por servi�os que atendam as propriedades
   * especificadas.
   * 
   * @param props as propriedades de servi�o que se deseja buscar
   * 
   * @return as ofertas de servi�os encontradas, ou <code>null</code> caso algum
   *         erro tenha ocorrido.
   */
  private ServiceOfferDesc[] find(ServiceProperty[] props) {
    boolean failed = true;
    Exception ex = null;
    ServiceOfferDesc[] offerDescs = null;
    try {
      OfferRegistry offerRegistry = context.getOfferRegistry();
      offerDescs = offerRegistry.findServices(props);
      failed = false;
    }
    // bus core
    catch (ServiceFailure e) {
      ex = e;
      logger.log(Level.SEVERE, "Erro ao realizar login.", e);
    }
    catch (TRANSIENT e) {
      ex = e;
      logger.log(Level.WARNING, String.format(
        "o barramento em %s:%s esta inacess�vel no momento", host, port), e);
    }
    catch (COMM_FAILURE e) {
      ex = e;
      logger.log(Level.WARNING,
        "falha de comunica��o ao acessar servi�os n�cleo do barramento", e);
    }
    catch (NO_PERMISSION e) {
      ex = e;
      if (e.minor == NoLoginCode.value) {
        logger.log(Level.WARNING, "n�o h� um login v�lido no momento", e);
      }
      else {
        logger.log(Level.SEVERE, String.format(
          "erro de NO_PERMISSION n�o esperado: minor_code = %s", e.minor), e);
      }
    }
    finally {
      if (failed) {
        callback.onFindFailure(this, ex);
      }
    }
    return offerDescs;
  }

  /**
   * M�todo respons�vel por buscar todos as ofertas de servi�o publicadas no
   * barramento.
   * 
   * @return as ofertas de servi�os encontradas, ou <code>null</code> caso algum
   *         erro tenha ocorrido.
   */
  private ServiceOfferDesc[] getAllServices() {
    boolean failed = true;
    Exception ex = null;
    ServiceOfferDesc[] offerDescs = null;
    try {
      OfferRegistry offerRegistry = context.getOfferRegistry();
      offerDescs = offerRegistry.getServices();
      failed = false;
    }
    // bus core
    catch (ServiceFailure e) {
      ex = e;
      logger.log(Level.SEVERE, "Erro ao realizar login.", e);
    }
    catch (TRANSIENT e) {
      ex = e;
      logger.log(Level.WARNING, String.format(
        "o barramento em %s:%s esta inacess�vel no momento", host, port), e);
    }
    catch (COMM_FAILURE e) {
      ex = e;
      logger.log(Level.WARNING,
        "falha de comunica��o ao acessar servi�os n�cleo do barramento", e);
    }
    catch (NO_PERMISSION e) {
      ex = e;
      if (e.minor == NoLoginCode.value) {
        logger.log(Level.WARNING, "n�o h� um login v�lido no momento", e);
      }
      else {
        logger.log(Level.SEVERE, String.format(
          "erro de NO_PERMISSION n�o esperado: minor_code = %s", e.minor), e);
      }
    }
    finally {
      if (failed) {
        callback.onFindFailure(this, ex);
      }
    }
    return offerDescs;
  }

  /**
   * Classe interna do Assistente que representa uma oferta a se manter
   * registrada no barramento.
   * 
   * @author Tecgraf
   */
  private class Offer {

    /** O assistente */
    Assistant assist;
    /** O Componente a ser registrado */
    ComponentContext component;
    /** Propriedades a serem cadastradas na oferta */
    ServiceProperty[] properties;
    /** Refer�ncia para a descri��o da oferta */
    AtomicReference<ServiceOfferDesc> offer =
      new AtomicReference<ServiceOfferDesc>();
    /** Lock da oferta */
    Object lock = new Object();
    /** Vari�vel condicional que informa que um evento de INVALID LOGIN ocorreu */
    AtomicBoolean event = new AtomicBoolean(true);

    /**
     * Construtor.
     * 
     * @param assist o assistente
     * @param component o componente a ser ofertado
     * @param properties as propriedades com as quais a oferta deve se cadastrar
     */
    public Offer(Assistant assist, ComponentContext component,
      ServiceProperty[] properties) {
      this.assist = assist;
      this.component = component;
      this.properties = Arrays.copyOf(properties, properties.length);
    }

    /**
     * Recupera o login da conex�o que registrou a oferta.
     * 
     * @return o login da conex�o que registrou a oferta, ou <code>null</code>
     *         caso n�o tenha sido registrada nenhuma vez.
     */
    public String loginId() {
      ServiceOfferDesc desc = offer.get();
      if (desc != null) {
        for (ServiceProperty prop : desc.properties) {
          if (prop.name.equals("openbus.offer.login")) {
            return prop.value;
          }
        }
      }
      return null;
    }

    /**
     * M�todo respons�vel por registrar a oferta de servi�o no barramento. O
     * m�todo retorna uma indica��o se deveria retentar o login , dado que
     * ocorreu alguma falha durante o processo
     * 
     * @return <code>true</code> caso o registro tenha falhado, e
     *         <code>false</code> caso seja bem sucedido.
     */
    public boolean registryOffer() {
      boolean failed = true;
      Exception ex = null;
      try {
        OfferRegistry offerRegistry = assist.context.getOfferRegistry();
        ServiceOffer theOffer =
          offerRegistry.registerService(component.getIComponent(), properties);
        offer.set(theOffer.describe());
        failed = false;
      }
      // CHECK o que fazer em caso de erros inesperados? Capturo Exception?
      // register
      catch (UnauthorizedFacets e) {
        StringBuffer interfaces = new StringBuffer();
        for (String facet : e.facets) {
          interfaces.append("\n  - ");
          interfaces.append(facet);
        }
        ex = e;
        logger
          .log(
            Level.WARNING,
            String
              .format(
                "a entidade n�o foi autorizada pelo administrador do barramento a ofertar os servi�os: %s",
                interfaces.toString()), e);
      }
      catch (InvalidService e) {
        ex = e;
        logger.log(Level.WARNING,
          "o servi�o ofertado apresentou alguma falha durante o registro.", e);
      }
      catch (InvalidProperties e) {
        StringBuffer props = new StringBuffer();
        for (ServiceProperty prop : e.properties) {
          props.append("\n  - ");
          props.append(String.format("name = %s, value = %s", prop.name,
            prop.value));
        }
        ex = e;
        logger.log(Level.WARNING, String.format(
          "tentativa de registrar servi�o com propriedades inv�lidas: %s",
          props.toString()), e);
      }
      // bus core
      catch (ServiceFailure e) {
        ex = e;
        logger.log(Level.SEVERE, "Erro ao realizar login.", e);
      }
      catch (TRANSIENT e) {
        ex = e;
        logger.log(Level.WARNING, String.format(
          "o barramento em %s:%s esta inacess�vel no momento", assist.host,
          assist.port), e);
      }
      catch (COMM_FAILURE e) {
        ex = e;
        logger.log(Level.WARNING,
          "falha de comunica��o ao acessar servi�os n�cleo do barramento", e);
      }
      catch (NO_PERMISSION e) {
        ex = e;
        if (e.minor == NoLoginCode.value) {
          logger.log(Level.WARNING, "n�o h� um login v�lido no momento", e);
        }
        else {
          logger.log(Level.SEVERE, String.format(
            "erro de NO_PERMISSION n�o esperado: minor_code = %s", e.minor), e);
        }
      }
      finally {
        if (failed) {
          assist.callback.onRegisterFailure(assist, component.getIComponent(),
            properties, ex);
        }
      }
      return failed;
    }

    /**
     * Marca a oferta como inv�lida para que o registro seja refeito.
     */
    public void reset() {
      synchronized (this.lock) {
        this.event.set(true);
        this.lock.notifyAll();
        logger.fine("Resetando oferta.");
      }
    }

    public void cancel() {
      // CHECK: m�todo parece ser desnecess�rio
      ServiceOfferDesc theOffer = this.offer.getAndSet(null);
      if (theOffer != null) {
        boolean failed = true;
        Exception ex = null;
        while (failed && !assist.shutdown) {
          try {
            theOffer.ref.remove();
            failed = false;
          }
          catch (UnauthorizedOperation e) {
            logger.log(Level.SEVERE,
              "BUG: assistente deveria poder remover a oferta registrada", e);
          }
          // bus core
          catch (ServiceFailure e) {
            ex = e;
            logger.log(Level.SEVERE, "Erro ao remover oferta.", e);
          }
          catch (TRANSIENT e) {
            ex = e;
            logger.log(Level.WARNING, String.format(
              "o barramento em %s:%s esta inacess�vel no momento", assist.host,
              assist.port), e);
          }
          catch (COMM_FAILURE e) {
            ex = e;
            logger.log(Level.WARNING,
              "falha de comunica��o ao acessar servi�os n�cleo do barramento",
              e);
          }
          catch (NO_PERMISSION e) {
            ex = e;
            if (e.minor == NoLoginCode.value) {
              logger.log(Level.WARNING, "n�o h� um login v�lido no momento", e);
            }
            else {
              logger.log(Level.SEVERE,
                String.format(
                  "erro de NO_PERMISSION n�o esperado: minor_code = %s",
                  e.minor), e);
            }
          }
          finally {
            if (failed) {
              try {
                Thread.sleep(assist.interval * 1000);
              }
              catch (InterruptedException e) {
                logger.fine("Thread 'Offer.cancel' foi interrompida.");
              }
            }
          }
        }
      }
    }
  }

  /**
   * Callback do assistente a ser chamado quando ocorrer uma exce��o
   * {@link NO_PERMISSION} com minor igual a {@link InvalidLoginCode}
   * 
   * @author Tecgraf
   */
  private class OnInvalidLogin implements InvalidLoginCallback {

    /**
     * Refaz o login e dispara threads para registrar as ofertas novamente.
     * {@inheritDoc}
     */
    @Override
    public void invalidLogin(Connection conn, LoginInfo login) {
      logger.fine("Iniciando callback 'OnInvalidLogin");
      DoLogin doLogin = new DoLogin(Assistant.this);
      doLogin.run();
      synchronized (Assistant.this.offers) {
        for (Offer aOffer : Assistant.this.offers.values()) {
          aOffer.reset();
        }
      }
      logger.fine("Finalizando callback 'OnInvalidLogin");
    }

  }

  /**
   * Tarefa que executa o processo de login junto ao barramento.
   * 
   * @author Tecgraf
   */
  private class DoLogin implements Runnable {

    /** Assistente em uso */
    Assistant assist;

    /**
     * Construtor
     * 
     * @param assist o assistente em uso.
     */
    public DoLogin(Assistant assist) {
      this.assist = assist;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
      boolean retry = true;
      logger.fine("Iniciando tarefa 'DoLogin'.");
      if (assist.conn.login() != null) {
        // j� possui um login v�lido
        retry = false;
      }
      while (retry && !assist.shutdown) {
        retry = assist.login();
        if (retry) {
          try {
            Thread.sleep(assist.interval * 1000);
          }
          catch (InterruptedException e) {
            logger.fine("Thread 'DoLogin' foi interrompida.");
          }
        }
      }
      logger.fine("Finalizando tarefa 'DoLogin'.");
    }
  }

  /**
   * Tarefa que executa o processo de registro de oferta junto ao barramento.
   * 
   * @author Tecgraf
   */
  private class DoRegister implements Runnable {

    /** Assistente em uso */
    Assistant assist;
    /** Oferta de servi�o a ser registrada */
    private Offer offer;

    /**
     * Construtor
     * 
     * @param assist o assistente em uso.
     * @param offer a oferta a ser registrada.
     */
    public DoRegister(Assistant assist, Offer offer) {
      this.assist = assist;
      this.offer = offer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
      logger.fine("Iniciando tarefa 'DoRegister'.");
      while (!assist.shutdown) {
        boolean retry = true;
        // espera por notifica��o de evento
        synchronized (offer.lock) {
          while (!offer.event.get() && !assist.shutdown) {
            try {
              offer.lock.wait();
            }
            catch (InterruptedException e) {
              logger.fine("Thread 'DoRegister' foi interrompida.");
              break;
            }
          }
          offer.event.set(false);
        }
        // tratando 1 evento
        while (retry && !assist.shutdown) {
          logger.fine("Thread 'DoRegister' tratando evento");
          LoginInfo login = assist.conn.login();
          if (login != null) {
            if (!login.id.equals(offer.loginId())) {
              retry = offer.registryOffer();
            }
            else {
              retry = false;
            }
          }
          if (retry) {
            try {
              Thread.sleep(assist.interval * 1000);
            }
            catch (InterruptedException e) {
              logger.warning("Thread 'DoRegister' foi interrompida.");
              break;
            }
          }
          else {
            logger.fine("Completou atendimento de evento 'DoRegister'.");
          }
        }
      }
      logger.fine("Finalizando tarefa 'DoRegister'.");
    }
  }

  /**
   * Implementa��o padr�o da callback de falhas de execu��o do assistente.
   * 
   * @author Tecgraf
   */
  private class DefaultFailureCallback implements OnFailureCallback {

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoginFailure(Assistant assistant, Exception exception) {
      // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRegisterFailure(Assistant assistant, IComponent component,
      ServiceProperty[] properties, Exception except) {
      // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFindFailure(Assistant assistant, Exception exception) {
      // do nothing
    }

  }

}
