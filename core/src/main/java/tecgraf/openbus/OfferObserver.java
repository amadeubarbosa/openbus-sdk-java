package tecgraf.openbus;

/**
 * Interface de observação de eventos relacionados a uma oferta específica.
 * <p>
 * O registro de ofertas local ({@link OfferRegistry}) se encarrega de repassar
 * os eventos que estão relacionados à oferta observada.
 * 
 * @author Tecgraf
 */
public interface OfferObserver {
  /**
   * As propriedades da oferta foram atualizadas no barramento.
   * 
   * @param offer A oferta cujas propriedades foram atualizadas. O objeto
   * JAVA RemoteOffer é apenas uma representação local da oferta e não deve
   * ser utilizado para identificá-la. Para tal, utilize o identificador da
   * oferta presente em suas propriedades.
   */
  void propertiesChanged(RemoteOffer offer);

  /**
   * A oferta teve sua publicação no barramento removida.
   * 
   * @param offer A oferta cuja publicação foi removida do barramento. O objeto
   * JAVA RemoteOffer é apenas uma representação local da oferta e não deve
   * ser utilizado para identificá-la. Para tal, utilize o identificador da
   * oferta presente em suas propriedades.
   */
  void removed(RemoteOffer offer);
}
