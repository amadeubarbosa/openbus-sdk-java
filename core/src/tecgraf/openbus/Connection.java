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
 * Uma conex�o representa uma forma de acesso a um barramento. Basicamente, uma
 * conex�o � usada para representar uma identidade de acesso a um barramento. �
 * poss�vel uma aplica��o assumir m�ltiplas identidades ao acessar um ou mais
 * barramentos criando m�ltiplas conex�es para esses barramentos.
 * <p>
 * Para que as conex�es possam ser efetivamente utilizadas elas precisam estar
 * autenticadas no barramento, que pode ser visto como um identificador de
 * acesso. Cada login possui um identificador �nico e � autenticado em nome de
 * uma entidade, que pode representar um sistema computacional ou mesmo uma
 * pessoa. A fun��o da entidade � atribuir a responsabilidade �s chamadas feitas
 * com aquele login.
 * <p>
 * � importante notar que a conex�o s� define uma forma de acesso, mas n�o �
 * usada diretamente pela aplica��o ao realizar ou receber chamadas, pois as
 * chamadas ocorrem usando proxies e servants de um {@link ORB}. As conex�es que
 * s�o efetivamente usadas nas chamadas do ORB s�o definidas atrav�s do
 * {@link OpenBusContext} associado ao ORB.
 * 
 * @author Tecgraf
 */
public interface Connection {

  /**
   * Recupera o {@link ORB} correspondente ao {@link OpenBusContext} a partir do
   * qual essa conex�o foi criada.
   * 
   * @return o ORB
   */
  org.omg.CORBA.ORB orb();

  /**
   * Recupera o identificador do barramento ao qual essa conex�o se refere.
   * 
   * @return o identificador do barramento.
   */
  String busid();

  /**
   * Recupera as Informa��es do login dessa conex�o ou <code>null</code> se a
   * conex�o n�o est� autenticada, ou seja, n�o tem um login v�lido no
   * barramento.
   * 
   * @return as informa��es do login.
   */
  LoginInfo login();

  /**
   * Efetua login de uma entidade usando autentica��o por senha.
   * <p>
   * A autentica��o por senha � validada usando um dos validadores de senha
   * definidos pelo adminsitrador do barramento.
   * 
   * @param entity Identificador da entidade a ser autenticada.
   * @param password Senha de autentica��o no barramento da entidade.
   * 
   * @exception AlreadyLoggedIn A conex�o j� est� autenticada.
   * @exception AccessDenied Senha fornecida para autentica��o da entidade n�o
   *            foi validada pelo barramento.
   * @exception TooManyAttempts A autentica��o foi recusada por um n�mero
   *            excessivo de tentativas inv�lidas de autentica��o por senha.
   * @exception ServiceFailure Ocorreu uma falha interna nos servi�os do
   *            barramento que impediu a autentica��o da conex�o.
   */
  void loginByPassword(String entity, byte[] password) throws AccessDenied,
    AlreadyLoggedIn, TooManyAttempts, ServiceFailure;

  /**
   * Efetua login de uma entidade usando autentica��o por certificado.
   * <p>
   * A autentica��o por certificado � validada usando um certificado de login
   * registrado pelo adminsitrador do barramento.
   * 
   * @param entity Identificador da entidade a ser autenticada.
   * @param privateKey Chave privada correspondente ao certificado registrado a
   *        ser utilizada na autentica��o.
   * 
   * @exception AlreadyLoggedIn A conex�o j� est� autenticada.
   * @exception AccessDenied A chave privada fornecida n�o corresponde ao
   *            certificado da entidade registrado no barramento indicado.
   * @exception MissingCertificate N�o h� certificado para essa entidade
   *            registrado no barramento indicado.
   * @exception ServiceFailure Ocorreu uma falha interna nos servi�os do
   *            barramento que impediu a autentica��o da conex�o.
   */
  void loginByCertificate(String entity, RSAPrivateKey privateKey)
    throws AlreadyLoggedIn, AccessDenied, MissingCertificate, ServiceFailure;

  /**
   * Inicia o processo de login por autentica��o compartilhada.
   * <p>
   * A autentica��o compartilhada permite criar um novo login compartilhando a
   * mesma autentica��o do login atual da conex�o. Portanto essa opera��o s�
   * pode ser chamada enquanto a conex�o estiver autenticada, caso contr�rio a
   * exce��o de sistema {@link NO_PERMISSION}[{@link NoLoginCode}] � lan�ada. As
   * informa��es fornecidas por essa opera��o devem ser passadas para a opera��o
   * {@link #loginBySharedAuth(SharedAuthSecret) loginBySharedAuth} para
   * conclus�o do processo de login por autentica��o compartilhada. Isso deve
   * ser feito dentro do tempo de lease definido pelo administrador do
   * barramento. Caso contr�rio essas informa��es se tornam inv�lidas e n�o
   * podem mais ser utilizadas para criar um login.
   * 
   * @return Segredo a ser fornecido na conclus�o do processo de login.
   * 
   * @exception ServiceFailure Ocorreu uma falha interna nos servi�os do
   *            barramento que impediu a obten��o do objeto de login e segredo.
   */
  SharedAuthSecret startSharedAuth() throws ServiceFailure;

  /**
   * Efetua login de uma entidade usando autentica��o compartilhada.
   * <p>
   * A autentica��o compartilhada � feita a partir de um segredo obtido atrav�s
   * da opera��o {@link #startSharedAuth() startSharedAuth} de uma conex�o
   * autenticada.
   * 
   * @param secret Segredo a ser fornecido na conclus�o do processo de login.
   * 
   * @exception InvalidLoginProcess A tentativa de login associada ao segredo
   *            informado � inv�lido, por exemplo depois do segredo ser
   *            cancelado, ter expirado, ou j� ter sido utilizado.
   * @exception AlreadyLoggedIn A conex�o j� est� autenticada.
   * @exception AccessDenied O segredo fornecido n�o corresponde ao esperado
   *            pelo barramento.
   * @exception ServiceFailure Ocorreu uma falha interna nos servi�os do
   *            barramento que impediu a autentica��o da conex�o.
   */
  void loginBySharedAuth(SharedAuthSecret secret) throws AlreadyLoggedIn,
    InvalidLoginProcess, AccessDenied, ServiceFailure;

  /**
   * Efetua logout da conex�o, tornando o login atual inv�lido.
   * <p>
   * Ap�s a chamada a essa opera��o a conex�o fica desautenticada, implicando
   * que qualquer chamada realizada pelo ORB usando essa conex�o resultar� numa
   * exce��o de sistema {@link NO_PERMISSION}[{@link NoLoginCode}] e chamadas
   * recebidas por esse ORB ser�o respondidas com a exce��o
   * {@link NO_PERMISSION}[{@link UnknownBusCode}] indicando que n�o foi
   * poss�vel validar a chamada pois a conex�o est� temporariamente
   * desautenticada.
   * 
   * @return <code>true</code> se o processo de logout for conclu�do com �xito e
   *         <code>false</code> se n�o for poss�vel invalidar o login atual.
   * 
   * @exception ServiceFailure Ocorreu uma falha interna nos servi�os do
   *            barramento durante chamada ao remota.
   */
  boolean logout() throws ServiceFailure;

  /**
   * Callback a ser chamada quando o login atual se tornar inv�lido.
   * <p>
   * Esse atributo � utilizado para definir um objeto que implementa uma
   * interface de callback a ser chamada sempre que a conex�o receber uma
   * notifica��o de que o seu login est� inv�lido. Essas notifica��es ocorrem
   * durante chamadas realizadas ou recebidas pelo barramento usando essa
   * conex�o. Um login pode se tornar inv�lido caso o administrador
   * explicitamente o torne inv�lido ou caso a thread interna de renova��o de
   * login n�o seja capaz de renovar o lease do login a tempo. Caso esse
   * atributo seja <code>null</code>, nenhum objeto de callback � chamado na
   * ocorr�ncia desse evento.
   * <p>
   * Durante a execu��o dessa callback um novo login pode ser restabelecido.
   * Neste caso, a chamada do barramento que recebeu a notifica��o de login
   * inv�lido � refeita usando o novo login, caso contr�rio, a chamada original
   * lan�a a exce��o de de sistema {@link NO_PERMISSION}[ {@link NoLoginCode}].
   * <p>
   * Importante observar que a primeira chamada remota que esta callback
   * realizar <b>deve</b> ser uma tentativa de autentica��o junto ao barramento
   * (loginBy*). Caso a callback realize qualquer outra chamada remota, a mesma
   * potencialmenete ocasionar� um la�o infinito, pois esta outra chamada ir�
   * falhar, devido ao login invalidado, e chamar� novamente a callback para
   * tentar refazer a autentica��o.
   * 
   * @param callback Objeto que implementa a interface de callback a ser chamada
   *        ou <code>null</code> caso nenhum objeto deva ser chamado na
   *        ocorr�ncia desse evento.
   */
  void onInvalidLoginCallback(InvalidLoginCallback callback);

  /**
   * Recupera a callback a ser chamada sempre que o login se torna inv�lido.
   * 
   * @return a callback ou <code>null</code> caso ela n�o exista.
   */
  InvalidLoginCallback onInvalidLoginCallback();

}