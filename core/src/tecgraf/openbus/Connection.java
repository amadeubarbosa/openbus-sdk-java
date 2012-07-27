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
 * Conexão com um barramento.
 * 
 * @author Tecgraf
 */
public interface Connection {

  /**
   * Recupera o ORB utilizado pela conexão.
   * 
   * @return o ORB
   */
  org.omg.CORBA.ORB orb();

  /**
   * Recupera o serviço de registro de ofertas.
   * 
   * @return o serviço de registro de ofertas.
   */
  OfferRegistry offers();

  /**
   * Recupera a identificação do barramento ao qual essa conexão se refere.
   * 
   * @return a identificação do barramento.
   */
  String busid();

  /**
   * Recupera as informações sobre o login da entidade que autenticou essa
   * conexão.
   * 
   * @return as informações do login.
   */
  LoginInfo login();

  /**
   * Efetua login no barramento como uma entidade usando autenticação por senha.
   * 
   * @param entity Identificador da entidade a ser conectada.
   * @param password Senha de autenticação da entidade no barramento.
   * 
   * @exception AccessDenied Senha fornecida para autenticação da entidade não
   *            foi validada pelo barramento.
   * @exception AlreadyLoggedIn A conexão já está logada.
   * @exception BusChanged O identificador do barramento mudou. Uma nova conexão
   *            deve ser criada.
   * @exception ServiceFailure Ocorreu uma falha interna nos serviços do
   *            barramento que impediu o estabelecimento da conexão.
   */
  void loginByPassword(String entity, byte[] password) throws AccessDenied,
    AlreadyLoggedIn, BusChanged, ServiceFailure;

  /**
   * Efetua login no barramento como uma entidade usando autenticação por
   * certificado.
   * 
   * @param entity Identificador da entidade a ser conectada.
   * @param privKeyBytes Bytes contento a chave privada da entidade utilizada na
   *        autenticação.
   * 
   * @exception MissingCertificate Não há certificado para essa entidade
   *            registrado no barramento indicado.
   * @exception InvalidPrivateKey A chave privada fornecida está corrompida.
   * @exception AlreadyLoggedIn A conexão já está logada.
   * @exception BusChanged O identificador do barramento mudou. Uma nova conexão
   *            deve ser criada.
   * @exception AccessDenied A chave privada fornecida não corresponde ao
   *            certificado da entidade registrado no barramento indicado.
   * @exception ServiceFailure Ocorreu uma falha interna nos serviços do
   *            barramento que impediu o estabelecimento da conexão.
   */
  void loginByCertificate(String entity, byte[] privKeyBytes)
    throws InvalidPrivateKey, AlreadyLoggedIn, BusChanged, AccessDenied,
    MissingCertificate, ServiceFailure;

  /**
   * Inicia o processo de login por single sign-on.
   * 
   * @param secret Segredo a ser fornecido na conclusão do processo de login.
   * 
   * @return Objeto que represeta o processo de login iniciado.
   * @throws ServiceFailure
   */
  LoginProcess startSharedAuth(OctetSeqHolder secret) throws ServiceFailure;

  /**
   * Efetua login no barramento como uma entidade usando autenticação por single
   * sign-on.
   * 
   * @param process Objeto que represeta o processo de login iniciado.
   * @param secret Segredo a ser fornecido na conclusão do processo de login.
   * 
   * @exception AlreadyLoggedIn A conexão já está logada.
   * @exception InvalidLoginProcess O LoginProcess informado é inválido, por
   *            exemplo depois de ser cancelado ou ter expirado.
   * @exception BusChanged O identificador do barramento mudou. Uma nova conexão
   *            deve ser criada.
   * @exception AccessDenied O segredo fornecido não corresponde ao esperado
   *            pelo barramento.
   * @exception ServiceFailure Ocorreu uma falha interna nos serviços do
   *            barramento que impediu o estabelecimento da conexão.
   */
  void loginBySharedAuth(LoginProcess process, byte[] secret)
    throws AlreadyLoggedIn, InvalidLoginProcess, BusChanged, AccessDenied,
    ServiceFailure;

  /**
   * Efetua logout no barramento.
   * 
   * @return <code>true</code> se o processo de logout for concluído com êxito e
   *         <code>false</code> se a conexão já estiver deslogada (login
   *         inválido).
   * @throws ServiceFailure
   */
  boolean logout() throws ServiceFailure;

  /**
   * Define a callback a ser chamada sempre que o login se torna inválido.
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
   * Caso a thread corrente seja a thread de execução de uma chamada remota
   * oriunda do barramento dessa conexão, essa operação devolve um objeto que
   * representa a cadeia de chamadas do barramento que esta chamada faz parte.
   * Caso contrário devolve 'null'.
   * 
   * @return Cadeia da chamada em execução.
   */
  CallerChain getCallerChain();

  /**
   * Associa uma cadeia de chamadas do barramento a thread corrente, de forma
   * que todas as chamadas remotas seguintes dessa thread através dessa conexão
   * sejam feitas como parte dessa cadeia de chamadas.
   * 
   * @param chain a cadeia de chamadas.
   */
  void joinChain(CallerChain chain);

  /**
   * Associa a cadeia de chamadas do barramento retornada pelo getCallerChain a
   * thread corrente, de forma que todas as chamadas remotas seguintes dessa
   * thread através dessa conexão sejam feitas como parte dessa cadeia de
   * chamadas.
   */
  void joinChain();

  /**
   * Remove a associação da cadeia de chamadas com a thread corrente, fazendo
   * com que todas as chamadas seguintes da thread corrente feitas através dessa
   * conexão deixem de fazer parte da cadeia de chamadas associada previamente.
   * Ou seja, todas as chamadas passam a iniciar novas cadeias de chamada.
   */
  void exitChain();

  /**
   * Devolve um objeto que representa a cadeia de chamadas associada a thread
   * atual nessa conexão. A cadeia de chamadas informada foi associada
   * previamente pela operação 'joinChain'. Caso a thread corrente não tenha
   * nenhuma cadeia associada, essa operação devolve 'null'.
   * 
   * @return Cadeia de chamadas associada.
   */
  CallerChain getJoinedChain();

}