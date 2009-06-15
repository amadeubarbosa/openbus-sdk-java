/*
 * $Id$
 */
package openbus.common;

/**
 * Usada para informar que a renovação de um <i>lease</i> a partir do
 * {@link LeaseRenewer} falhou.
 * 
 * @author Tecgraf/PUC-Rio
 */
public interface LeaseExpiredCallback {
  /**
   * O <i>lease</i> expirou e não será mais renovado pelo {@link LeaseRenewer};
   */
  void expired();
}