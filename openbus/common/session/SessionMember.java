/*
 * $Id$
 */
package openbus.common.session;

import java.util.ArrayList;

import openbus.Registry;
import openbusidl.ss.SessionEventSinkPOA;

import org.omg.CORBA.UserException;
import org.omg.PortableServer.POA;

import scs.core.ComponentId;
import scs.core.FacetDescription;
import scs.core.servant.IComponentServant;

/**
 * Representa um componente membro de uma sessão.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class SessionMember extends IComponentServant {
  /**
   * O nome da faceta de um receptor de eventos de uma sessão.
   */
  private static final String EVENT_SINK_NAME = "eventSink";
  /**
   * O nome da interface de um receptor de eventos de uma sessão.
   */
  private static final String EVENT_SINK_INTERFACE = "IDL:openbusidl/ss/SessionEventSink:1.0";
  /**
   * O receptor de eventos da sessão.
   */
  private org.omg.CORBA.Object eventSink;

  /**
   * Cria um componente membro de uma sessão.
   * 
   * @param eventSink O receptor de eventos da sessão.
   * 
   * @throws UserException Caso ocorra algum erro na exportação do receptor de
   *         eventos.
   */
  public SessionMember(SessionEventSinkPOA eventSink) throws UserException {
    POA poa = Registry.getInstance().getPOA();
    this.eventSink = poa.servant_to_reference(eventSink);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected ComponentId createComponentId() {
    return new ComponentId(this.getClass().getName(), 1);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected ArrayList<FacetDescription> createFacets() {
    FacetDescription description = new FacetDescription(EVENT_SINK_NAME,
      EVENT_SINK_INTERFACE, this.eventSink);
    ArrayList<FacetDescription> facets = new ArrayList<FacetDescription>();
    facets.add(description);
    return facets;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean doStartup() {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean doShutdown() {
    return true;
  }
}