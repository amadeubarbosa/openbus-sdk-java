package tecgraf.openbus.interop.delegation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.Connection;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.interop.util.Utils;

public class MessengerImpl extends MessengerPOA {

  private Connection conn;
  private Map<String, List<PostDesc>> inboxOf;

  public MessengerImpl(Connection conn) {
    this.conn = conn;
    this.inboxOf =
      Collections.synchronizedMap(new HashMap<String, List<PostDesc>>());
  }

  @Override
  public void post(String to, String message) {
    LoginInfo caller = conn.getCallerChain().caller();
    LoginInfo[] originators = conn.getCallerChain().originators();
    String from = caller.entity;
    System.out.println(String.format("post para '%s' de '%s'", to, Utils
      .chain2str(originators, caller)));
    synchronized (inboxOf) {
      List<PostDesc> list = this.inboxOf.get(to);
      if (list == null) {
        list = new ArrayList<PostDesc>();
        inboxOf.put(to, list);
      }
      list.add(new PostDesc(from, message));
    }
  }

  @Override
  public PostDesc[] receivePosts() {
    CallerChain chain = conn.getCallerChain();
    LoginInfo caller = chain.caller();
    LoginInfo[] originators = chain.originators();
    String owner = caller.entity;
    if (originators.length > 0) {
      owner = originators[0].entity;
    }
    System.out.println(String.format("download das mensagens de %s por %s",
      owner, Utils.chain2str(originators, caller)));
    List<PostDesc> list = this.inboxOf.remove(owner);
    if (list == null) {
      list = new ArrayList<PostDesc>();
    }
    return list.toArray(new PostDesc[list.size()]);
  }
}
