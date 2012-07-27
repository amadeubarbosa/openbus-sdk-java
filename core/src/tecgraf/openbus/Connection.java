package tecgraf.openbus;

import tecgraf.openbus.core.v2_0.OctetSeqHolder;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.core.v2_0.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_0.services.access_control.LoginProcess;
import tecgraf.openbus.core.v2_0.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_0.services.offer_registry.OfferRegistry;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.exception.BusChanged;
import tecgraf.openbus.exception.InvalidLoginProcess;
import tecgraf.openbus.exception.InvalidPrivateKey;

/**
 * Conex�o com um barramento.
 * 
 * @author Tecgraf
 */
public interface Connection {

  /**
   * Recupera o ORB utilizado pela conex�o.
   * 
   * @return o ORB
   */
  org.omg.CORBA.ORB orb();

  /**
   * Recupera o servi�o de registro de ofertas.
   * 
   * @return o servi�o de registro de ofertas.
   */
  OfferRegistry offers();

  /**
   * Recupera a identifica��o do barramento ao qual essa conex�o se refere.
   * 
   * @return a identifica��o do barramento.
   */
  String busid();

  /**
   * Recupera as informa��es sobre o login da entidade que autenticou essa
   * conex�o.
   * 
   * @return as informa��es do login.
   */
  LoginInfo login();

  /**
   * Efetua login no barramento como uma entidade usando autentica��o por senha.
   * 
   * @param entity Identificador da entidade a ser conectada.
   * @param password Senha de autentica��o da entidade no barramento.
   * 
   * @exception AccessDenied Senha fornecida para autentica��o da entidade n�o
   *            foi validada pelo barramento.
   * @exception AlreadyLoggedIn A conex�o j� est� logada.
   * @exception BusChanged O identificador do barramento mudou. Uma nova conex�o
   *            deve ser criada.
   * @exception ServiceFailure Ocorreu uma falha interna nos servi�os do
   *            barramento que impediu o estabelecimento da conex�o.
   */
  void loginByPassword(String entity, byte[] password) throws AccessDenied,
    AlreadyLoggedIn, BusChanged, ServiceFailure;

  /**
   * Efetua login no barramento como uma entidade usando autentica��o por
   * certificado.
   * 
   * @param entity Identificador da entidade a ser conectada.
   * @param privKeyBytes Bytes contento a chave privada da entidade utilizada na
   *        autentica��o.
   * 
   * @exception MissingCertificate N�o h� certificado para essa entidade
   *            registrado no barramento indicado.
   * @exception InvalidPrivateKey A chave privada fornecida est� corrompida.
   * @exception AlreadyLoggedIn A conex�o j� est� logada.
   * @exception BusChanged O identificador do barramento mudou. Uma nova conex�o
   *            deve ser criada.
   * @exception AccessDenied A chave privada fornecida n�o corresponde ao
   *            certificado da entidade registrado no barramento indicado.
   * @exception ServiceFailure Ocorreu uma falha interna nos servi�os do
   *            barramento que impediu o estabelecimento da conex�o.
   */
  void loginByCertificate(String entity, byte[] privKeyBytes)
    throws InvalidPrivateKey, AlreadyLoggedIn, BusChanged, AccessDenied,
    MissingCertificate, ServiceFailure;

  /**
   * Inicia o processo de login por single sign-on.
   * 
   * @param secret Segredo a ser fornecido na conclus�o do processo de login.
   * 
   * @return Objeto que represeta o processo de login iniciado.
   * @throws ServiceFailure
   */
  LoginProcess startSharedAuth(OctetSeqHolder secret) throws ServiceFailure;

  /**
   * Efetua login no barramento como uma entidade usando autentica��o por single
   * sign-on.
   * 
   * @param process Objeto que represeta o processo de login iniciado.
   * @param secret Segredo a ser fornecido na conclus�o do processo de login.
   * 
   * @exception AlreadyLoggedIn A conex�o j� est� logada.
   * @exception InvalidLoginProcess O LoginProcess informado � inv�lido, por
   *            exemplo depois de ser cancelado ou ter expirado.
   * @exception BusChanged O identificador do barramento mudou. Uma nova conex�o
   *            deve ser criada.
   * @exception AccessDenied O segredo fornecido n�o corresponde ao esperado
   *            pelo barramento.
   * @exception ServiceFailure Ocorreu uma falha interna nos servi�os do
   *            barramento que impediu o estabelecimento da conex�o.
   */
  void loginBySharedAuth(LoginProcess process, byte[] secret)
    throws AlreadyLoggedIn, InvalidLoginProcess, BusChanged, AccessDenied,
    ServiceFailure;

  /**
   * Efetua logout no barramento.
   * 
   * @return <code>true</code> se o processo de logout for conclu�do com �xito e
   *         <code>false</code> se a conex�o j� estiver deslogada (login
   *         inv�lido).
   * @throws ServiceFailure
   */
  boolean logout() throws ServiceFailure;

  /**
   * Define a callback a ser chamada sempre que o login se torna inv�lido.
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

  /**
   * Caso a thread corrente seja a thread de execu��o de uma chamada remota
   * oriunda do barramento dessa conex�o, essa opera��o devolve um objeto que
   * representa a cadeia de chamadas do barramento que esta chamada faz parte.
   * Caso contr�rio devolve 'null'.
   * 
   * @return Cadeia da chamada em execu��o.
   */
  CallerChain getCallerChain();

  /**
   * Associa uma cadeia de chamadas do barramento a thread corrente, de forma
   * que todas as chamadas remotas seguintes dessa thread atrav�s dessa conex�o
   * sejam feitas como parte dessa cadeia de chamadas.
   * 
   * @param chain a cadeia de chamadas.
   */
  void joinChain(CallerChain chain);

  /**
   * Associa a cadeia de chamadas do barramento retornada pelo getCallerChain a
   * thread corrente, de forma que todas as chamadas remotas seguintes dessa
   * thread atrav�s dessa conex�o sejam feitas como parte dessa cadeia de
   * chamadas.
   */
  void joinChain();

  /**
   * Remove a associa��o da cadeia de chamadas com a thread corrente, fazendo
   * com que todas as chamadas seguintes da thread corrente feitas atrav�s dessa
   * conex�o deixem de fazer parte da cadeia de chamadas associada previamente.
   * Ou seja, todas as chamadas passam a iniciar novas cadeias de chamada.
   */
  void exitChain();

  /**
   * Devolve um objeto que representa a cadeia de chamadas associada a thread
   * atual nessa conex�o. A cadeia de chamadas informada foi associada
   * previamente pela opera��o 'joinChain'. Caso a thread corrente n�o tenha
   * nenhuma cadeia associada, essa opera��o devolve 'null'.
   * 
   * @return Cadeia de chamadas associada.
   */
  CallerChain getJoinedChain();

}