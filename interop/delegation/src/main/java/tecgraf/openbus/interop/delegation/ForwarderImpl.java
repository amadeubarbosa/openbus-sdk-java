package tecgraf.openbus.interop.delegation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.interop.util.Utils;

public class ForwarderImpl extends ForwarderPOA {

  private OpenBusContext context;
  private Map<String, ForwardInfo> forwardsOf;

  public ForwarderImpl(OpenBusContext context) {
    this.context = context;
    this.forwardsOf =
      Collections.synchronizedMap(new HashMap<String, ForwardInfo>());
  }

  @Override
  public void setForward(String to) {
    CallerChain chain = context.getCallerChain();
    LoginInfo caller = chain.caller();
    LoginInfo[] originators = chain.originators();
    String user = caller.entity;
    System.out.println(String.format("configurando forward para '%s' por '%s'",
      to, Utils.chain2str(originators, caller)));
    this.forwardsOf.put(user, new ForwardInfo(chain, to));
  }

  @Override
  public void cancelForward(String to) {
    LoginInfo caller = context.getCallerChain().caller();
    LoginInfo[] originators = context.getCallerChain().originators();
    String user = caller.entity;
    ForwardInfo forward = this.forwardsOf.remove(user);
    if (forward != null) {
      System.out.println(String.format("cancelando forward para '%s' por '%s'",
        forward.to, Utils.chain2str(originators, caller)));
    }
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
    private ForwarderImpl forwarder;
    private OpenBusContext context;
    private Messenger messenger;

    public Timer(OpenBusContext context, ForwarderImpl forwarder,
      Messenger messenger) {
      this.forwarder = forwarder;
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
          this.stop = true;
        }
        Map<String, ForwardInfo> forwardsOf = forwarder.getForwardsOf();
        synchronized (forwardsOf) {
          for (Entry<String, ForwardInfo> entry : forwardsOf.entrySet()) {
            String user = entry.getKey();
            ForwardInfo info = entry.getValue();
            System.out.println("Verificando mensagens de " + user);
            context.joinChain(info.chain);
            PostDesc[] posts = messenger.receivePosts();
            context.exitChain();
            for (PostDesc post : posts) {
              messenger.post(info.to, String.format(
                "forwarded message by %s:%s", post.from, post.message));
            }
          }
        }
      }
    }
  }

}
