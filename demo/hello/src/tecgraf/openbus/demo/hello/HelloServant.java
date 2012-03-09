package tecgraf.openbus.demo.hello;

public final class HelloServant extends HelloPOA {
  @Override
  public void sayHello() {
    String entity = "";
    System.out.println(String.format("Hello from %s!", entity));
  }
}
