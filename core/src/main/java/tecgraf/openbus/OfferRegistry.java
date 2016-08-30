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
   * Fornece a conexão utilizada para as chamadas remotas desse registro.
   * @return A conexão.
   */
  Connection conn();

  /**
   * Solicita que seja registrado um serviço, representado por um componente
   * ({@link IComponent}), no barramento. A oferta local retornada representa
   * essa solicitação, que será mantida pelo registro de ofertas local até
   * que haja um logout explícito ou que a oferta seja removida pela aplicação.
   *
   * Essa operação não é bloqueante, o registro da oferta será feito por uma
   * outra thread. O objeto oferta local retornado permite realizar as
   * operações cabíveis sem que seja necessário se preocupar com o momento do
   * registro real da oferta no barramento.
   *
   * Essa operação pode bloquear enquanto não houver um login ativo. Caso seja
   * interrompida, retornará <code>NULL</code> e se manterá interrompida.
   *
   * @param service_ref o serviço a ser publicado.
   * @param properties as propriedades a serem associados à publicação do
   *        serviço.
   * @return Uma representação local da oferta que será mantida publicada no
   * barramento.
   */
  LocalOffer registerService(IComponent service_ref, ArrayListMultimap<String,
    String> properties);

  /**
   * Busca por ofertas que apresentem um conjunto de propriedades definido.
   * Em particular, caso nenhuma propriedade seja especificada, nenhuma
   * oferta será incluída no resultado dessa operação. As propriedades
   * utilizadas nas buscas podem ser as fornecidas no momento do registro da
   * oferta de serviço, assim como as geradas automaticamente pelo registro
   * de ofertas de serviços do barramento.
   *
   * Essa operação realiza uma chamada remota e é bloqueante.
   *
   * Essa operação pode bloquear enquanto não houver um login ativo. Caso seja
   * interrompida, retornará uma lista vazia e se manterá interrompida.
   *
   * @param properties Propriedades que as ofertas de serviços desejadas devem
   *        apresentar.
   * 
   * @return Uma lista com as ofertas de serviço encontradas.
   *
   * @throws ServiceFailure Caso o registro de ofertas reporte alguma falha ao
   * realizar a operação.
   */
  List<RemoteOffer> findServices(ArrayListMultimap<String, String> properties)
    throws ServiceFailure;

  /**
   * Obtém uma lista de todas as ofertas de serviço registradas no barramento.
   * Essa operação realiza uma chamada remota e é bloqueante.
   *
   * Essa operação pode bloquear enquanto não houver um login ativo. Caso seja
   * interrompida, retornará uma lista vazia e se manterá interrompida.
   *
   * @return Uma lista com as ofertas de serviço registradas no barramento no
   * momento da requisição.
   * @throws ServiceFailure Caso o registro de ofertas reporte alguma falha ao
   * realizar a operação.
   */
  List<RemoteOffer> getAllServices() throws ServiceFailure;

  /**
   * Solicita que seja cadastrado um observador interessado em receber
   * eventos de registro de ofertas, de acordo com um conjunto de
   * propriedades especificadas. A inscrição local retornada representa essa
   * solicitação, que será mantida pelo registro de ofertas local até que haja
   * um logout explícito ou que o observador seja removido pela aplicação.
   *
   * Para cada observador subscrito pela aplicação, será criado um objeto
   * CORBA para o recebimento dos eventos gerados pelo barramento.
   *
   * Essa operação não é bloqueante, o cadastro do observador será feito por
   * uma outra thread. O objeto subscrição retornado permite realizar as
   * operações cabíveis sem que seja necessário se preocupar com o momento do
   * cadastro real do observador no barramento.
   *
   * Essa operação pode bloquear enquanto não houver um login ativo. Caso seja
   * interrompida, retornará <code>NULL</code> e se manterá interrompida.
   *
   * @param observer O observador.
   * @param properties Propriedades que as ofertas de serviços registradas devem
   *        apresentar para que o observador seja notificado.
   * 
   * @return Objeto que representa a inscrição do observador.
   * @throws ServantNotActive Caso não seja possível ativar no POA fornecido
   * pela conexão um objeto CORBA que é criado para atender às notificações
   * do barramento.
   * @throws WrongPolicy Caso haja alguma inconsistência com as políticas do
   * POA fornecido pela conexão ao criar um objeto CORBA para atender às
   * notificações do barramento.
   */
  OfferRegistrySubscription subscribeObserver(OfferRegistryObserver observer,
    ArrayListMultimap<String, String> properties) throws ServantNotActive,
    WrongPolicy;
}