package tecgraf.openbus.interop.delegation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tecgraf.openbus.Openbus;
import tecgraf.openbus.core.v1_05.access_control_service.Credential;

public class MessengerImpl extends MessengerPOA {

  private Map<String, List<PostDesc>> inboxOf;

  public MessengerImpl() {
    this.inboxOf =
      Collections.synchronizedMap(new HashMap<String, List<PostDesc>>());
  }

  @Override
  public void post(String to, String message) {
    Credential callCred = Openbus.getInstance().getInterceptedCredential();
    String from =
      !callCred.delegate.equals("") ? callCred.delegate : callCred.owner;
    System.out.println(String.format("post para '%s' de '%s'", to, from));
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
    Credential callCred = Openbus.getInstance().getInterceptedCredential();
    String owner = callCred.owner;
    String delegate =
      !callCred.delegate.equals("") ? callCred.delegate : callCred.owner;
    System.out.println(String.format("download das mensagens de %s por %s",
      delegate, owner));
    List<PostDesc> list = this.inboxOf.remove(delegate);
    if (list == null) {
      list = new ArrayList<PostDesc>();
    }
    return list.toArray(new PostDesc[list.size()]);
  }
}
