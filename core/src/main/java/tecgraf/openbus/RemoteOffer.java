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
 * Representa��o de uma oferta registrada no barramento.
 *
 * @author Tecgraf
 */
public interface RemoteOffer {
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
  IComponent service_ref();

  /**
   * Fornece o mapa de propriedades associadas � oferta remota. O mapa pode
   * ser atualizado caso o par�metro update seja verdadeiro. Nesse caso, ser�
   * realizada uma chamada remota bloqueante ao barramento. Caso contr�rio, o
   * mapa corresponder� �s propriedades obtidas na �ltima atualiza��o.
   *
   * Altera��es �s propriedades tamb�m podem ser acompanhadas atrav�s de
   * observadores de ofertas.
   *
   * @param update Indica se uma chamada remota ao barramento deve ser
   *               realizada para a atualiza��o do mapa de propriedades.
   * @return O mapa de propriedades.
   */
  ArrayListMultimap<String, String> properties(boolean update);

  /**
   * Altera o mapa local de propriedades associadas � oferta. O mapa da
   * oferta remota no barramento n�o � atualizado, e portanto n�o s�o
   * realizadas chamadas remotas. Este m�todo � �til para quando se recebe uma
   * notifica��o de atualiza��o atrav�s de um observador e se deseja
   * atualizar objetos RemoteOffer diferentes mas referentes � mesma oferta no
   * barramento.
   *
   * @param properties As novas propriedades para a oferta local.
   */
  void propertiesLocal(ArrayListMultimap<String, String> properties);

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
   * @throws ServiceFailure Caso o registro de ofertas reporte alguma falha ao
   * realizar a opera��o.
   */
  void propertiesRemote(ArrayListMultimap<String, String> properties) throws
    UnauthorizedOperation, InvalidProperties, ServiceFailure;

  /**
   * Remove a oferta do barramento. Esse m�todo s� pode ser usado pelo dono
   * da oferta ou por um usu�rio administrador. Essa opera��o realiza uma
   * chamada remota e � bloqueante.
   *
   * @throws ServiceFailure Caso o registro de ofertas reporte alguma falha ao
   * realizar a opera��o.
   * @throws UnauthorizedOperation
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
   * interrompida, retornar� <code>NULL</code> e se manter� interrompida.
   *
   * @param observer o observador a ser cadastrado.
   * @return A solicita��o de cadastro do observador. Caso a oferta j� n�o
   * exista mais no barramento, ser� retornado <code>NULL</code>.
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
