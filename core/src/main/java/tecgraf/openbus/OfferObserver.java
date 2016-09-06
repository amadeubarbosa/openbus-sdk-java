package tecgraf.openbus;

/**
 * Interface de observa��o de eventos relacionados a uma oferta espec�fica.
 * <p>
 * O registro de ofertas local ({@link OfferRegistry}) se encarrega de repassar
 * os eventos que est�o relacionados � oferta observada.
 * 
 * @author Tecgraf
 */
public interface OfferObserver {
  /**
   * As propriedades da oferta foram atualizadas no barramento.
   * 
   * @param offer A oferta cujas propriedades foram atualizadas. O objeto
   * JAVA RemoteOffer � apenas uma representa��o local da oferta e n�o deve
   * ser utilizado para identific�-la. Para tal, utilize o identificador da
   * oferta presente em suas propriedades.
   */
  void propertiesChanged(RemoteOffer offer);

  /**
   * A oferta teve sua publica��o no barramento removida.
   * 
   * @param offer A oferta cuja publica��o foi removida do barramento. O objeto
   * JAVA RemoteOffer � apenas uma representa��o local da oferta e n�o deve
   * ser utilizado para identific�-la. Para tal, utilize o identificador da
   * oferta presente em suas propriedades.
   */
  void removed(RemoteOffer offer);
}
