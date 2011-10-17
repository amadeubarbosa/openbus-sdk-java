package tecgraf.openbus.demo.hello;

public final class HelloServant extends IHelloPOA {
  @Override
  public void sayHello() {
    String entity = "";
    System.out.println(String.format("Hello from %s!", entity));
  }
}
