package tecgraf.openbus.interop.delegation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.interop.util.Utils;

public class BroadcasterImpl extends BroadcasterPOA {

  private OpenBusContext context;
  private List<String> subscribers;
  private Messenger messenger;

  public BroadcasterImpl(OpenBusContext context, Messenger messenger) {
    this.context = context;
    this.subscribers = Collections.synchronizedList(new ArrayList<String>());
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
    LoginInfo[] originators = context.getCallerChain().originators();
    String user = caller.entity;
    System.out.println("inscrição de " + Utils.chain2str(originators, caller));
    subscribers.add(user);
  }

  @Override
  public void unsubscribe() {
    LoginInfo caller = context.getCallerChain().caller();
    LoginInfo[] originators = context.getCallerChain().originators();
    String user = caller.entity;
    System.out.println("cancelando inscrição de "
      + Utils.chain2str(originators, caller));
    subscribers.remove(user);
  }

}
