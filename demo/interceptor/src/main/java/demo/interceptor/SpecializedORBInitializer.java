package demo.interceptor;

import java.util.Properties;

import org.omg.CORBA.ORB;

import tecgraf.openbus.core.ORBInitializer;

/**
 * Inicializador de ORB especializado que inclui interceptadores no ORB para
 * inclusão de informações extras no contexto das chamadas CORBA.
 * 
 * @author Tecgraf
 */
public class SpecializedORBInitializer {

  /**
   * Identificador do contexto que será utilizado.
   */
  private static final int BDIEP_CONTEXT_ID = 1111771392; // 0x42444900 - BDI\0

  /**
   * Inicializa um {@link ORB} chamando o método de inicialização de
   * {@link ORBInitializer#initORB}, mas também insere novos interceptadores no
   * ORB.
   * 
   * @return o {@link ORB} criado.
   */
  public static ORB initORB() {
    Properties props = new Properties();
    return initORB(null, props);
  }

  /**
   * Inicializa um {@link ORB} chamando o método de inicialização de
   * {@link ORBInitializer}, mas também insere novos interceptadores no ORB.
   * 
   * @param args Parâmetros usados na inicialização do {@link ORB}, similar à
   *        operação {@link ORB#init} definida pelo padrão CORBA.
   * @param props Propriedades usados na inicialização do {@link ORB}, similar à
   *        operação {@link ORB#init} definida pelo padrão CORBA.
   * @return o {@link ORB} criado.
   */
  public static ORB initORB(String[] args, Properties props) {
    props.put("org.omg.PortableInterceptor.ORBInitializerClass."
      + PersonalInitializer.class.getSimpleName(), PersonalInitializer.class
      .getName());
    // inicializando e configurando o ORB
    ORB orb = ORBInitializer.initORB(args, props);
    ContextInspector inspector = ContextInspector.getContextInspector(orb);
    inspector.setORB(orb);
    inspector.setContextId(BDIEP_CONTEXT_ID);
    return orb;
  }
}
