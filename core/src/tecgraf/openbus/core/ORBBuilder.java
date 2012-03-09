package tecgraf.openbus.core;

import java.util.Properties;

import org.omg.CORBA.ORB;

final class ORBBuilder {
  private String[] args;
  private Properties props;

  public ORBBuilder() {
    this(null, null);
  }

  public ORBBuilder(String[] args) {
    this(args, null);
  }

  public ORBBuilder(Properties props) {
    this(null, props);
  }

  public ORBBuilder(String[] args, Properties props) {
    this.args = args;
    if (props == null) {
      this.props = new Properties();
    }
    else {
      this.props = new Properties(props);
    }
    this.props.put("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
    this.props.put("org.omg.CORBA.ORBSingletonClass",
      "org.jacorb.orb.ORBSingleton");
  }

  public void setHost(String host) {
    this.props.put("OAIAddr", host);
  }

  public void setPort(int port) {
    this.props.put("OAPort", String.valueOf(port));
  }

  public void addInitializer(ORBInitializerInfo initializer) {
    this.props.put("org.omg.PortableInterceptor.ORBInitializerClass."
      + initializer.getId(), initializer.getClassName());
  }

  public ORB build() {
    return ORB.init(this.args, this.props);
  }
}
