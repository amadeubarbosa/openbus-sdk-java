/*
 * $Id: FTClientInterceptor.java 
 */
package tecgraf.openbus.interceptors;

import java.io.OutputStream;
import java.io.PrintWriter;

import openbusidl.acs.Credential;
import openbusidl.acs.CredentialHelper;
import openbusidl.acs.IAccessControlService;
import openbusidl.rs.IRegistryService;

import org.jacorb.orb.ParsedIOR;
import org.jacorb.orb.util.CorbaLoc;
import org.omg.CORBA.Any;
import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.ORB;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.TRANSIENT;
import org.omg.IOP.Codec;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.ForwardRequest;

import tecgraf.openbus.FaultToleranceManager;
import tecgraf.openbus.Openbus;
import tecgraf.openbus.exception.ACSUnavailableException;
import tecgraf.openbus.exception.CORBAException;
import tecgraf.openbus.exception.ServiceUnavailableException;
import tecgraf.openbus.util.Log;
import tecgraf.openbus.util.Utils;

/**
 * Implementa um interceptador "cliente" com tolerância a falhas
 * 
 * @author Tecgraf/PUC-Rio
 */
class FTClientInterceptor extends ClientInterceptor  {

	/**
	 *Gerencia a lista de replicas.
	 */
	private FaultToleranceManager ftManager;

/**
   * Constrói o interceptador.
   * 
   * @param codec codificador/decodificador
   */
  FTClientInterceptor(Codec codec) {
    super(codec);
    this.ftManager = FaultToleranceManager.getInstance();
  }

    /**
   * {@inheritDoc}
   */
  public void receive_exception(ClientRequestInfo ri) throws ForwardRequest {
	  Log.INTERCEPTORS.info("[receive_exception] TRATANDO EXCECAO ENVIADA DO SERVIDOR!");
     
	  String msg = "";
	  boolean fetch = false;
	  if (ri.received_exception_id().equals("IDL:omg.org/CORBA/TRANSIENT:1.0")) {
		  fetch = true;
	  }else if (ri.received_exception_id().equals("IDL:omg.org/CORBA/OBJECT_NOT_EXIST:1.0")) {
		  fetch = true;
	  }else if (ri.received_exception_id().equals("IDL:omg.org/CORBA/COMM_FAILURE:1.0")) {
		  fetch = true;
	  }else if (ri.received_exception_id().equals("IDL:omg.org/CORBA/TRANSIENT:1.0")) {
		  fetch = true;
	  }
	  Openbus bus = Openbus.getInstance();
	  ORB orb = bus.getORB();
	 
	  ParsedIOR pior = new ParsedIOR( (org.jacorb.orb.ORB) orb, orb.object_to_string( ri.target() ));
	  String key = CorbaLoc.parseKey( pior.get_object_key());
	  
	  if (key.equals(Utils.ACCESS_CONTROL_SERVICE_KEY) || 
			  key.equals(Utils.LEASE_PROVIDER_KEY) || 
			  key.equals(Utils.ICOMPONENT_KEY) || 
			  key.equals(Utils.FAULT_TOLERANT_KEY + "ACS")){
		  while (fetch){
			  if (ftManager.updateACSHostInUse()){
				  bus.setHost(ftManager.getACSHostInUse().getHostName());
				  bus.setPort(ftManager.getACSHostInUse().getHostPort());
				  try {
					  bus.fetchACS();
					  throw new ForwardRequest( bus.getAccessControlService() );
				  } catch (ACSUnavailableException e) {
					  fetch = true;
					  msg = e.getMessage();
				  } catch (CORBAException e) {
					  fetch = true;
					  msg = e.getMessage();
				  } catch (ServiceUnavailableException e) {
					  fetch = true;
					  msg = e.getMessage();
				  }
			  }else{
				  fetch = false;
			  }
		  }
		  System.out.println("[ACSUnavailableException] " + msg);
	  }else if (key.equals(Utils.REGISTRY_SERVICE_KEY) ) {
		  IRegistryService rs = bus.getAccessControlService().getRegistryService();
		  throw new ForwardRequest( rs );
	  }
  }

  /**
   * {@inheritDoc}
   */
  public void receive_other(ClientRequestInfo ri) {
	  Log.INTERCEPTORS.info("[receive_other] TRATANDO OUTRA RESPOSTA!");
      // TODO
  }
}
