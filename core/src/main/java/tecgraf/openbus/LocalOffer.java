package tecgraf.openbus;

/**
 * Representa uma oferta gerada por este processo. Essa oferta ser� mantida no
 * barramento pelo registro de ofertas do qual se originou, at� que a
 * aplica��o remova-a ou realize um logout proposital.
 *
 * @author Tecgraf
 */
public interface LocalOffer {
  /**
   * Fornece uma oferta remota relacionada � oferta existente no barramento.
   * Caso a oferta n�o esteja registrada no barramento no momento da
   * chamada ou n�o haja um login ativo, a chamada ficar� bloqueada at� que
   * essas condi��es sejam cumpridas. Caso seja interrompida, retornar�
   * <code>NULL</code> e se manter� interrompida.
   *
   * @return A oferta remota.
   */
  RemoteOffer remoteOffer();

  /**
   * Fornece uma oferta remota relacionada � oferta existente no barramento.
   * Se o tempo especificado se esgotar, <code>NULL</code> ser� retornado.
   *
   * Caso a oferta n�o esteja registrada no barramento no momento da
   * chamada ou n�o haja um login ativo, a chamada ficar� bloqueada at� que
   * essas condi��es sejam cumpridas. Caso seja interrompida, retornar�
   * <code>NULL</code> e se manter� interrompida.
   *
   * @param timeoutmillis O tempo m�ximo a aguardar pelo registro da oferta
   *                      remota, em milisegundos. O valor 0 neste par�metro
   *                      e no par�metro nanos faz com que a thread aguarde
   *                      eternamente, como na assinatura sem par�metros.
   * @param nanos Caso uma granularidade de nanossegundos seja necess�ria.
   * @return A oferta remota, ou null caso n�o esteja registrada remotamente.
   */
  RemoteOffer remoteOffer(long timeoutmillis, int nanos);

  /**
   * Solicita que a oferta n�o seja mais mantida no barramento. Caso a oferta
   * esteja registrada no momento da chamada, ela ser� removida do barramento
   * em uma outra thread.
   *
   * Essa opera��o pode bloquear enquanto n�o houver um login ativo. Caso seja
   * interrompida, retornar� e se manter� interrompida.
   */
  void remove();
}
