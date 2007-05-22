/*
 * $Id$
 */
package openbus.common.interceptors;

import openbus.common.CredentialManager;
import openbusidl.acs.Credential;
import openbusidl.acs.CredentialHelper;

import org.omg.IOP.Codec;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

/**
 * Implementa um interceptador "servidor", para obten��o de informa��es no
 * contexto de uma requisi��o.
 * 
 * @author Tecgraf/PUC-Rio
 */
public class ServerInterceptor extends org.omg.CORBA.LocalObject implements
  ServerRequestInterceptor {

  /**
   * Representa a identifica��o do "service context" (contexto) utilizado para
   * transporte de credenciais em requisi��es de servi�o.
   */
  private int contextId;

  /**
   * Representa o objeto respons�vel pelo marshall/unmarshall de credenciais
   * para transporte/obten��o de contextos de requisi��es de servico.
   */
  private Codec codec;

  /**
   * Constr�i o interceptador.
   * 
   * @param codec codificador/decodificador
   */
  public ServerInterceptor(Codec codec) {
    this.contextId = 1234;
    this.codec = codec;
  }

  /**
   * {@inheritDoc}
   */
  public String name() {
    return "ServerInterceptor";
  }

  /**
   * {@inheritDoc} <br>
   * Intercepta o request para obten��o de informa��o de contexto.
   */
  public void receive_request_service_contexts(ServerRequestInfo ri) {
    System.out.println("ATINGI PONTO DE INTERCEPTA��O SERVIDOR!");

    ServiceContext serviceContext;
    try {
      /* Verifica se h� informa��o de contexto (credencial) */
      serviceContext = ri.get_request_service_context(contextId);
      System.out.println("TEM CREDENCIAL!");
      byte[] value = serviceContext.context_data;
      Credential credential = CredentialHelper.extract(codec.decode_value(
        value, CredentialHelper.type()));
      System.out.println("CREDENCIAL: " + credential.identifier + ","
        + credential.entityName);

      /* Verifica se a credencial � v�lida */
      CredentialManager credentialManager = CredentialManager.getInstance();
      if (credentialManager.getACS().isValid(credential)) {
        System.out.println("CREDENCIAL VALIDADA!");

        /*
         * Insere o valor da credencial no slot alocado para seu transporte ao
         * tratador da requisi��o de servi�o
         */
        ri.set_slot(credentialManager.getCredentialSlot(), credentialManager
          .getCredentialValue(credential));
      }
      else {
        System.out.println("CREDENCIAL INVALIDA!");
        throw new org.omg.CORBA.NO_PERMISSION(0,
          org.omg.CORBA.CompletionStatus.COMPLETED_NO);
      }
    }
    catch (org.omg.CORBA.BAD_PARAM bp) {
      System.out.println("N�O H� CREDENCIAL!");
    }
    catch (org.omg.IOP.CodecPackage.FormatMismatch fm) {
      System.out.println("ERRO NO FORMATO DA CREDENCIAL!");
      fm.printStackTrace();
    }
    catch (org.omg.IOP.CodecPackage.TypeMismatch tm) {
      System.out.println("ERRO NO TIPO DA CREDENCIAL!");
      tm.printStackTrace();
    }
    catch (InvalidSlot is) {
      System.out.println("SLOT INVALIDO !!!!");
      is.printStackTrace();
    }
  }

  /**
   * {@inheritDoc}
   */
  public void receive_request(ServerRequestInfo ri) {
  }

  /**
   * {@inheritDoc}
   */
  public void send_reply(ServerRequestInfo ri) {
  }

  /**
   * {@inheritDoc}
   */
  public void send_exception(ServerRequestInfo ri) {
  }

  /**
   * {@inheritDoc}
   */
  public void send_other(ServerRequestInfo ri) {
  }

  /**
   * {@inheritDoc}
   */
  public void destroy() {
  }
}
