/*
 * $Id$
 */
package tecgraf.openbus.interceptors;

import org.omg.CORBA.LocalObject;
import org.omg.IOP.Codec;

/**
 * Implementa um interceptador, para o tratamento de informações no contexto de
 * uma requisição.
 * 
 * @author Tecgraf/PUC-Rio
 */
abstract class InterceptorImpl extends LocalObject implements
  org.omg.PortableInterceptor.Interceptor {
  /**
   * Representa a identificação do "service context" (contexto) utilizado para
   * transporte de credenciais em requisições de serviço.
   */
  protected static final int CONTEXT_ID = 1234;

  /**
   * Representa o objeto responsável pelo marshall/unmarshall de credenciais
   * para transporte/obtenção de contextos de requisições de servico.
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
   * @param codec O objeto responsável pelo marshall/unmarshall de credenciais.
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
   * Obtém o codificador responsável pelo marshall/unmarshall de credenciais.
   * 
   * @return O codificador responsável pelo marshall/unmarshall de credenciais.
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
