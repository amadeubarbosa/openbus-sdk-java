package tecgraf.openbus.core;

import java.util.Map.Entry;
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
      this.props = buildDefaultProperties();
    }
    else {
      this.props = buildFromProperties(props);
    }
    this.props.setProperty("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
    this.props.setProperty("org.omg.CORBA.ORBSingletonClass",
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

  private Properties buildDefaultProperties() {
    Properties props = new Properties();
    props.setProperty("jacorb.log.default.verbosity", "1"); // ERROR
    props
      .setProperty("jacorb.connection.client.pending_reply_timeout", "30000");
    return props;
  }

  private Properties buildFromProperties(Properties props) {
    Properties result = new Properties(buildDefaultProperties());
    for (Entry<Object, Object> entry : props.entrySet()) {
      result.setProperty(String.valueOf(entry.getKey()), String.valueOf(entry
        .getValue()));
    }
    return result;
  }
}
