package tecgraf.openbus;

import java.util.List;

import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import tecgraf.openbus.core.v2_1.OctetSeqHolder;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.UnauthorizedOperation;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidLogins;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;

/**
 * Interface local de registro de logins. Mantém apenas um objeto CORBA como
 * observador independente do número de observadores da aplicação.
 *
 * @author Tecgraf
 */
public interface LoginRegistry {
  /**
   * Fornece a conexão utilizada para as chamadas remotas desse registro.
   * @return A conexão.
   */
  Connection conn();

  /**
   * Obtém uma lista de todos os logins ativos no barramento. Essa operação
   * realiza uma chamada remota e é bloqueante. Não é possível executar essa
   * chamada com sucesso sem direitos de administração sobre o barramento.
   *
   * Essa operação pode bloquear enquanto não houver um login ativo. Caso seja
   * interrompida, retornará uma lista vazia e se manterá interrompida.
   *
   * @return Lista de informações de todos os logins ativos no barramento.
   * @throws ServiceFailure Caso o registro de logins reporte alguma falha ao
   * realizar a operação.
   * @throws UnauthorizedOperation Caso a entidade que realizou a chamada não
   * tenha permissões de administrador.
   */
  List<LoginInfo> allLogins() throws ServiceFailure, UnauthorizedOperation;

  /**
   * Obtém uma lista de todos os logins ativos de uma entidade. Essa operação
   * realiza uma chamada remota e é bloqueante.
   *
   * Essa operação pode bloquear enquanto não houver um login ativo. Caso seja
   * interrompida, retornará uma lista vazia e se manterá interrompida.
   *
   * @param entity Identificador de uma entidade.
   * @return Lista de informações de todos os logins ativos da entidade.
   *
   * @throws ServiceFailure Caso o registro de logins reporte alguma falha ao
   * realizar a operação.
   * @throws UnauthorizedOperation Caso a entidade que realizou a chamada não
   * seja a própria entidade e não tenha permissões de administrador.
   */
  List<LoginInfo> entityLogins(String entity) throws ServiceFailure,
    UnauthorizedOperation;

  /**
   * Invalida um login no barramento. Essa operação
   * realiza uma chamada remota e é bloqueante.
   *
   * Essa operação pode bloquear enquanto não houver um login ativo. Caso seja
   * interrompida, retornará <code>false</code> e se manterá interrompida.
   *
   * @param loginId Identificador do login a ser invalidado.
   * @return <code>true</code> se o login informado está válido e foi
   *         invalidado, ou <code>false</code> se o login informado não é
   *         válido.
   * @throws ServiceFailure Caso o registro de logins reporte alguma falha ao
   * realizar a operação.
   * @throws UnauthorizedOperation Caso a entidade que realizou a chamada não
   * seja a própria entidade e não tenha permissões de administrador.
   */
  boolean invalidateLogin(String loginId) throws ServiceFailure,
    UnauthorizedOperation;

  /**
   * Obtém informações de um login válido. Essa operação
   * realiza uma chamada remota e é bloqueante.
   *
   * Essa operação pode bloquear enquanto não houver um login ativo. Caso seja
   * interrompida, retornará um LoginInfo não preenchido e se manterá
   * interrompida.
   *
   * @param loginId Identificador do login a ser consultado.
   * @param pubkey Valor de retorno com a chave pública da entidade.
   * @return Informações sobre o login requisitado.
   * @throws InvalidLogins Caso que o login requisitado seja inválido.
   * @throws ServiceFailure Caso o registro de logins reporte alguma falha ao
   * realizar a operação.
   */
  LoginInfo loginInfo(String loginId, OctetSeqHolder pubkey)
    throws InvalidLogins, ServiceFailure;

  /**
   * Verifica a validade de um login. Essa operação
   * realiza uma chamada remota e é bloqueante.
   *
   * Essa operação pode bloquear enquanto não houver um login ativo. Caso seja
   * interrompida, retornará -1 e se manterá interrompida.
   *
   * @param loginId Identificador do login a ser consultado.
   * @return Valor indicando o tempo máximo (em segundos) pelo qual o login
   *         permanecerá válido sem necessidade de renovação. Caso a validade já
   *         tenha expirado, o valor indicado é zero, indicando que o login não
   *         é mais válido.
   * @throws ServiceFailure Caso o registro de logins reporte alguma falha ao
   * realizar a operação.
   */
  int loginValidity(String loginId) throws ServiceFailure;

  /**
   * Solicita que seja cadastrado um observador interessado em receber
   * eventos de logins. A inscrição local retornada representa essa
   * solicitação, que será mantida pelo registro de logins local até que haja
   * um logout explícito ou que o observador seja removido pela aplicação.
   *
   * Essa operação não é bloqueante, o cadastro do observador se necessário
   * será feito por uma outra thread. O objeto subscrição retornado permite
   * realizar as operações cabíveis sem que seja necessário se preocupar com o
   * momento do cadastro real do observador no barramento.
   *
   * Essa operação pode bloquear enquanto não houver um login ativo. Caso seja
   * interrompida, retornará <code>NULL</code> e se manterá interrompida.
   *
   * @param observer Objeto a ser utilizado para a nofificação de eventos.
   * @return Objeto para gerência da inscrição do observador.
   * @throws ServantNotActive Caso não seja possível ativar no POA fornecido
   * pela conexão um objeto CORBA que é criado para atender às notificações
   * do barramento.
   * @throws WrongPolicy Caso haja alguma inconsistência com as políticas do
   * POA fornecido pela conexão ao criar um objeto CORBA para atender às
   * notificações do barramento.
   */
  LoginSubscription subscribeObserver(LoginObserver observer) throws
    ServantNotActive, WrongPolicy;
}