package tecgraf.openbus;

import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.PortableInterceptor.Current;

import tecgraf.openbus.core.v2_0.OctetSeqHolder;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.core.v2_0.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_0.services.access_control.LoginProcess;
import tecgraf.openbus.core.v2_0.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_0.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_0.services.access_control.UnknownBusCode;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferRegistry;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.exception.BusChanged;
import tecgraf.openbus.exception.InvalidLoginProcess;
import tecgraf.openbus.exception.InvalidPrivateKey;

/**
 * Objeto que representa uma forma de acesso a um barramento.
 * <p>
 * Uma conexão representa uma forma de acesso a um barramento. Basicamente, uma
 * conexão é usada para representar uma identidade de acesso a um barramento. É
 * possível uma aplicação assumir múltiplas identidades ao acessar um ou mais
 * barramentos criando múltiplas conexões para esses barramentos.
 * <p>
 * Para que as conexões possam ser efetivamente utilizadas elas precisam estar
 * autenticadas no barramento, que pode ser visto como um identificador de
 * acesso. Cada login possui um identificador único e é autenticado em nome de
 * uma entidade, que pode representar um sistema computacional ou mesmo uma
 * pessoa. A função da entidade é atribuir a responsabilidade às chamadas feitas
 * com aquele login.
 * <p>
 * É importante notar que a conexão só define uma forma de acesso, mas não é
 * usada diretamente pela aplicação ao realizar ou receber chamadas, pois as
 * chamadas ocorrem usando proxies e servants de um {@link ORB}. As conexões que
 * são efetivamente usadas nas chamadas do ORB são definidas através do
 * {@link ConnectionManager} associado ao ORB.
 * 
 * @author Tecgraf
 */
public interface Connection {

  /**
   * Recupera o {@link ORB} correspondente ao {@link ConnectionManager} a partir
   * do qual essa conexão foi criada.
   * 
   * @return o ORB
   */
  org.omg.CORBA.ORB orb();

  /**
   * Recupera a referência do serviço núcleo de registro de ofertas do
   * barramento ao qual a conexão se refere.
   * 
   * @return o serviço de registro de ofertas.
   */
  OfferRegistry offers();

  /**
   * Recupera o identificador do barramento ao qual essa conexão se refere.
   * 
   * @return o identificador do barramento.
   */
  String busid();

  /**
   * Recupera as Informações do login dessa conexão ou <code>null</code> se a
   * conexão não está autenticada, ou seja, não tem um login válido no
   * barramento.
   * 
   * @return as informações do login.
   */
  LoginInfo login();

  /**
   * Efetua login de uma entidade usando autenticação por senha.
   * <p>
   * A autenticação por senha é validada usando um dos validadores de senha
   * definidos pelo adminsitrador do barramento.
   * 
   * @param entity Identificador da entidade a ser autenticada.
   * @param password Senha de autenticação no barramento da entidade.
   * 
   * @exception AlreadyLoggedIn A conexão já está autenticada.
   * @exception BusChanged O identificador do barramento mudou. Uma nova conexão
   *            deve ser criada.
   * @exception AccessDenied Senha fornecida para autenticação da entidade não
   *            foi validada pelo barramento.
   * @exception ServiceFailure Ocorreu uma falha interna nos serviços do
   *            barramento que impediu a autenticação da conexão.
   */
  void loginByPassword(String entity, byte[] password) throws AccessDenied,
    AlreadyLoggedIn, BusChanged, ServiceFailure;

  /**
   * Efetua login de uma entidade usando autenticação por certificado.
   * <p>
   * A autenticação por certificado é validada usando um certificado de login
   * registrado pelo adminsitrador do barramento.
   * 
   * @param entity Identificador da entidade a ser autenticada.
   * @param privateKey Chave privada correspondente ao certificado registrado a
   *        ser utilizada na autenticação.
   * 
   * @exception InvalidPrivateKey A chave privada fornecida não é válida.
   * @exception AlreadyLoggedIn A conexão já está autenticada.
   * @exception BusChanged O identificador do barramento mudou. Uma nova conexão
   *            deve ser criada.
   * @exception AccessDenied A chave privada fornecida não corresponde ao
   *            certificado da entidade registrado no barramento indicado.
   * @exception MissingCertificate Não há certificado para essa entidade
   *            registrado no barramento indicado.
   * @exception ServiceFailure Ocorreu uma falha interna nos serviços do
   *            barramento que impediu a autenticação da conexão.
   */
  void loginByCertificate(String entity, byte[] privateKey)
    throws InvalidPrivateKey, AlreadyLoggedIn, BusChanged, AccessDenied,
    MissingCertificate, ServiceFailure;

  /**
   * Inicia o processo de login por autenticação compartilhada.
   * <p>
   * A autenticação compartilhada permite criar um novo login compartilhando a
   * mesma autenticação do login atual da conexão. Portanto essa operação só
   * pode ser chamada enquanto a conexão estiver autenticada, caso contrário a
   * exceção de sistema {@link NO_PERMISSION}[{@link NoLoginCode}] é lançada. As
   * informações fornecidas por essa operação devem ser passadas para a operação
   * {@link #loginBySharedAuth(LoginProcess, byte[]) loginBySharedAuth} para
   * conclusão do processo de login por autenticação compartilhada. Isso deve
   * ser feito dentro do tempo de lease definido pelo administrador do
   * barramento. Caso contrário essas informações se tornam inválidas e não
   * podem mais ser utilizadas para criar um login.
   * 
   * @param secret Segredo a ser fornecido na conclusão do processo de login.
   * 
   * @return Objeto que represeta o processo de login iniciado.
   * 
   * @exception ServiceFailure Ocorreu uma falha interna nos serviços do
   *            barramento que impediu a obtenção do objeto de login e segredo.
   */
  LoginProcess startSharedAuth(OctetSeqHolder secret) throws ServiceFailure;

  /**
   * Efetua login de uma entidade usando autenticação compartilhada.
   * <p>
   * A autenticação compartilhada é feita a partir de informações obtidas a
   * através da operação {@link #startSharedAuth(OctetSeqHolder)
   * startSharedAuth} de uma conexão autenticada.
   * 
   * @param process Objeto que represeta o processo de login iniciado.
   * @param secret Segredo a ser fornecido na conclusão do processo de login.
   * 
   * @exception InvalidLoginProcess O LoginProcess informado é inválido, por
   *            exemplo depois de ser cancelado ou ter expirado.
   * @exception AlreadyLoggedIn A conexão já está autenticada.
   * @exception BusChanged O identificador do barramento mudou. Uma nova conexão
   *            deve ser criada.
   * @exception AccessDenied O segredo fornecido não corresponde ao esperado
   *            pelo barramento.
   * @exception ServiceFailure Ocorreu uma falha interna nos serviços do
   *            barramento que impediu a autenticação da conexão.
   */
  void loginBySharedAuth(LoginProcess process, byte[] secret)
    throws AlreadyLoggedIn, InvalidLoginProcess, BusChanged, AccessDenied,
    ServiceFailure;

  /**
   * Efetua logout da conexão, tornando o login atual inválido.
   * <p>
   * Após a chamada a essa operação a conexão fica desautenticada, implicando
   * que qualquer chamada realizada pelo ORB usando essa conexão resultará numa
   * exceção de sistema {@link NO_PERMISSION}[{@link NoLoginCode}] e chamadas
   * recebidas por esse ORB serão respondidas com a exceção
   * {@link NO_PERMISSION}[{@link UnknownBusCode}] indicando que não foi
   * possível validar a chamada pois a conexão está temporariamente
   * desautenticada.
   * 
   * @return <code>true</code> se o processo de logout for concluído com êxito e
   *         <code>false</code> se a conexão já estiver desautenticada (login
   *         inválido).
   * 
   * @exception ServiceFailure Ocorreu uma falha interna nos serviços do
   *            barramento durante chamada ao remota.
   */
  boolean logout() throws ServiceFailure;

  /**
   * Callback a ser chamada quando o login atual se tornar inválido.
   * <p>
   * Esse atributo é utilizado para definir um objeto que implementa uma
   * interface de callback a ser chamada sempre que a conexão receber uma
   * notificação de que o seu login está inválido. Essas notificações ocorrem
   * durante chamadas realizadas ou recebidas pelo barramento usando essa
   * conexão. Um login pode se tornar inválido caso o administrador
   * explicitamente o torne inválido ou caso a thread interna de renovação de
   * login não seja capaz de renovar o lease do login a tempo. Caso esse
   * atributo seja <code>null</code>, nenhum objeto de callback é chamado na
   * ocorrência desse evento.
   * <p>
   * Durante a execução dessa callback um novo login pode ser restabelecido.
   * Neste caso, a chamada do barramento que recebeu a notificação de login
   * inválido é refeita usando o novo login, caso contrário, a chamada original
   * lança a exceção de de sistema {@link NO_PERMISSION}[{@link NoLoginCode}].
   * 
   * @param callback Objeto que implementa a interface de callback a ser chamada
   *        ou <code>null</code> caso nenhum objeto deva ser chamado na
   *        ocorrência desse evento.
   */
  void onInvalidLoginCallback(InvalidLoginCallback callback);

  /**
   * Recupera a callback a ser chamada sempre que o login se torna inválido.
   * 
   * @return a callback ou <code>null</code> caso ela não exista.
   */
  InvalidLoginCallback onInvalidLoginCallback();

  /**
   * Devolve a cadeia de chamadas à qual a execução corrente pertence.
   * <p>
   * Caso a contexto corrente (e.g. definido pelo {@link Current}) seja o
   * contexto de execução de uma chamada remota oriunda do barramento dessa
   * conexão, essa operação devolve um objeto que representa a cadeia de
   * chamadas do barramento que esta chamada faz parte. Caso contrário, devolve
   * <code>null</code>.
   * 
   * @return Cadeia da chamada em execução.
   */
  CallerChain getCallerChain();

  /**
   * Associa uma cadeia de chamadas ao contexto corrente.
   * <p>
   * Associa uma cadeia de chamadas ao contexto corrente (e.g. definido pelo
   * {@link Current}), de forma que todas as chamadas remotas seguintes neste
   * mesmo contexto sejam feitas como parte dessa cadeia de chamadas.
   * 
   * @param chain Cadeia de chamadas a ser associada ao contexto corrente.
   */
  void joinChain(CallerChain chain);

  /**
   * Associa a cadeia de chamadas obtida em {@link #getCallerChain()} ao
   * contexto corrente.
   * <p>
   * Associa a cadeia de chamadas obtida em {@link #getCallerChain()} ao
   * contexto corrente (e.g. definido pelo {@link Current}), de forma que todas
   * as chamadas remotas seguintes neste mesmo contexto sejam feitas como parte
   * dessa cadeia de chamadas.
   */
  void joinChain();

  /**
   * Faz com que nenhuma cadeia de chamadas esteja associada ao contexto
   * corrente.
   * <p>
   * Remove a associação da cadeia de chamadas ao contexto corrente (e.g.
   * definido pelo {@link Current}), fazendo com que todas as chamadas seguintes
   * feitas neste mesmo contexto deixem de fazer parte da cadeia de chamadas
   * associada previamente. Ou seja, todas as chamadas passam a iniciar novas
   * cadeias de chamada.
   */
  void exitChain();

  /**
   * Devolve a cadeia de chamadas associada ao contexto corrente.
   * <p>
   * Devolve um objeto que representa a cadeia de chamadas associada ao contexto
   * corrente (e.g. definido pelo {@link Current}) nesta conexão. A cadeia de
   * chamadas informada foi associada previamente pela operação
   * {@link #joinChain(CallerChain) joinChain}. Caso o contexto corrente não
   * tenha nenhuma cadeia associada, essa operação devolve <code>null</code>.
   * 
   * @return Cadeia de chamadas associada ao contexto corrente ou
   *         <code>null</code> .
   */
  CallerChain getJoinedChain();

}