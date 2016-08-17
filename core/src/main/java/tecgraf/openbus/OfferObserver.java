package tecgraf.openbus;

/**
 * Interface de observação de eventos relacionados a uma oferta específica.
 * <p>
 * O registro de ofertas local ({@link OfferRegistry}) se encarrega de repassar
 * os eventos que estão relacionados à oferta observada. Em casos de intervalos
 * de tempo sem conexão ao barramento, o registro local garante que o último
 * evento ocorrido ao longo desse intervalo será disparado quando
 * for restabelecida a conexão com o barramento.
 * 
 * @author Tecgraf
 */
public interface OfferObserver {
  /**
   * As propriedades da oferta foram atualizadas no barramento.
   * 
   * @param offer a oferta cujas propriedades foram atualizadas.
   */
  void propertiesChanged(RemoteOffer offer);

  /**
   * A oferta teve sua publicação no barramento removida.
   * 
   * @param offer a oferta cuja publicação foi removida do barramento.
   */
  void removed(RemoteOffer offer);
}
