package tecgraf.openbus;

/**
 * Representa uma oferta gerada por este processo. Essa oferta será mantida no
 * barramento pelo registro de ofertas do qual se originou, até que a
 * aplicação remova-a ou realize um logout proposital.
 *
 * @author Tecgraf
 */
public interface LocalOffer {
  /**
   * Fornece uma oferta remota relacionada à oferta existente no barramento.
   * Caso a oferta não esteja registrada no barramento no momento da
   * chamada ou não haja um login ativo, a chamada ficará bloqueada até que
   * essas condições sejam cumpridas. Caso seja interrompida, retornará
   * <code>NULL</code> e se manterá interrompida.
   *
   * @return A oferta remota.
   */
  RemoteOffer remoteOffer();

  /**
   * Fornece uma oferta remota relacionada à oferta existente no barramento.
   * Se o tempo especificado se esgotar, <code>NULL</code> será retornado.
   *
   * Caso a oferta não esteja registrada no barramento no momento da
   * chamada ou não haja um login ativo, a chamada ficará bloqueada até que
   * essas condições sejam cumpridas. Caso seja interrompida, retornará
   * <code>NULL</code> e se manterá interrompida.
   *
   * @param timeoutmillis O tempo máximo a aguardar pelo registro da oferta
   *                      remota, em milisegundos. O valor 0 neste parâmetro
   *                      e no parâmetro nanos faz com que a thread aguarde
   *                      eternamente, como na assinatura sem parâmetros.
   * @param nanos Caso uma granularidade de nanossegundos seja necessária.
   * @return A oferta remota, ou null caso não esteja registrada remotamente.
   */
  RemoteOffer remoteOffer(long timeoutmillis, int nanos);

  /**
   * Solicita que a oferta não seja mais mantida no barramento. Caso a oferta
   * esteja registrada no momento da chamada, ela será removida do barramento
   * em uma outra thread.
   *
   * Essa operação pode bloquear enquanto não houver um login ativo. Caso seja
   * interrompida, retornará e se manterá interrompida.
   */
  void remove();
}
