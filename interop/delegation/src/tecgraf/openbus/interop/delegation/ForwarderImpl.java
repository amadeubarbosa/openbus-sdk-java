package tecgraf.openbus.interop.delegation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import tecgraf.openbus.Openbus;
import tecgraf.openbus.core.v1_05.access_control_service.Credential;

public class ForwarderImpl extends ForwarderPOA {

  private Map<String, ForwardInfo> forwardsOf;

  public ForwarderImpl() {
    this.forwardsOf =
      Collections.synchronizedMap(new HashMap<String, ForwardInfo>());
  }

  @Override
  public void setForward(String to) {
    String user = Utils.getUser();
    System.out.println(String.format("configurando forward para '%s' por '%s'",
      to, user));
    this.forwardsOf.put(user, new ForwardInfo(user, to));
  }

  @Override
  public void cancelForward(String to) {
    String user = Utils.getUser();
    ForwardInfo forward = this.forwardsOf.remove(user);
    if (forward != null) {
      System.out.println(String.format("cancelando forward para '%s' por '%s'",
        forward.to, user));
    }
  }

  @Override
  public String getForward() throws NoForward {
    String user = Utils.getUser();
    ForwardInfo forward = this.forwardsOf.get(user);
    if (forward == null) {
      throw new NoForward();
    }
    return forward.to;
  }

  public Map<String, ForwardInfo> getForwardsOf() {
    return forwardsOf;
  }

  public static class ForwardInfo {
    public String user;
    public String to;

    public ForwardInfo(String user, String to) {
      this.user = user;
      this.to = to;
    }
  }

  public static class Timer extends Thread {

    private volatile boolean stop = false;
    private ForwarderImpl forwarder;
    private Messenger messenger;

    public Timer(ForwarderImpl forwarder, Messenger messenger) {
      this.forwarder = forwarder;
      this.messenger = messenger;
    }

    public void stopTimer() {
      this.stop = true;
    }

    @Override
    public void run() {
      while (!stop) {
        try {
          Thread.sleep(5000);
        }
        catch (InterruptedException e) {
          this.stop = true;
        }
        Credential myCredential = Openbus.getInstance().getCredential();
        Map<String, ForwardInfo> forwardsOf = forwarder.getForwardsOf();
        synchronized (forwardsOf) {
          for (Entry<String, ForwardInfo> entry : forwardsOf.entrySet()) {
            String user = entry.getKey();
            ForwardInfo info = entry.getValue();
            System.out.println("Verificando mensagens de " + user);
            myCredential.delegate = user;
            Openbus.getInstance().setThreadCredential(myCredential);
            PostDesc[] posts = messenger.receivePosts();
            myCredential.delegate = "";
            for (PostDesc post : posts) {
              messenger.post(info.to, String.format(
                "forwarded message by %s: %s", post.from, post.message));
            }
          }
        }
      }
    }
  }

}
