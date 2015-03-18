package tecgraf.openbus.interop.delegation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.utils.LibUtils;

public class MessengerImpl extends MessengerPOA {

  private OpenBusContext context;
  private Map<String, List<PostDesc>> inboxOf;

  public MessengerImpl(OpenBusContext context) {
    this.context = context;
    this.inboxOf =
      Collections.synchronizedMap(new HashMap<String, List<PostDesc>>());
  }

  @Override
  public void post(String to, String message) {
    CallerChain chain = context.getCallerChain();
    String from = LibUtils.chain2str(chain);
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
    CallerChain chain = context.getCallerChain();
    LoginInfo caller = chain.caller();
    LoginInfo[] originators = chain.originators();
    String owner = caller.entity;
    if (originators.length > 0) {
      owner = originators[0].entity;
    }
    List<PostDesc> list = this.inboxOf.remove(owner);
    if (list == null) {
      list = new ArrayList<PostDesc>();
    }
    return list.toArray(new PostDesc[list.size()]);
  }
}
