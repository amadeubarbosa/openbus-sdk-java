package tecgraf.openbus.interop.delegation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tecgraf.openbus.Openbus;
import tecgraf.openbus.core.v1_05.access_control_service.Credential;

public class BroadcasterImpl extends BroadcasterPOA {

  private List<String> subscribers;
  private Messenger messenger;

  public BroadcasterImpl(Messenger messenger) {
    this.subscribers = Collections.synchronizedList(new ArrayList<String>());
    this.messenger = messenger;
  }

  @Override
  public void post(String message) {
    Credential myCredential = Openbus.getInstance().getCredential();
    myCredential.delegate =
      Openbus.getInstance().getInterceptedCredential().owner;
    Openbus.getInstance().setThreadCredential(myCredential);
    synchronized (subscribers) {
      for (String user : subscribers) {
        messenger.post(user, message);
      }
    }
    myCredential.delegate = "";
  }

  @Override
  public void subscribe() {
    String user = Utils.getUser();
    System.out.println("inscrição de " + user);
    subscribers.add(user);
  }

  @Override
  public void unsubscribe() {
    String user = Utils.getUser();
    System.out.println("cancelando inscrição de " + user);
    subscribers.remove(user);
  }

}
