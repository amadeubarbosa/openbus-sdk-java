package tecgraf.openbus;

import com.google.common.collect.ArrayListMultimap;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import scs.core.IComponent;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.UnauthorizedOperation;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_1.services.offer_registry.InvalidProperties;

/**
 * Representa��o de uma oferta registrada no barramento. O objeto JAVA
 * RemoteOffer � apenas uma representa��o local da oferta e n�o deve ser
 * utilizado para identific�-la. Para tal, utilize o identificador da oferta
 * presente em suas propriedades.
 *
 * @author Tecgraf
 */
public interface RemoteOffer {
  /**
   * Fornece a conex�o utilizada para as chamadas remotas dessa oferta.
   * @return A conex�o.
   */
  Connection connection();

  /**
   * Fornece informa��es sobre o login que registrou esta oferta espec�fica.
   * N�o realiza chamadas remotas.
   *
   * @return Informa��es sobre o login que registrou esta oferta.
   */
  LoginInfo owner();

  /**
   * Fornece uma refer�ncia para o servi�o remoto.
   *
   * @return A refer�ncia.
   */
  IComponent service();

  /**
   * Fornece o mapa de propriedades associadas � oferta remota. Realiza uma
   * chamada remota bloqueante ao barramento.
   *
   * Altera��es �s propriedades tamb�m podem ser acompanhadas atrav�s de
   * observadores de ofertas.
   *
   * @return O mapa de propriedades.
   */
  ArrayListMultimap<String, String> properties();

  /**
   * Altera o mapa de propriedades associadas � oferta remota. Realiza uma
   * chamada remota bloqueante ao barramento. Essa chamada s� pode ser feita
   * pelo dono da oferta ou por um usu�rio administrador.
   *
   * @param properties As novas propriedades para a oferta.
   * @throws UnauthorizedOperation Caso n�o haja permiss�o para realizar a
   * altera��o.
   * @throws InvalidProperties Caso as propriedades contenham entradas
   * inv�lidas.
   * @throws ServiceFailure Caso o barramento reporte alguma falha inesperada ao
   * realizar a opera��o.
   */
  void properties(ArrayListMultimap<String, String> properties) throws
    UnauthorizedOperation, InvalidProperties, ServiceFailure;

  /**
   * Remove a oferta do barramento. Esse m�todo s� pode ser usado pelo dono
   * da oferta ou por um usu�rio administrador. Essa opera��o realiza uma
   * chamada remota e � bloqueante.
   *
   * @throws ServiceFailure Caso o barramento reporte alguma falha inesperada ao
   * realizar a opera��o.
   * @throws UnauthorizedOperation Caso o login n�o tenha permiss�o para
   * realizar esta altera��o. Apenas o login que registrou a oferta e logins
   * com poderes administrativos podem remover uma oferta.
   */
  void remove() throws ServiceFailure, UnauthorizedOperation;

  /**
   * Solicita que seja cadastrado na oferta remota um observador interessado em
   * receber eventos dessa oferta. A inscri��o local retornada representa
   * essa solicita��o, que ser� mantida pelo registro de ofertas local at�
   * que haja um logout expl�cito ou que o observador seja removido pela
   * aplica��o.
   *
   * Essa opera��o n�o � bloqueante, o cadastro do observador ser� feito por
   * uma outra thread. O objeto subscri��o retornado permite realizar
   * as opera��es cab�veis sem que seja necess�rio se preocupar com o
   * momento do cadastro real do observador no barramento.
   *
   * Essa opera��o pode bloquear enquanto n�o houver um login ativo. Caso seja
   * interrompida, retornar� {@code null} e se manter� interrompida.
   *
   * @param observer O observador a ser cadastrado.
   * @return A solicita��o de cadastro do observador. Caso a oferta j� n�o
   * exista mais no barramento, ser� retornado {@code null}.
   * @throws ServantNotActive Caso n�o seja poss�vel ativar no POA fornecido
   * pela conex�o um objeto CORBA que � criado para atender �s notifica��es
   * do barramento.
   * @throws WrongPolicy Caso haja alguma inconsist�ncia com as pol�ticas do
   * POA fornecido pela conex�o ao criar um objeto CORBA para atender �s
   * notifica��es do barramento.
   */
  OfferSubscription subscribeObserver(OfferObserver observer) throws
    ServantNotActive, WrongPolicy;
}
