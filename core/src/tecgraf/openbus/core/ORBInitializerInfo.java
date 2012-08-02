package tecgraf.openbus.core;

import org.omg.PortableInterceptor.ORBInitializer;

final class ORBInitializerInfo {
  private String id;
  private String className;

  public ORBInitializerInfo(Class<? extends ORBInitializer> clazz) {
    this(clazz, clazz.getName());
  }

  public ORBInitializerInfo(Class<? extends ORBInitializer> clazz, String id) {
    if (id == null) {
      throw new IllegalArgumentException(
        "O identificador do inicializador do ORB não pode ser nulo");
    }
    this.className = clazz.getName();
    this.id = id;
  }

  String getId() {
    return this.id;
  }

  String getClassName() {
    return this.className;
  }
}
