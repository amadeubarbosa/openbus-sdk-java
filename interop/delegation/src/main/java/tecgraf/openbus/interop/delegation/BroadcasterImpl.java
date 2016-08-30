package tecgraf.openbus.interop.delegation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;

public class BroadcasterImpl extends BroadcasterPOA {

  private OpenBusContext context;
  private final List<String> subscribers;
  private Messenger messenger;

  public BroadcasterImpl(OpenBusContext context, Messenger messenger) {
    this.context = context;
    this.subscribers = Collections.synchronizedList(new ArrayList<>());
    this.messenger = messenger;
  }

  @Override
  public void post(String message) {
    context.joinChain();
    synchronized (subscribers) {
      for (String user : subscribers) {
        messenger.post(user, message);
      }
    }
  }

  @Override
  public void subscribe() {
    LoginInfo caller = context.getCallerChain().caller();
    String user = caller.entity;
    subscribers.add(user);
  }

  @Override
  public void unsubscribe() {
    LoginInfo caller = context.getCallerChain().caller();
    String user = caller.entity;
    subscribers.remove(user);
  }

}
