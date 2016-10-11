package tecgraf.openbus.core;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.INITIALIZE;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;

/**
 * Classe responsável por inicializar o {@link ORB}
 * 
 * @author Tecgraf
 */
final class ORBBuilder {
  /** Argumentos de linha de comando */
  private final String[] args;
  /** Propriedades */
  private final Properties props;
  /** Instância de logging */
  private static final Logger logger = Logger.getLogger(ORBBuilder.class
    .getName());

  /**
   * Construtor.
   */
  public ORBBuilder() {
    this(null, null);
  }

  /**
   * Construtor
   * 
   * @param args argumentos de linha de comando
   */
  public ORBBuilder(String[] args) {
    this(args, null);
  }

  /**
   * Construtor
   * 
   * @param props propriedades.
   */
  public ORBBuilder(Properties props) {
    this(null, props);
  }

  /**
   * Construtor
   * 
   * @param args argumentos de linha de comando
   * @param props propriedades
   */
  public ORBBuilder(String[] args, Properties props) {
    this.args = args;
    this.props = new Properties();
    // propriedades padrões mas não obrigatórias (podem ser sobrescritas)
    this.props.put("jacorb.connection.client.disconnect_after_systemexception",
      false);
    if (props != null) {
      this.props.putAll(props);
    }
    // propriedade obrigatória
    this.props.setProperty("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
  }

  /**
   * Configura as informações do inicializador do ORB.
   * 
   * @param initializer informações do inicializador.
   */
  public void addInitializer(ORBInitializerInfo initializer) {
    this.props.put("org.omg.PortableInterceptor.ORBInitializerClass."
      + initializer.getId(), initializer.getClassName());
  }

  /**
   * Constroi o {@link ORB}. Chama o {@link ORB#init(String[], Properties)}.
   * 
   * @return o ORB
   */
  public ORB build() {
    ORB orb = ORB.init(this.args, this.props);
    try {
      OpenBusContextImpl context = (OpenBusContextImpl) orb
        .resolve_initial_references("OpenBusContext");
      context.ORB(orb);
      context.POA(null);
    } catch (InvalidName e) {
      String message = "Falha inesperada ao registrar o POA no multiplexador";
      logger.log(Level.SEVERE, message, e);
      throw new INITIALIZE(message);
    }
    return orb;
  }
}
