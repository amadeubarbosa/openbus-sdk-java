package tecgraf.openbus;

import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import scs.core.IComponent;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;

/**
 * Interface local do registro de ofertas.
 *
 * @author Tecgraf
 */
public interface OfferRegistry {
  /**
   * Fornece a conex�o utilizada para as chamadas remotas desse registro.
   * @return A conex�o.
   */
  Connection conn();

  /**
   * Solicita que seja registrado um servi�o, representado por um componente
   * ({@link IComponent}), no barramento. A oferta local retornada representa
   * essa solicita��o, que ser� mantida pelo registro de ofertas local at�
   * que haja um logout expl�cito ou que a oferta seja removida pela aplica��o.
   *
   * Essa opera��o n�o � bloqueante, o registro da oferta ser� feito por uma
   * outra thread. O objeto oferta local retornado permite realizar as
   * opera��es cab�veis sem que seja necess�rio se preocupar com o momento do
   * registro real da oferta no barramento.
   *
   * Essa opera��o pode bloquear enquanto n�o houver um login ativo. Caso seja
   * interrompida, retornar� <code>NULL</code> e se manter� interrompida.
   *
   * @param service_ref o servi�o a ser publicado.
   * @param properties as propriedades a serem associados � publica��o do
   *        servi�o.
   * @return Uma representa��o local da oferta que ser� mantida publicada no
   * barramento.
   */
  LocalOffer registerService(IComponent service_ref, ArrayListMultimap<String,
    String> properties);

  /**
   * Busca por ofertas que apresentem um conjunto de propriedades definido.
   * Em particular, caso nenhuma propriedade seja especificada, nenhuma
   * oferta ser� inclu�da no resultado dessa opera��o. As propriedades
   * utilizadas nas buscas podem ser as fornecidas no momento do registro da
   * oferta de servi�o, assim como as geradas automaticamente pelo registro
   * de ofertas de servi�os do barramento.
   *
   * Essa opera��o realiza uma chamada remota e � bloqueante.
   *
   * Essa opera��o pode bloquear enquanto n�o houver um login ativo. Caso seja
   * interrompida, retornar� uma lista vazia e se manter� interrompida.
   *
   * @param properties Propriedades que as ofertas de servi�os desejadas devem
   *        apresentar.
   * 
   * @return Uma lista com as ofertas de servi�o encontradas.
   *
   * @throws ServiceFailure Caso o registro de ofertas reporte alguma falha ao
   * realizar a opera��o.
   */
  List<RemoteOffer> findServices(ArrayListMultimap<String, String> properties)
    throws ServiceFailure;

  /**
   * Obt�m uma lista de todas as ofertas de servi�o registradas no barramento.
   * Essa opera��o realiza uma chamada remota e � bloqueante.
   *
   * Essa opera��o pode bloquear enquanto n�o houver um login ativo. Caso seja
   * interrompida, retornar� uma lista vazia e se manter� interrompida.
   *
   * @return Uma lista com as ofertas de servi�o registradas no barramento no
   * momento da requisi��o.
   * @throws ServiceFailure Caso o registro de ofertas reporte alguma falha ao
   * realizar a opera��o.
   */
  List<RemoteOffer> getAllServices() throws ServiceFailure;

  /**
   * Solicita que seja cadastrado um observador interessado em receber
   * eventos de registro de ofertas, de acordo com um conjunto de
   * propriedades especificadas. A inscri��o local retornada representa essa
   * solicita��o, que ser� mantida pelo registro de ofertas local at� que haja
   * um logout expl�cito ou que o observador seja removido pela aplica��o.
   *
   * Para cada observador subscrito pela aplica��o, ser� criado um objeto
   * CORBA para o recebimento dos eventos gerados pelo barramento.
   *
   * Essa opera��o n�o � bloqueante, o cadastro do observador ser� feito por
   * uma outra thread. O objeto subscri��o retornado permite realizar as
   * opera��es cab�veis sem que seja necess�rio se preocupar com o momento do
   * cadastro real do observador no barramento.
   *
   * Essa opera��o pode bloquear enquanto n�o houver um login ativo. Caso seja
   * interrompida, retornar� <code>NULL</code> e se manter� interrompida.
   *
   * @param observer O observador.
   * @param properties Propriedades que as ofertas de servi�os registradas devem
   *        apresentar para que o observador seja notificado.
   * 
   * @return Objeto que representa a inscri��o do observador.
   * @throws ServantNotActive Caso n�o seja poss�vel ativar no POA fornecido
   * pela conex�o um objeto CORBA que � criado para atender �s notifica��es
   * do barramento.
   * @throws WrongPolicy Caso haja alguma inconsist�ncia com as pol�ticas do
   * POA fornecido pela conex�o ao criar um objeto CORBA para atender �s
   * notifica��es do barramento.
   */
  OfferRegistrySubscription subscribeObserver(OfferRegistryObserver observer,
    ArrayListMultimap<String, String> properties) throws ServantNotActive,
    WrongPolicy;
}