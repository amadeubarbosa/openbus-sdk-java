package tecgraf.openbus.interop.delegation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tecgraf.openbus.Connection;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.interop.util.Utils;

public class BroadcasterImpl extends BroadcasterPOA {

  private Connection conn;
  private List<String> subscribers;
  private Messenger messenger;

  public BroadcasterImpl(Connection conn, Messenger messenger) {
    this.conn = conn;
    this.subscribers = Collections.synchronizedList(new ArrayList<String>());
    this.messenger = messenger;
  }

  @Override
  public void post(String message) {
    conn.joinChain();
    synchronized (subscribers) {
      for (String user : subscribers) {
        messenger.post(user, message);
      }
    }
  }

  @Override
  public void subscribe() {
    LoginInfo caller = conn.getCallerChain().caller();
    LoginInfo[] originators = conn.getCallerChain().originators();
    String user = caller.entity;
    System.out.println("inscrição de " + Utils.chain2str(originators, caller));
    subscribers.add(user);
  }

  @Override
  public void unsubscribe() {
    LoginInfo caller = conn.getCallerChain().caller();
    LoginInfo[] originators = conn.getCallerChain().originators();
    String user = caller.entity;
    System.out.println("cancelando inscrição de "
      + Utils.chain2str(originators, caller));
    subscribers.remove(user);
  }

}
