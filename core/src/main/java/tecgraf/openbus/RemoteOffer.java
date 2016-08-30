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
 * Representação de uma oferta registrada no barramento.
 *
 * @author Tecgraf
 */
public interface RemoteOffer {
  /**
   * Fornece informações sobre o login que registrou esta oferta específica.
   * Não realiza chamadas remotas.
   *
   * @return Informações sobre o login que registrou esta oferta.
   */
  LoginInfo owner();

  /**
   * Fornece uma referência para o serviço remoto.
   *
   * @return A referência.
   */
  IComponent service_ref();

  /**
   * Fornece o mapa de propriedades associadas à oferta remota. O mapa pode
   * ser atualizado caso o parâmetro update seja verdadeiro. Nesse caso, será
   * realizada uma chamada remota bloqueante ao barramento. Caso contrário, o
   * mapa corresponderá às propriedades obtidas na última atualização.
   *
   * Alterações às propriedades também podem ser acompanhadas através de
   * observadores de ofertas.
   *
   * @param update Indica se uma chamada remota ao barramento deve ser
   *               realizada para a atualização do mapa de propriedades.
   * @return O mapa de propriedades.
   */
  ArrayListMultimap<String, String> properties(boolean update);

  /**
   * Altera o mapa local de propriedades associadas à oferta. O mapa da
   * oferta remota no barramento não é atualizado, e portanto não são
   * realizadas chamadas remotas. Este método é útil para quando se recebe uma
   * notificação de atualização através de um observador e se deseja
   * atualizar objetos RemoteOffer diferentes mas referentes à mesma oferta no
   * barramento.
   *
   * @param properties As novas propriedades para a oferta local.
   */
  void propertiesLocal(ArrayListMultimap<String, String> properties);

  /**
   * Altera o mapa de propriedades associadas à oferta remota. Realiza uma
   * chamada remota bloqueante ao barramento. Essa chamada só pode ser feita
   * pelo dono da oferta ou por um usuário administrador.
   *
   * @param properties As novas propriedades para a oferta.
   * @throws UnauthorizedOperation Caso não haja permissão para realizar a
   * alteração.
   * @throws InvalidProperties Caso as propriedades contenham entradas
   * inválidas.
   * @throws ServiceFailure Caso o registro de ofertas reporte alguma falha ao
   * realizar a operação.
   */
  void propertiesRemote(ArrayListMultimap<String, String> properties) throws
    UnauthorizedOperation, InvalidProperties, ServiceFailure;

  /**
   * Remove a oferta do barramento. Esse método só pode ser usado pelo dono
   * da oferta ou por um usuário administrador. Essa operação realiza uma
   * chamada remota e é bloqueante.
   *
   * @throws ServiceFailure Caso o registro de ofertas reporte alguma falha ao
   * realizar a operação.
   * @throws UnauthorizedOperation
   */
  void remove() throws ServiceFailure, UnauthorizedOperation;

  /**
   * Solicita que seja cadastrado na oferta remota um observador interessado em
   * receber eventos dessa oferta. A inscrição local retornada representa
   * essa solicitação, que será mantida pelo registro de ofertas local até
   * que haja um logout explícito ou que o observador seja removido pela
   * aplicação.
   *
   * Essa operação não é bloqueante, o cadastro do observador será feito por
   * uma outra thread. O objeto subscrição retornado permite realizar
   * as operações cabíveis sem que seja necessário se preocupar com o
   * momento do cadastro real do observador no barramento.
   *
   * Essa operação pode bloquear enquanto não houver um login ativo. Caso seja
   * interrompida, retornará <code>NULL</code> e se manterá interrompida.
   *
   * @param observer o observador a ser cadastrado.
   * @return A solicitação de cadastro do observador. Caso a oferta já não
   * exista mais no barramento, será retornado <code>NULL</code>.
   * @throws ServantNotActive Caso não seja possível ativar no POA fornecido
   * pela conexão um objeto CORBA que é criado para atender às notificações
   * do barramento.
   * @throws WrongPolicy Caso haja alguma inconsistência com as políticas do
   * POA fornecido pela conexão ao criar um objeto CORBA para atender às
   * notificações do barramento.
   */
  OfferSubscription subscribeObserver(OfferObserver observer) throws
    ServantNotActive, WrongPolicy;
}
