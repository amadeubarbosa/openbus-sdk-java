package tecgraf.openbus.assistant;

import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.core.v2_0.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_0.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_0.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_0.services.offer_registry.InvalidProperties;
import tecgraf.openbus.core.v2_0.services.offer_registry.InvalidService;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferRegistry;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.core.v2_0.services.offer_registry.UnauthorizedFacets;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.exception.InvalidLoginProcess;
import tecgraf.openbus.exception.InvalidPropertyValue;

/**
 * Assistente que auxilia a integração de uma aplicação a um barramento.
 * <p>
 * O assistente realiza tarefas de manutenção da integração da aplicação com o
 * barramento. Tais tarefas incluem:
 * <ul>
 * <li>Restabelecimento de login, mesmo em virtude de falhas temporárias.
 * <li>Registro de ofertas e observadores de serviço, mesmo após
 * restabelecimento de login.
 * <li>Busca de ofetas de serviço disponíveis no barramento.
 * </ul>
 * <p>
 * O assistente implementa a mesma interface do registro de ofertas, porém nunca
 * lança exceções, pois todas as operações são realizadas de forma assíncrona.
 * Eventuais falhas nessas operações assíncronas são notificadas através de
 * callbacks.
 * 
 * @author Tecgraf
 */
public abstract class Assistant {

  /** Instância do logger */
  private static final Logger logger = Logger.getLogger(Assistant.class
    .getName());

  /** Intervalo de espera entre tentativas */
  private int interval = 5;
  private String host;
  private int port;
  /** ORB utilizado pelo assistente */
  private ORB orb;
  /** Contexto do ORB utilizado */
  private OpenBusContext context;
  /** Conexão criada e utilizada pelo assistente */
  private Connection conn;
  /** Callback para informar os erros ocorridos no uso do assistente */
  private OnFailureCallback callback;
  /** Mapa de ofertas a serem mantidas pelo assistente */
  private HashMap<ComponentContext, Offer> offers;

  private ExecutorService threadPool = Executors.newCachedThreadPool();;

  /**
   * Construtor.
   */
  public Assistant(String host, int port) {
    this(host, port, null);
  }

  /**
   * Construtor.
   * 
   * @param params Parâmetros de configuração do assistente.
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
        "ORB utilizado não foi inicializado corretamente.", e);
    }
    try {
      this.conn = this.context.createConnection(host, port, params.connprops);
    }
    catch (InvalidPropertyValue e) {
      throw new IllegalArgumentException(
        "Propriedades definidas para a conexão são inválidas", e);
    }
    if (context.getDefaultConnection() != null) {
      throw new IllegalArgumentException("ORB já está em uso.");
    }
    context.setDefaultConnection(conn);
    // TODO cadastrar onInvalidLoginCallback
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
   * Consulta o ORB em utilizado pelo assistente.
   * 
   * @return ORB utilizado pelo assistante.
   */
  public ORB orb() {
    return this.orb;
  }

  /**
   * Solicita que o assitente registre um serviço no barramento.
   * <p>
   * Esse método notifica o assistente de que o serviço fornecido deve se
   * mantido como uma oferta de serviço válida no barramento. Para tanto, sempre
   * que o assistente restabelecer o login esse serviço será novamente
   * registrado no barramento.
   * <p>
   * Para que o registro de serviços seja bem sucedido é necessário que o ORB
   * utilizado pelo assistente esteja processando chamadas, por exemplo, fazendo
   * com que a aplicação chame o método 'ORB::run()'.
   * 
   * @param component Referência do serviço sendo ofertado.
   * @param properties Propriedades do serviço sendo ofertado.
   */
  public void registerService(ComponentContext component,
    List<ServiceProperty> properties) {
    Offer offer = new Offer(this, component, properties);
    this.offers.put(component, offer);
    // dispara o registro da oferta de serviço
    threadPool.execute(new DoRegister(this, offer));
  }

  /**
   * Busca por ofertas que apresentem um conjunto de propriedades definido.
   * <p>
   * Serão selecionadas apenas as ofertas de serviço que apresentem todas as
   * propriedades especificadas. As propriedades utilizadas nas buscas podem ser
   * aquelas fornecidas no momento do registro da oferta de serviço, assim como
   * as propriedades automaticamente geradas pelo barramento.
   * 
   * @param properties Propriedades que as ofertas de serviços encontradas devem
   *        apresentar.
   * @param retries Parâmetro opcional indicando o número de novas tentativas de
   *        busca de ofertas em caso de falhas, como o barramento estar
   *        indisponível ou não for possível estabelecer um login até o momento.
   *        'retries' com o valor 0 implica que a operação retorna imediatamente
   *        após uma única tentativa. Para tentar indefinidamente o valor de
   *        'retries' deve ser -1. Entre cada tentativa é feita uma pausa dada
   *        pelo parâmetro 'interval' fornecido na criação do assistente (veja a
   *        interface 'AssistantFactory').
   * 
   * @return Sequência de descrições de ofertas de serviço encontradas.
   */
  public ServiceOfferDesc[] findServices(ServiceProperty[] properties,
    int retries) {
    int attempt = retries;
    do {
      ServiceOfferDesc[] offerDescs = find(properties);
      if (offerDescs != null) {
        return offerDescs;
      }
      // CHECK devo me preocupar com o "overflow" do contador? 
      --attempt;
    } while (attempt > 0 || retries < 0);
    // CHECK retorna lista vazia ou nulo?
    return new ServiceOfferDesc[0];
  }

  /**
   * Devolve uma lista de todas as ofertas de serviço registradas.
   * 
   * @param retries Parâmetro opcional indicando o número de novas tentativas de
   *        busca de ofertas em caso de falhas, como o barramento estar
   *        indisponível ou não for possível estabelecer um login até o momento.
   *        'retries' com o valor 0 implica que a operação retorna imediatamente
   *        após uma única tentativa. Para tentar indefinidamente o valor de
   *        'retries' deve ser -1. Entre cada tentativa é feita uma pausa dada
   *        pelo parâmetro 'interval' fornecido na criação do assistente (veja a
   *        interface 'AssistantFactory').
   * 
   * @return Sequência de descrições de ofertas de serviço registradas.
   */
  public ServiceOfferDesc[] getServices(int retries) {
    int attempt = retries;
    do {
      ServiceOfferDesc[] offerDescs = getAllServices();
      if (offerDescs != null) {
        return offerDescs;
      }
      --attempt;
    } while (attempt > 0 || retries < 0);
    // CHECK retorna lista vazia ou nulo?
    return new ServiceOfferDesc[0];
  }

  /**
   * Encerra o funcionamento do assistente liberando todos os recursos alocados
   * por ele.
   * <p>
   * Essa operação deve ser chamada antes do assistente ser descartado, pois
   * como o assistente tem um funcionamento ativo, ele continua funcionando e
   * consumindo recursos mesmo que a aplicação não tenha mais referências a ele.
   * Em particular, alguns dos recursos gerenciados pelo assistente são:
   * <ul>
   * <li>Login no barramento;
   * <li>Ofertas de serviço registradas no barramento;
   * <li>Observadores de serviço registrados no barramento;
   * <li>Threads de manutenção desses recursos no barramento;
   * <li>Conexão default no ORB sendo utilizado;
   * </ul>
   * Em particular, o processamento de requisições do ORB (e.g. através da
   * operação 'ORB::run()') não é gerido pelo assistente, portanto é
   * responsabilidade da aplicação iniciar e parar esse processamento (e.g.
   * através da operação 'ORB::shutdown()')
   */
  public void shutdown() {
    threadPool.shutdownNow();
    // Aguarda o término da execução das threads
    try {
      long timeout = 3 * interval;
      TimeUnit timeUnit = TimeUnit.SECONDS;
      if (!threadPool.awaitTermination(timeout, timeUnit)) {
        logger.log(Level.SEVERE, String.format(
          "pool de threads não finalizou. Timeout = %s s", timeout));
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
        "falha severa no barramento em %s:%s : %s", host, port, e.message));
    }
    catch (TRANSIENT e) {
      logger.log(Level.SEVERE, String.format(
        "o barramento em %s:%s esta inacessível no momento", host, port));
    }
    catch (COMM_FAILURE e) {
      logger.log(Level.SEVERE,
        "falha de comunicação ao acessar serviços núcleo do barramento");
    }
    catch (NO_PERMISSION e) {
      if (e.minor == NoLoginCode.value) {
        logger.log(Level.SEVERE, "não há um login válido no momento");
      }
      else {
        logger.log(Level.SEVERE, String.format(
          "erro de NO_PERMISSION não esperado: minor_code = %s", e.minor));
      }
    }
    context.setDefaultConnection(null);
  }

  /**
   * Método de obtenção de dados para autenticação de login.
   * <p>
   * Método a ser implementado que será chamado quando o assistente precisar
   * autenticar uma entidade para restabelecer um login com o barramento.
   * 
   * @return Informações para autenticação para estabelecimento de login.
   */
  public abstract AuthArgs onLoginAuthentication();

  /**
   * Método responsável por recuperar os argumentos necessários e realizar o
   * login junto ao barramento. O método retorna uma indicação se ocorreu alguma
   * falha durante o login.
   * 
   * @return <code>true</code> caso o processo de login tenha falhado, e
   *         <code>false</code> caso seja bem sucedido.
   */
  private boolean login() {
    AuthArgs args = onLoginAuthentication();
    boolean failed = true;
    Exception ex = null;
    try {
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
    // CHECK o que fazer em caso de erros inesperados? Capturo Exception?
    catch (AccessDenied e) {
      ex = e;
      logger.log(Level.SEVERE, "Erro ao realizar login.", e);
    }
    catch (AlreadyLoggedIn e) {
      // ignorando o erro
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
      logger.log(Level.SEVERE, String.format(
        "o barramento em %s:%s esta inacessível no momento", host, port));
    }
    catch (COMM_FAILURE e) {
      ex = e;
      logger.log(Level.SEVERE,
        "falha de comunicação ao acessar serviços núcleo do barramento");
    }
    catch (NO_PERMISSION e) {
      ex = e;
      if (e.minor == NoLoginCode.value) {
        logger.log(Level.SEVERE, "não há um login válido no momento");
      }
      else {
        logger.log(Level.SEVERE, String.format(
          "erro de NO_PERMISSION não esperado: minor_code = %s", e.minor));
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
   * Método responsável por buscar por serviços que atendam as propriedades
   * especificadas.
   * 
   * @param props as propriedades de serviço que se deseja buscar
   * 
   * @return as ofertas de serviços encontradas, ou <code>null</code> caso algum
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
      logger.log(Level.SEVERE, String.format(
        "o barramento em %s:%s esta inacessível no momento", host, port));
    }
    catch (COMM_FAILURE e) {
      ex = e;
      logger.log(Level.SEVERE,
        "falha de comunicação ao acessar serviços núcleo do barramento");
    }
    catch (NO_PERMISSION e) {
      ex = e;
      if (e.minor == NoLoginCode.value) {
        logger.log(Level.SEVERE, "não há um login válido no momento");
      }
      else {
        logger.log(Level.SEVERE, String.format(
          "erro de NO_PERMISSION não esperado: minor_code = %s", e.minor));
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
   * Método responsável por buscar todos as ofertas de serviço publicadas no
   * barramento.
   * 
   * @return as ofertas de serviços encontradas, ou <code>null</code> caso algum
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
      logger.log(Level.SEVERE, String.format(
        "o barramento em %s:%s esta inacessível no momento", host, port));
    }
    catch (COMM_FAILURE e) {
      ex = e;
      logger.log(Level.SEVERE,
        "falha de comunicação ao acessar serviços núcleo do barramento");
    }
    catch (NO_PERMISSION e) {
      ex = e;
      if (e.minor == NoLoginCode.value) {
        logger.log(Level.SEVERE, "não há um login válido no momento");
      }
      else {
        logger.log(Level.SEVERE, String.format(
          "erro de NO_PERMISSION não esperado: minor_code = %s", e.minor));
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
   * 
   * Representa um conjunto de parâmetros opcionais que podem ser utilizados
   * para definir parâmetros de configuração na construção do Assistente.
   * <p>
   * Os parâmetros opicionais são descritos abaixo:
   * <ul>
   * <li>interval: Tempo em segundos indicando o tempo mínimo de espera antes de
   * cada nova tentativa após uma falha na execução de uma tarefa. Por exemplo,
   * depois de uma falha na tentativa de um login ou registro de oferta, o
   * assistente espera pelo menos o tempo indicado por esse parâmetro antes de
   * tentar uma nova tentativa.
   * <li>orb: O ORB a ser utilizado pelo assistente para realizar suas tarefas.
   * O assistente também configura esse ORB de forma que todas as chamadas
   * feitas por ele sejam feitas com a identidade do login estabelecido pelo
   * assistente. Esse ORB deve ser iniciado de acordo com os requisitos do
   * projeto OpenBus, como feito pela operação 'ORBInitializer::initORB()'.
   * <li>connprops: Propriedades da conexão a ser criada com o barramento
   * espeficiado. Para maiores informações sobre essas propriedades, veja a
   * operação 'OpenBusContext::createConnection()'.
   * <li>callback: Objeto de callback que recebe notificações de falhas das
   * tarefas realizadas pelo assistente.
   * </ul>
   * 
   * @author Tecgraf
   */
  static public class AssistantParams {
    /**
     * Tempo em segundos indicando o tempo mínimo de espera antes de cada nova
     * tentativa após uma falha na execução de uma tarefa. Por exemplo, depois
     * de uma falha na tentativa de um login ou registro de oferta, o assistente
     * espera pelo menos o tempo indicado por esse parâmetro antes de tentar uma
     * nova tentativa.
     */
    public Integer interval;
    /**
     * O ORB a ser utilizado pelo assistente para realizar suas tarefas. O
     * assistente também configura esse ORB de forma que todas as chamadas
     * feitas por ele sejam feitas com a identidade do login estabelecido pelo
     * assistente. Esse ORB deve ser iniciado de acordo com os requisitos do
     * projeto OpenBus, como feito pela operação 'ORBInitializer::initORB()'.
     */
    public ORB orb;
    /**
     * Propriedades da conexão a ser criada com o barramento espeficiado. Para
     * maiores informações sobre essas propriedades, veja a operação
     * 'OpenBusContext::createConnection()'.
     */
    public Properties connprops;
    /**
     * Objeto de callback que recebe notificações de falhas das tarefas
     * realizadas pelo assistente.
     */
    public OnFailureCallback callback;
  }

  private static class Offer {

    Assistant assist;
    ComponentContext component;
    ServiceProperty[] properties;

    public Offer(Assistant assist, ComponentContext component,
      List<ServiceProperty> properties) {
      this.assist = assist;
      this.component = component;
      this.properties =
        properties.toArray(new ServiceProperty[properties.size()]);
    }

    public boolean registryOffer() {
      boolean failed = true;
      Exception ex = null;
      try {
        OfferRegistry offerRegistry = assist.context.getOfferRegistry();
        offerRegistry.registerService(component.getIComponent(), properties);
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
            Level.SEVERE,
            String
              .format(
                "a entidade não foi autorizada pelo administrador do barramento a ofertar os serviços: %s",
                interfaces.toString()));
      }
      catch (InvalidService e) {
        ex = e;
        logger.log(Level.SEVERE,
          "o serviço ofertado apresentou alguma falha durante o registro.");
      }
      catch (InvalidProperties e) {
        StringBuffer props = new StringBuffer();
        for (ServiceProperty prop : e.properties) {
          props.append("\n  - ");
          props.append(String.format("name = %s, value = %s", prop.name,
            prop.value));
        }
        ex = e;
        logger.log(Level.SEVERE, String.format(
          "tentativa de registrar serviço com propriedades inválidas: %s",
          props.toString()));
      }
      // bus core
      catch (ServiceFailure e) {
        ex = e;
        logger.log(Level.SEVERE, "Erro ao realizar login.", e);
      }
      catch (TRANSIENT e) {
        ex = e;
        logger.log(Level.SEVERE, String.format(
          "o barramento em %s:%s esta inacessível no momento", assist.host,
          assist.port));
      }
      catch (COMM_FAILURE e) {
        ex = e;
        logger.log(Level.SEVERE,
          "falha de comunicação ao acessar serviços núcleo do barramento");
      }
      catch (NO_PERMISSION e) {
        ex = e;
        if (e.minor == NoLoginCode.value) {
          logger.log(Level.SEVERE, "não há um login válido no momento");
        }
        else {
          logger.log(Level.SEVERE, String.format(
            "erro de NO_PERMISSION não esperado: minor_code = %s", e.minor));
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
      if (assist.conn.login() != null) {
        // já possui um login válido
        retry = false;
      }
      while (retry) {
        retry = assist.login();
        if (retry) {
          try {
            Thread.sleep(assist.interval * 1000);
          }
          catch (InterruptedException e) {
            // do nothing
          }
        }
      }
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
    /** Oferta de serviço a ser registrada */
    private Offer offer;

    /**
     * Construtor
     * 
     * @param assist o assistente em uso.
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
      boolean retry = true;
      while (retry) {
        retry = offer.registryOffer();
        retry = assist.login();
        if (retry) {
          try {
            Thread.sleep(assist.interval * 1000);
          }
          catch (InterruptedException e) {
            // do nothing
          }
        }
      }
    }
  }

  /**
   * Implementação padrão da callback de falhas de execução do assistente.
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

  /**
   * @param args
   */
  public static void main(String[] args) {
    Assistant assist = new Assistant("localhost", 2089) {
      @Override
      public AuthArgs onLoginAuthentication() {
        return new AuthArgs("entity", "entity".getBytes());
      }
    };
  }

}
