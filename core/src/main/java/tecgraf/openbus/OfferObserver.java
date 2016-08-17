package tecgraf.openbus;

/**
 * Interface de observa��o de eventos relacionados a uma oferta espec�fica.
 * <p>
 * O registro de ofertas local ({@link OfferRegistry}) se encarrega de repassar
 * os eventos que est�o relacionados � oferta observada. Em casos de intervalos
 * de tempo sem conex�o ao barramento, o registro local garante que o �ltimo
 * evento ocorrido ao longo desse intervalo ser� disparado quando
 * for restabelecida a conex�o com o barramento.
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
   * A oferta teve sua publica��o no barramento removida.
   * 
   * @param offer a oferta cuja publica��o foi removida do barramento.
   */
  void removed(RemoteOffer offer);
}
