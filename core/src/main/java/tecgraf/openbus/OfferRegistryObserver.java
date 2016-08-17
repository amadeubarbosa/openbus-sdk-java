package tecgraf.openbus;

/**
 * Interface de observação de eventos de registro de ofertas.
 * <p>
 * O registro de ofertas local ({@link OfferRegistry}) se encarrega de garantir
 * o repasse de eventos que possam ter ocorrido durante momentos de falta de
 * conexão ao barramento. Dependendo do intervalo de tempo sem conexão com
 * o barramento (<i>t</i>), o registro pode não repassar alguns eventos, onde os
 * possíveis cenários para não repassar os eventos são listados a seguir:
 * 
 * <li>Caso durante o intervalo <i>t</i> seja registrada uma oferta que atenda
 * aos critérios de observação, mas durante esse mesmo intervalo a mesma oferta
 * tenha seu registro removido.
 * 
 *
 * @author Tecgraf/PUC-Rio
 */
public interface OfferRegistryObserver {

  /**
   * Uma oferta que atende aos critérios de observação foi registrada no
   * barramento.
   * 
   * @param offer a oferta registrada.
   */
  void offerRegistered(RemoteOffer offer);
}
