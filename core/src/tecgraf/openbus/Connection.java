package tecgraf.openbus;

import java.security.interfaces.RSAPrivateKey;

import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;

import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_1.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.access_control.TooManyAttempts;
import tecgraf.openbus.core.v2_1.services.access_control.UnknownBusCode;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.exception.InvalidLoginProcess;

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
 * {@link OpenBusContext} associado ao ORB.
 * 
 * @author Tecgraf
 */
public interface Connection {

  /**
   * Recupera o {@link ORB} correspondente ao {@link OpenBusContext} a partir do
   * qual essa conexão foi criada.
   * 
   * @return o ORB
   */
  org.omg.CORBA.ORB orb();

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
   * @exception AccessDenied Senha fornecida para autenticação da entidade não
   *            foi validada pelo barramento.
   * @exception TooManyAttempts A autenticação foi recusada por um número
   *            excessivo de tentativas inválidas de autenticação por senha.
   * @exception ServiceFailure Ocorreu uma falha interna nos serviços do
   *            barramento que impediu a autenticação da conexão.
   */
  void loginByPassword(String entity, byte[] password) throws AccessDenied,
    AlreadyLoggedIn, TooManyAttempts, ServiceFailure;

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
   * @exception AlreadyLoggedIn A conexão já está autenticada.
   * @exception AccessDenied A chave privada fornecida não corresponde ao
   *            certificado da entidade registrado no barramento indicado.
   * @exception MissingCertificate Não há certificado para essa entidade
   *            registrado no barramento indicado.
   * @exception ServiceFailure Ocorreu uma falha interna nos serviços do
   *            barramento que impediu a autenticação da conexão.
   */
  void loginByCertificate(String entity, RSAPrivateKey privateKey)
    throws AlreadyLoggedIn, AccessDenied, MissingCertificate, ServiceFailure;

  /**
   * Inicia o processo de login por autenticação compartilhada.
   * <p>
   * A autenticação compartilhada permite criar um novo login compartilhando a
   * mesma autenticação do login atual da conexão. Portanto essa operação só
   * pode ser chamada enquanto a conexão estiver autenticada, caso contrário a
   * exceção de sistema {@link NO_PERMISSION}[{@link NoLoginCode}] é lançada. As
   * informações fornecidas por essa operação devem ser passadas para a operação
   * {@link #loginBySharedAuth(SharedAuthSecret) loginBySharedAuth} para
   * conclusão do processo de login por autenticação compartilhada. Isso deve
   * ser feito dentro do tempo de lease definido pelo administrador do
   * barramento. Caso contrário essas informações se tornam inválidas e não
   * podem mais ser utilizadas para criar um login.
   * 
   * @return Segredo a ser fornecido na conclusão do processo de login.
   * 
   * @exception ServiceFailure Ocorreu uma falha interna nos serviços do
   *            barramento que impediu a obtenção do objeto de login e segredo.
   */
  SharedAuthSecret startSharedAuth() throws ServiceFailure;

  /**
   * Efetua login de uma entidade usando autenticação compartilhada.
   * <p>
   * A autenticação compartilhada é feita a partir de um segredo obtido através
   * da operação {@link #startSharedAuth() startSharedAuth} de uma conexão
   * autenticada.
   * 
   * @param secret Segredo a ser fornecido na conclusão do processo de login.
   * 
   * @exception InvalidLoginProcess A tentativa de login associada ao segredo
   *            informado é inválido, por exemplo depois do segredo ser
   *            cancelado, ter expirado, ou já ter sido utilizado.
   * @exception AlreadyLoggedIn A conexão já está autenticada.
   * @exception AccessDenied O segredo fornecido não corresponde ao esperado
   *            pelo barramento.
   * @exception ServiceFailure Ocorreu uma falha interna nos serviços do
   *            barramento que impediu a autenticação da conexão.
   */
  void loginBySharedAuth(SharedAuthSecret secret) throws AlreadyLoggedIn,
    InvalidLoginProcess, AccessDenied, ServiceFailure;

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
   *         <code>false</code> se não for possível invalidar o login atual.
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
   * lança a exceção de de sistema {@link NO_PERMISSION}[ {@link NoLoginCode}].
   * <p>
   * Importante observar que a primeira chamada remota que esta callback
   * realizar <b>deve</b> ser uma tentativa de autenticação junto ao barramento
   * (loginBy*). Caso a callback realize qualquer outra chamada remota, a mesma
   * potencialmenete ocasionará um laço infinito, pois esta outra chamada irá
   * falhar, devido ao login invalidado, e chamará novamente a callback para
   * tentar refazer a autenticação.
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

}