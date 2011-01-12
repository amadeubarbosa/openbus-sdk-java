package tecgraf.openbus.demo.eventSink;

import org.omg.CORBA.Object;

import scs.core.servant.ComponentContext;
import tecgraf.openbus.session_service.v1_06.SessionEvent;
import tecgraf.openbus.session_service.v1_06.SessionEventSinkPOA;

/**
 * Implementação dummy da faceta EventSink.
 * 
 */
public final class EventSinkImpl extends SessionEventSinkPOA {
  private ComponentContext context;

  /**
   * Construtor padrão.
   * 
   * @param context Contexto ao qual essa faceta pertence.
   */
  public EventSinkImpl(ComponentContext context) {
    this.context = context;
  }

  @Override
  public Object _get_component() {
    return this.context.getIComponent();
  }

  public void push(String arg0, SessionEvent arg1) {
    System.out.println("Evento Push recebido. Tipo: " + arg1.type);
  }

  public void disconnect(String arg0) {
    System.out.println("Evento Disconnect recebido.");
  }
}
