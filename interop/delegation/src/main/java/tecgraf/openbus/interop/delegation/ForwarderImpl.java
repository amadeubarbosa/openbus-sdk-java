package tecgraf.openbus.interop.delegation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;

public class ForwarderImpl extends ForwarderPOA {

  private OpenBusContext context;
  private final Map<String, ForwardInfo> forwardsOf;

  public ForwarderImpl(OpenBusContext context) {
    this.context = context;
    this.forwardsOf =
      Collections.synchronizedMap(new HashMap<>());
  }

  @Override
  public void setForward(String to) {
    CallerChain chain = context.getCallerChain();
    LoginInfo caller = chain.caller();
    String user = caller.entity;
    this.forwardsOf.put(user, new ForwardInfo(chain, to));
  }

  @Override
  public void cancelForward(String to) {
    LoginInfo caller = context.getCallerChain().caller();
    String user = caller.entity;
    this.forwardsOf.remove(user);
  }

  @Override
  public String getForward() throws NoForward {
    LoginInfo caller = context.getCallerChain().caller();
    String user = caller.entity;
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
    public CallerChain chain;
    public String to;

    public ForwardInfo(CallerChain chain, String to) {
      this.chain = chain;
      this.to = to;
    }
  }

  public static class Timer extends Thread {

    private volatile boolean stop = false;
    private final Map<String, ForwardInfo> forwardsOf;
    private OpenBusContext context;
    private Messenger messenger;

    public Timer(OpenBusContext context, ForwarderImpl forwarder,
      Messenger messenger) {
      this.forwardsOf = forwarder.getForwardsOf();
      this.messenger = messenger;
      this.context = context;
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
          stopTimer();
        }
        synchronized (forwardsOf) {
          for (ForwardInfo info : forwardsOf.values()) {
            context.joinChain(info.chain);
            PostDesc[] posts = messenger.receivePosts();
            context.exitChain();
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
