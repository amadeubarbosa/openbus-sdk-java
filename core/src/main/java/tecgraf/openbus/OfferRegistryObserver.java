package tecgraf.openbus;

/**
 * Interface de observa��o de eventos de registro de ofertas.
 * <p>
 * O registro de ofertas local ({@link OfferRegistry}) se encarrega de garantir
 * o repasse de eventos que possam ter ocorrido durante momentos de falta de
 * conex�o ao barramento. Dependendo do intervalo de tempo sem conex�o com
 * o barramento (<i>t</i>), o registro pode n�o repassar alguns eventos, onde os
 * poss�veis cen�rios para n�o repassar os eventos s�o listados a seguir:
 * 
 * <li>Caso durante o intervalo <i>t</i> seja registrada uma oferta que atenda
 * aos crit�rios de observa��o, mas durante esse mesmo intervalo a mesma oferta
 * tenha seu registro removido.
 * 
 *
 * @author Tecgraf/PUC-Rio
 */
public interface OfferRegistryObserver {

  /**
   * Uma oferta que atende aos crit�rios de observa��o foi registrada no
   * barramento.
   * 
   * @param offer a oferta registrada.
   */
  void offerRegistered(RemoteOffer offer);
}
