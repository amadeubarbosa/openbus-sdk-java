/*
 * $Id$
 */
package tecgraf.openbus.interceptors;

import org.omg.CORBA.LocalObject;
import org.omg.IOP.Codec;

/**
 * Implementa um interceptador, para o tratamento de informa��es no contexto de
 * uma requisi��o.
 * 
 * @author Tecgraf/PUC-Rio
 */
abstract class InterceptorImpl extends LocalObject implements
  org.omg.PortableInterceptor.Interceptor {
  /**
   * Representa a identifica��o do "service context" (contexto) utilizado para
   * transporte de credenciais em requisi��es de servi�o.
   */
  protected static final int CONTEXT_ID = 1234;

  /**
   * Representa o objeto respons�vel pelo marshall/unmarshall de credenciais
   * para transporte/obten��o de contextos de requisi��es de servico.
   */
  private Codec codec;

  /**
   * O nome do interceptador.
   */
  private String name;

  /**
   * Cria o interceptador.
   * 
   * @param name O nome do interceptador.
   * @param codec O objeto respons�vel pelo marshall/unmarshall de credenciais.
   */
  protected InterceptorImpl(String name, Codec codec) {
    this.name = name;
    this.codec = codec;
  }

  /**
   * {@inheritDoc}
   */
  public final String name() {
    return this.name;
  }

  /**
   * Obt�m o codificador respons�vel pelo marshall/unmarshall de credenciais.
   * 
   * @return O codificador respons�vel pelo marshall/unmarshall de credenciais.
   */
  protected final Codec getCodec() {
    return this.codec;
  }

  /**
   * {@inheritDoc}
   */
  public final void destroy() {
    // Nada a ser feito.
  }
}
