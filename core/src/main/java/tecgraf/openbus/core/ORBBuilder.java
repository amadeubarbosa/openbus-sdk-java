package tecgraf.openbus.core;

import java.util.Properties;

import org.omg.CORBA.ORB;

/**
 * Classe respons�vel por inicializar o {@link ORB}
 * 
 * @author Tecgraf
 */
final class ORBBuilder {
  /** Argumentos de linha de comando */
  private String[] args;
  /** Propriedades */
  private Properties props;

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
    // propriedades padr�es mas n�o obrigat�rias (podem ser sobrescritas)
    this.props.put("jacorb.connection.client.disconnect_after_systemexception",
      false);
    if (props != null) {
      this.props.putAll(props);
    }
    // propriedade obrigat�ria
    this.props.setProperty("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
  }

  /**
   * Configura as informa��es do inicializador do ORB.
   * 
   * @param initializer informa��es do inicializador.
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
    return ORB.init(this.args, this.props);
  }

}