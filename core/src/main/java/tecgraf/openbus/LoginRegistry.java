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
 * Interface local de registro de logins. Mant�m apenas um objeto CORBA como
 * observador independente do n�mero de observadores da aplica��o.
 *
 * @author Tecgraf
 */
public interface LoginRegistry {
  /**
   * Fornece a conex�o utilizada para as chamadas remotas desse registro.
   * @return A conex�o.
   */
  Connection conn();

  /**
   * Obt�m uma lista de todos os logins ativos no barramento. Essa opera��o
   * realiza uma chamada remota e � bloqueante. N�o � poss�vel executar essa
   * chamada com sucesso sem direitos de administra��o sobre o barramento.
   *
   * Essa opera��o pode bloquear enquanto n�o houver um login ativo. Caso seja
   * interrompida, retornar� uma lista vazia e se manter� interrompida.
   *
   * @return Lista de informa��es de todos os logins ativos no barramento.
   * @throws ServiceFailure Caso o registro de logins reporte alguma falha ao
   * realizar a opera��o.
   * @throws UnauthorizedOperation Caso a entidade que realizou a chamada n�o
   * tenha permiss�es de administrador.
   */
  List<LoginInfo> allLogins() throws ServiceFailure, UnauthorizedOperation;

  /**
   * Obt�m uma lista de todos os logins ativos de uma entidade. Essa opera��o
   * realiza uma chamada remota e � bloqueante.
   *
   * Essa opera��o pode bloquear enquanto n�o houver um login ativo. Caso seja
   * interrompida, retornar� uma lista vazia e se manter� interrompida.
   *
   * @param entity Identificador de uma entidade.
   * @return Lista de informa��es de todos os logins ativos da entidade.
   *
   * @throws ServiceFailure Caso o registro de logins reporte alguma falha ao
   * realizar a opera��o.
   * @throws UnauthorizedOperation Caso a entidade que realizou a chamada n�o
   * seja a pr�pria entidade e n�o tenha permiss�es de administrador.
   */
  List<LoginInfo> entityLogins(String entity) throws ServiceFailure,
    UnauthorizedOperation;

  /**
   * Invalida um login no barramento. Essa opera��o
   * realiza uma chamada remota e � bloqueante.
   *
   * Essa opera��o pode bloquear enquanto n�o houver um login ativo. Caso seja
   * interrompida, retornar� <code>false</code> e se manter� interrompida.
   *
   * @param loginId Identificador do login a ser invalidado.
   * @return <code>true</code> se o login informado est� v�lido e foi
   *         invalidado, ou <code>false</code> se o login informado n�o �
   *         v�lido.
   * @throws ServiceFailure Caso o registro de logins reporte alguma falha ao
   * realizar a opera��o.
   * @throws UnauthorizedOperation Caso a entidade que realizou a chamada n�o
   * seja a pr�pria entidade e n�o tenha permiss�es de administrador.
   */
  boolean invalidateLogin(String loginId) throws ServiceFailure,
    UnauthorizedOperation;

  /**
   * Obt�m informa��es de um login v�lido. Essa opera��o
   * realiza uma chamada remota e � bloqueante.
   *
   * Essa opera��o pode bloquear enquanto n�o houver um login ativo. Caso seja
   * interrompida, retornar� um LoginInfo n�o preenchido e se manter�
   * interrompida.
   *
   * @param loginId Identificador do login a ser consultado.
   * @param pubkey Valor de retorno com a chave p�blica da entidade.
   * @return Informa��es sobre o login requisitado.
   * @throws InvalidLogins Caso que o login requisitado seja inv�lido.
   * @throws ServiceFailure Caso o registro de logins reporte alguma falha ao
   * realizar a opera��o.
   */
  LoginInfo loginInfo(String loginId, OctetSeqHolder pubkey)
    throws InvalidLogins, ServiceFailure;

  /**
   * Verifica a validade de um login. Essa opera��o
   * realiza uma chamada remota e � bloqueante.
   *
   * Essa opera��o pode bloquear enquanto n�o houver um login ativo. Caso seja
   * interrompida, retornar� -1 e se manter� interrompida.
   *
   * @param loginId Identificador do login a ser consultado.
   * @return Valor indicando o tempo m�ximo (em segundos) pelo qual o login
   *         permanecer� v�lido sem necessidade de renova��o. Caso a validade j�
   *         tenha expirado, o valor indicado � zero, indicando que o login n�o
   *         � mais v�lido.
   * @throws ServiceFailure Caso o registro de logins reporte alguma falha ao
   * realizar a opera��o.
   */
  int loginValidity(String loginId) throws ServiceFailure;

  /**
   * Solicita que seja cadastrado um observador interessado em receber
   * eventos de logins. A inscri��o local retornada representa essa
   * solicita��o, que ser� mantida pelo registro de logins local at� que haja
   * um logout expl�cito ou que o observador seja removido pela aplica��o.
   *
   * Essa opera��o n�o � bloqueante, o cadastro do observador se necess�rio
   * ser� feito por uma outra thread. O objeto subscri��o retornado permite
   * realizar as opera��es cab�veis sem que seja necess�rio se preocupar com o
   * momento do cadastro real do observador no barramento.
   *
   * Essa opera��o pode bloquear enquanto n�o houver um login ativo. Caso seja
   * interrompida, retornar� <code>NULL</code> e se manter� interrompida.
   *
   * @param observer Objeto a ser utilizado para a nofifica��o de eventos.
   * @return Objeto para ger�ncia da inscri��o do observador.
   * @throws ServantNotActive Caso n�o seja poss�vel ativar no POA fornecido
   * pela conex�o um objeto CORBA que � criado para atender �s notifica��es
   * do barramento.
   * @throws WrongPolicy Caso haja alguma inconsist�ncia com as pol�ticas do
   * POA fornecido pela conex�o ao criar um objeto CORBA para atender �s
   * notifica��es do barramento.
   */
  LoginSubscription subscribeObserver(LoginObserver observer) throws
    ServantNotActive, WrongPolicy;
}