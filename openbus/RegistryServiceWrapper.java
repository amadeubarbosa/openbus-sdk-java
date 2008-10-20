/*
 * $Id$
 */
package openbus;

import openbus.exception.CORBAException;
import openbusidl.rs.IRegistryService;
import openbusidl.rs.Property;
import openbusidl.rs.ServiceOffer;

import org.omg.CORBA.StringHolder;
import org.omg.CORBA.SystemException;

/**
 * Encapsula o Servi�o de Registro.
 * 
 * @author Tecgraf/PUC-Rio
 */
public final class RegistryServiceWrapper {
  /**
   * O Servi�o de Registro real, encapsulado por este objeto.
   */
  private IRegistryService rs;

  /**
   * Cria um objeto que encapsula o Servi�o de Registro.
   */
  RegistryServiceWrapper() {
    // Nada a ser feito.
  }

  /**
   * Define o Servi�o de Registro real.
   * 
   * @param rs O Servi�o de Registro real.
   */
  void setRS(IRegistryService rs) {
    this.rs = rs;
  }

  /**
   * Registra uma oferta de servi�o.
   * 
   * @param offer A oferta.
   * 
   * @return O identificador do registro da oferta.
   * 
   * @throws CORBAException Caso ocorra alguma exce��o na infra-estrutura CORBA.
   */
  public String register(ServiceOffer offer) throws CORBAException {
    try {
      StringHolder offerIdentifier = new StringHolder();
      if (this.rs.register(offer, offerIdentifier)) {
        return offerIdentifier.value;
      }
      return null;
    }
    catch (SystemException e) {
      throw new CORBAException(e);
    }
  }

  /**
   * Retira o registro de uma oferta de servi�o.
   * 
   * @param offerIdentifier O identificador do registro da oferta.
   * 
   * @return {@code true} caso o registro seja retirado, ou {@code false}, caso
   *         contr�rio.
   * 
   * @throws CORBAException Caso ocorra alguma exce��o na infra-estrutura CORBA.
   */
  public boolean unregister(String offerIdentifier) throws CORBAException {
    try {
      return this.rs.unregister(offerIdentifier);
    }
    catch (SystemException e) {
      throw new CORBAException(e);
    }
  }

  /**
   * Atualiza as propriedades de uma oferta de servi�o.
   * 
   * @param offerIdentifier O identificador do registro da oferta.
   * @param newProperties As novas propriedades da oferta.
   * 
   * @return {@code true} caso o registro seja atualizado, ou {@code false},
   *         caso contr�rio.
   * 
   * @throws CORBAException Caso ocorra alguma exce��o na infra-estrutura CORBA.
   */
  public boolean update(String offerIdentifier, Property[] newProperties)
    throws CORBAException {
    try {
      return this.rs.update(offerIdentifier, newProperties);
    }
    catch (SystemException e) {
      throw new CORBAException(e);
    }
  }

  /**
   * Realiza uma busca por ofertas de servi�o atrav�s de determinados crit�rios.
   * 
   * @param criteria Os crit�rios.
   * 
   * @return As ofertas de servi�o encontradas.
   * 
   * @throws CORBAException Caso ocorra alguma exce��o na infra-estrutura CORBA.
   */
  public ServiceOffer[] find(Property[] criteria) throws CORBAException {
    try {
      return this.rs.find(criteria);
    }
    catch (SystemException e) {
      throw new CORBAException(e);
    }
  }
}
