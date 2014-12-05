package tecgraf.openbus.core;

import org.omg.CORBA.ORB;
import org.omg.PortableInterceptor.ORBInitializer;

/**
 * Classe que encapsula as informações de nome da classe e identificador da
 * classe responsável por inicializar o {@link ORB}.
 * 
 * @author Tecgraf
 */
final class ORBInitializerInfo {
  /**
   * O identificador do inicializador do {@link ORB}.
   */
  private String id;

  /**
   * O nome da classe inicializadora do {@link ORB}
   */
  private String className;

  /**
   * Construtor.
   * 
   * @param clazz a classe responsável por inicializar o {@link ORB}
   */
  public ORBInitializerInfo(Class<? extends ORBInitializer> clazz) {
    this(clazz, clazz.getName());
  }

  /**
   * Construtor.
   * 
   * @param clazz a classe responsável por inicializar o {@link ORB}
   * @param id identificador do inicializador do {@link ORB}
   */
  public ORBInitializerInfo(Class<? extends ORBInitializer> clazz, String id) {
    if (id == null) {
      throw new IllegalArgumentException(
        "O identificador do inicializador do ORB não pode ser nulo");
    }
    this.className = clazz.getName();
    this.id = id;
  }

  /**
   * Recupera o identificador do inicializador do {@link ORB}.
   * 
   * @return o identificador.
   */
  String getId() {
    return this.id;
  }

  /**
   * Recupera o nome da classe inicializadora do {@link ORB}
   * 
   * @return o nome da classe.
   */
  String getClassName() {
    return this.className;
  }
}
