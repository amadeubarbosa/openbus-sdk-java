package tecgraf.openbus;

import com.google.common.collect.ArrayListMultimap;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;

import java.util.concurrent.TimeoutException;

/**
 * Representa uma inscri��o de um observador de registro de oferta de servi�o.
 * Essa inscri��o ser� mantida no barramento pelo registro de ofertas do qual se
 * originou, at� que a aplica��o remova-a ou realize um logout proposital.
 *
 * Os poss�veis eventos gerados s�o definidos pela interface
 * {@link OfferRegistryObserver}.
 *
 * @author Tecgraf
 */
public interface OfferRegistrySubscription {
  /**
   * Indica se a subscri��o foi bem-sucedida ou n�o. Caso algum erro a esteja
   * impedindo, lan�ar� a exce��o que corresponde ao �ltimo erro recebido.
   *
   * O SDK continuar� tentando realizar a subscri��o periodicamente
   * independente do erro acusado, at� conseguir ou at� que a mesma seja
   * removida pelo usu�rio atrav�s do m�todo {@link #remove()}.
   *
   * Caso a subscri��o n�o esteja cadastrada no barramento no momento da
   * chamada ou n�o haja um login v�lido, a chamada ficar� bloqueada at� que
   * essas condi��es sejam cumpridas. Caso seja interrompida, retornar�
   * false e se manter� interrompida.
   *
   * @throws ServiceFailure Caso haja algum erro inesperado no barramento ao
   * realizar a subscri��o.
   * @return True caso a subscri��o esteja ativa, false caso tenha sido
   * cancelada pelo usu�rio atrav�s do m�todo {@link #remove()}.
   */
  boolean subscribed() throws ServiceFailure;

  /**
   * Indica se a subscri��o foi bem-sucedida ou n�o. Caso algum erro a esteja
   * impedindo, lan�ar� a exce��o que corresponde ao �ltimo erro recebido.
   *
   * O SDK continuar� tentando realizar a subscri��o periodicamente
   * independente do erro acusado, at� conseguir ou at� que a mesma seja
   * removida pelo usu�rio atrav�s do m�todo {@link #remove()}.
   *
   * Caso a subscri��o n�o esteja cadastrada no barramento no momento da
   * chamada ou n�o haja um login v�lido, a chamada ficar� bloqueada at� que
   * essas condi��es sejam cumpridas ou o tempo se esgote. Caso seja
   * interrompida, retornar� false e se manter� interrompida.
   *
   * @throws ServiceFailure Caso haja algum erro inesperado no barramento ao
   * realizar a subscri��o.
   * @throws TimeoutException O tempo especificado se esgotou antes de a
   * subscri��o ser completada.
   * @return True caso a subscri��o esteja ativa, false caso tenha sido
   * cancelada pelo usu�rio atrav�s do m�todo {@link #remove()}.
   */
  boolean subscribed(long timeoutMillis) throws ServiceFailure,
    TimeoutException;

  /**
   * Recupera a refer�ncia para o observador inscrito pela aplica��o.
   * 
   * @return o observador.
   */
  OfferRegistryObserver observer();

  /**
   * Recupera a lista de propriedades de oferta nas quais o observador est�
   * interessado.
   * 
   * @return a lista de propriedades.
   */
  ArrayListMultimap<String, String> properties();

  /**
   * Remove a incri��o do observador, localmente. Uma outra thread ser�
   * encarregada de remover o observador do barramento.
   *
   * Essa opera��o pode bloquear enquanto n�o houver um login ativo. Caso seja
   * interrompida, retornar� e se manter� interrompida.
   *
   * Ap�s a execu��o deste m�todo, os m�todos {@link #subscribed()} e
   * {@link #subscribed(long)} retornar�o <code>NULL</code>.
   */
  void remove();
}