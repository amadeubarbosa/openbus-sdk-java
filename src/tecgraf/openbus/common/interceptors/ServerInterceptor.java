/*
 * $Id$
 */
package tecgraf.openbus.common.interceptors;

import openbusidl.acs.Credential;
import openbusidl.acs.CredentialHelper;
import openbusidl.acs.IAccessControlService;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.IOP.Codec;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

import tecgraf.openbus.Openbus;
import tecgraf.openbus.common.Log;

/**
 * Implementa um interceptador "servidor", para obten��o de informa��es no
 * contexto de uma requisi��o.
 * 
 * @author Tecgraf/PUC-Rio
 */
class ServerInterceptor extends InterceptorImpl implements
  ServerRequestInterceptor {

  /**
   * Inst�ncia do ORB associado a este interceptador.
   */
  private ORB orb;

  /**
   * Inst�ncia do barramento associado a este ORB e interceptador.
   */
  private Openbus bus;

  /**
   * Wrapper para o servi�o de controle de acesso associado ao barramento.
   */
  private IAccessControlService acs;

  /**
   * O slot para transporte da credencial.
   */
  private int credentialSlot;

  /**
   * Constr�i o interceptador.
   * 
   * @param codec codificador/decodificador
   * @param credentialSlot O slot para transporte da credencial.
   */
  ServerInterceptor(Codec codec, int credentialSlot) {
    super("ServerInterceptor", codec);
    this.credentialSlot = credentialSlot;
  }

  /**
   * {@inheritDoc}
   */
  public void receive_request_service_contexts(ServerRequestInfo ri) {
    Log.INTERCEPTORS.fine("ATINGI PONTO DE INTERCEPTA��O SERVIDOR!");

    /*
     * Work around para o LocateRequest
     * 
     * Durante o bind com o servidor, o cliente Orbix envia uma mensagem GIOP
     * 1.2 LocateRequest para o servidor, que � uma otimiza��o corba para
     * localizar o objeto. Esta mensageme n�o passa pelo nosso interceptador
     * cliente e portanto a mensagem � envidada sem a credencial. O JacORB sabe
     * lidar com essa mensagen GIOP, por�m diferentemente do Orbix, ele passa
     * essa mensagem pelo interceptador do servidor, que por sua vez faz uma
     * verifica��o que falha por falta de credencial. Essa mensagem n�o deve ser
     * verificada.
     * 
     * Analisando o c�digo do JacORB, podemos ver que para uso interno, ele
     * define esse request como uma opera��o de nome "_non_existent". Ent�o no
     * interceptador do servidor JacORB n�s podemos ver esse request com a
     * opera��o com esse nome.
     * 
     * Logo para podermos responder adequadamente com um GIOP 1.2 LocateReply,
     * foi adicionado uma condi��o que inibe a verifica��o no caso de ser essa
     * opera��o interceptada.
     */
    if (ri.operation().equals("_non_existent"))
      return;

    /* Verifica se j� obteve o barramento */
    if (bus == null) {
      bus = Openbus.getInstance();
      orb = bus.getORB();
      acs = bus.getAccessControlService();
      bus.setInterceptedCredentialSlot(credentialSlot);
    }

    ServiceContext serviceContext;
    try {
      /* Verifica se h� informa��o de contexto (credencial) */
      serviceContext = ri.get_request_service_context(CONTEXT_ID);
      Log.INTERCEPTORS.fine("TEM CREDENCIAL!");
      byte[] value = serviceContext.context_data;
      Credential credential =
        CredentialHelper.extract(this.getCodec().decode_value(value,
          CredentialHelper.type()));
      Log.INTERCEPTORS.fine("CREDENCIAL: " + credential.identifier + ","
        + credential.owner);

      /* Verifica se a credencial � v�lida */
      if (acs.isValid(credential)) {
        Log.INTERCEPTORS.fine("CREDENCIAL VALIDADA!");

        /*
         * Insere o valor da credencial no slot alocado para seu transporte ao
         * tratador da requisi��o de servi�o
         */
        Any credentialValue = orb.create_any();
        CredentialHelper.insert(credentialValue, credential);
        ri.set_slot(this.credentialSlot, credentialValue);
      }
      else {
        Log.INTERCEPTORS.info("CREDENCIAL INVALIDA!");
        throw new org.omg.CORBA.NO_PERMISSION(0,
          org.omg.CORBA.CompletionStatus.COMPLETED_NO);
      }
    }
    catch (Exception e) {
      Log.INTERCEPTORS.severe("Falha na valida��o da credencial", e);
      throw new org.omg.CORBA.NO_PERMISSION(0,
        org.omg.CORBA.CompletionStatus.COMPLETED_NO);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void receive_request(ServerRequestInfo ri) {
    // Nada a ser feito.
  }

  /**
   * {@inheritDoc}
   */
  public void send_reply(ServerRequestInfo ri) {
    // Nada a ser feito.
  }

  /**
   * {@inheritDoc}
   */
  public void send_exception(ServerRequestInfo ri) {
    // Nada a ser feito.
  }

  /**
   * {@inheritDoc}
   */
  public void send_other(ServerRequestInfo ri) {
    // Nada a ser feito.
  }
}
