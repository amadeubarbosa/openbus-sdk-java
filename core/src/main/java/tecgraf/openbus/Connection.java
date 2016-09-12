package tecgraf.openbus;

import java.security.interfaces.RSAPrivateKey;

import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;

import org.omg.PortableServer.POA;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidToken;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_1.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.access_control.TooManyAttempts;
import tecgraf.openbus.core.v2_1.services.access_control.UnknownBusCode;
import tecgraf.openbus.core.v2_1.services.access_control.UnknownDomain;
import tecgraf.openbus.core.v2_1.services.access_control.WrongEncoding;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.exception.InvalidLoginProcess;
import tecgraf.openbus.exception.WrongBus;

/**
 * Objeto que representa uma forma de acesso a um barramento.
 * <p>
 * Basicamente, uma conexão é usada para representar uma identidade de acesso
 * a um barramento. É possível uma aplicação assumir múltiplas identidades ao
 * acessar um ou mais barramentos criando múltiplas conexões para esses
 * barramentos.
 * <p>
 * Para que as conexões possam ser efetivamente utilizadas elas precisam estar
 * autenticadas no barramento, que pode ser visto como um identificador de
 * acesso. Cada <i>login</i> possui um identificador único e é autenticado em
 * nome de uma entidade, que pode representar um sistema computacional ou
 * mesmo uma pessoa. A função da entidade é atribuir a responsabilidade às
 * chamadas feitas com aquele <i>login</i>.
 * <p>
 * É importante notar que a conexão só define uma forma de acesso, mas não é
 * usada diretamente pela aplicação ao realizar ou receber chamadas, pois as
 * chamadas ocorrem usando <i>proxies</i> e <i>servants</i> de um {@link ORB}
 * . As conexões que são efetivamente usadas nas chamadas do ORB são definidas
 * através do {@link OpenBusContext} associado ao ORB.
 *
 * Para o caso de recursos mantidos por uma conexão através de seus
 * {@link LoginRegistry} e {@link OfferRegistry}, não é necessário definir a
 * conexão através do {@link OpenBusContext}, pois esses recursos já estarão
 * atrelados à conexão a qual pertencem.
 * 
 * @author Tecgraf
 */
public interface Connection {

  /**
   * Fornece o {@link ORB} correspondente ao {@link OpenBusContext} a partir do
   * qual essa conexão foi criada.
   * 
   * @return o ORB
   */
  org.omg.CORBA.ORB ORB();

  /**
   * Fornece o POA associado a essa conexão.
   *
   * @return o POA
   */
  POA POA();

  /**
   * Fornece o contexto do OpenBus associado a essa conexão.
   *
   * @return o contexto do OpenBus
   */
  OpenBusContext context();

  /**
   * Fornece o identificador do barramento ao qual essa conexão se refere.
   * 
   * @return o identificador do barramento.
   */
  String busId();

  /**
   * Fornece as Informações do login dessa conexão ou {@code null} se a
   * conexão não estiver autenticada, ou seja, não tiver um login válido no
   * barramento.
   * 
   * @return As informações do login.
   */
  LoginInfo login();

  /**
   * Efetua login de uma entidade usando autenticação por senha.
   * <p>
   * A autenticação por senha é validada através de um dos validadores de senha
   * definidos pelo administrador do barramento.
   * 
   * @param entity Identificador da entidade a ser autenticada.
   * @param password Senha de autenticação no barramento da entidade.
   * @param domain Identificador do domínio de autenticação.
   * 
   * @throws AlreadyLoggedIn A conexão já está autenticada.
   * @throws AccessDenied A senha fornecida para autenticação da entidade não
   *            foi validada pelo barramento.
   * @throws TooManyAttempts A autenticação foi recusada por um número
   *            excessivo de tentativas inválidas de autenticação por senha.
   * @throws UnknownDomain O domínio de autenticação não é conhecido.
   * @throws ServiceFailure Ocorreu uma falha interna nos serviços do
   *            barramento que impediu a autenticação da conexão.
   * @throws WrongEncoding A autenticação falhou, pois a senha não foi
   *            codificada corretamente com a chave pública do barramento.
   */
  void loginByPassword(String entity, byte[] password, String domain)
    throws AccessDenied, AlreadyLoggedIn, TooManyAttempts, UnknownDomain,
    ServiceFailure, WrongEncoding;

  /**
   * Efetua login de uma entidade usando autenticação por chave privada.
   * <p>
   * A autenticação por chave privada é validada através de um certificado
   * registrado pelo administrador do barramento.
   * 
   * @param entity Identificador da entidade a ser autenticada.
   * @param privateKey Chave privada correspondente ao certificado registrado
   *                   no barramento.
   * 
   * @throws AlreadyLoggedIn A conexão já está autenticada.
   * @throws AccessDenied A chave privada fornecida não corresponde ao
   *            certificado da entidade registrado no barramento.
   * @throws MissingCertificate Não há certificado para essa entidade
   *            registrado no barramento.
   * @throws ServiceFailure Ocorreu uma falha interna nos serviços do
   *            barramento que impediu a autenticação da conexão.
   * @throws WrongEncoding A autenticação falhou, pois a resposta ao desafio
   *            não foi codificada corretamente com a chave pública do
   *            barramento.
   */
  void loginByPrivateKey(String entity, RSAPrivateKey privateKey)
    throws AlreadyLoggedIn, AccessDenied, MissingCertificate, ServiceFailure, WrongEncoding;

  /**
   * Efetua login de uma entidade utilizando uma <i>callback</i> que retorne
   * dados de autenticação, como autenticações compartilhadas.
   * <p>
   * No caso específico da autenticação compartilhada, esta deve ser feita a
   * partir de um segredo obtido através da operação
   * {@link #startSharedAuth() startSharedAuth} de uma conexão autenticada.
   *
   * @param callback Callback a ser utilizada para obter os dados de
   *                 autenticação a serem utilizados no login.
   *
   * @throws InvalidLoginProcess A tentativa de login associada ao segredo
   *         informado é inválido, por exemplo depois do segredo ser
   *         cancelado, ter expirado, ou já ter sido utilizado.
   * @throws AlreadyLoggedIn A conexão já está autenticada.
   * @throws WrongBus As informações de autenticação não pertencem ao
   *         barramento contactado.
   * @throws AccessDenied As informações de autenticação não correspondem
   *         ao esperado pelo barramento.
   * @throws ServiceFailure Ocorreu uma falha interna nos serviços do
   *         barramento que impediu a autenticação da conexão.
   * @throws MissingCertificate O barramento não tem um certificado público
   *         para a entidade fornecida. Como as informações de login são
   *         dadas pela callback, deve-se verificar se a callback está
   *         obtendo-as corretamente.
   * @throws TooManyAttempts O barramento bloqueou tentativas de login desta
   *         conexão temporariamente, devido a um excesso de tentativas num
   *         curto período de tempo.
   * @throws UnknownDomain O barramento não reconhece o domínio especificado
   *         para este login. Como as informações de login são dadas pela
   *         callback, deve-se verificar se a callback está obtendo-as
   *         corretamente.
   * @throws WrongEncoding A autenticação falhou, pois a resposta ao desafio
   *         não foi codificada corretamente com a chave pública do barramento.
   */
  void loginByCallback(LoginCallback callback) throws AlreadyLoggedIn, WrongBus,
    InvalidLoginProcess, AccessDenied, TooManyAttempts, UnknownDomain,
    MissingCertificate, ServiceFailure, WrongEncoding;

  /**
   * Inicia o processo de login por autenticação compartilhada.
   *
   * A autenticação compartilhada permite criar um novo login compartilhando a
   * mesma autenticação do login atual da conexão. Portanto, essa operação só
   * pode ser chamada enquanto a conexão estiver autenticada, caso contrário a
   * exceção de sistema {@link NO_PERMISSION}[{@link NoLoginCode}] será lançada
   * . As informações fornecidas por essa operação devem ser passadas para a
   * operação {@link #loginByCallback(LoginCallback)} para conclusão do
   * processo de login por autenticação compartilhada. Isso deve ser feito
   * dentro do tempo de lease definido pelo administrador do barramento. Caso
   * contrário essas informações se tornarão inválidas e não poderão mais ser
   * utilizadas para criar um login.
   * 
   * @return Segredo a ser fornecido na conclusão do processo de login.
   * 
   * @throws ServiceFailure Ocorreu uma falha interna nos serviços do
   *            barramento que impediu a obtenção do objeto de login e segredo.
   */
  SharedAuthSecret startSharedAuth() throws ServiceFailure;

  /**
   * Efetua logout da conexão, tornando o login atual inválido.
   * <p>
   * Após a chamada a essa operação a conexão fica desautenticada, implicando
   * numa exceção de sistema {@link NO_PERMISSION}[{@link NoLoginCode}] para
   * qualquer chamada realizada pelo ORB usando essa conexão. Chamadas
   * recebidas por esse ORB serão respondidas com a exceção
   * {@link NO_PERMISSION}[{@link UnknownBusCode}] indicando que não foi
   * possível validar a chamada devido à conexão estar desautenticada.
   *
   * Um logout explícito incorre na remoção de todas as ofertas e todos os tipos
   * de observadores mantidos pela biblioteca de acesso OpenBus, tanto
   * localmente como no barramento. Um login posterior não levará ao
   * recadastro automático desses recursos.
   *
   * @return {@code True} se o processo de logout for concluído com êxito e
   *         {@code false} se não for possível invalidar o login atual. Mesmo
   *         que seja retornado {@code false}, os recursos que eram mantidos
   *         por este login não serão mais renovados e, após o tempo de
   *         <i>lease</i> do barramento, serão removidos automaticamente por
   *         falta de renovação.
   * 
   * @throws ServiceFailure Ocorreu uma falha interna nos serviços do
   *            barramento durante chamada ao remota.
   */
  boolean logout() throws ServiceFailure;

  /**
   * Fornece o registro de logins.
   *
   * @return o registro de logins.
   */
  LoginRegistry loginRegistry();

  /**
   * Fornece o registro de ofertas.
   *
   * @return o registro de ofertas.
   */
  OfferRegistry offerRegistry();

  /**
   * Cria uma nova cadeia de chamadas para a entidade especificada, onde o dono
   * da cadeia é esta conexão e utiliza-se a cadeia atual
   * ({@link OpenBusContext#joinedChain()}) como a cadeia que se deseja dar
   * seguimento ao encadeamento. É permitido especificar qualquer nome de
   * entidade, tendo ela um login ativo no momento ou não. A cadeia
   * resultante só poderá ser utilizada com sucesso por uma conexão que
   * possua a mesma identidade da entidade especificada.
   *
   * @param entity Nome da entidade para a qual deseja-se enviar a cadeia.
   * @return A cadeia gerada para ser utilizada pela entidade com o login
   *         especificado.
   *
   * @throws ServiceFailure Ocorreu uma falha interna nos serviços do barramento
   *         que impediu a criação da cadeia.
   */
  CallerChain makeChainFor(String entity) throws ServiceFailure;

  /**
   * Cria uma cadeia de chamadas assinada pelo barramento com informações de uma
   * autenticação externa ao barramento.
   * <p>
   * A cadeia criada somente pode ser utilizada pela entidade do login que faz a
   * chamada. O conteúdo da cadeia é dado pelas informações obtidas através do
   * <i>token</i> indicado.
   *
   * @param token Valor opaco que representa uma informação de autenticação
   *        externa.
   * @param domain Identificador do domínio de autenticação.
   * @return A nova cadeia de chamadas assinada.
   *
   * @throws InvalidToken O token fornecido não foi reconhecido.
   * @throws UnknownDomain O domínio de autenticação não é conhecido.
   * @throws WrongEncoding A importação falhou, pois o token não foi
   *            codificado corretamente com a chave pública do barramento.
   * @throws ServiceFailure Ocorreu uma falha interna nos serviços do
   *            barramento que impediu a criação da cadeia.
   */
  CallerChain importChain(byte[] token, String domain) throws InvalidToken,
    UnknownDomain, ServiceFailure, WrongEncoding;
}