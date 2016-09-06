package tecgraf.openbus;

/**
 * Segredo para compartilhamento de autentica��o.
 * <p>
 * Objeto que representa uma tentativa de compartilhamento de autentica��o
 * atrav�s do compartilhamento de um segredo, que pode ser utilizado para
 * realizar uma autentica��o junto ao barramento em nome da mesma entidade que
 * gerou e compartilhou o segredo.
 *
 * Cada segredo de autentica��o compartilhada pertence a um �nico barramento e
 * s� pode utilizado em uma �nica autentica��o.
 */
public interface SharedAuthSecret {

  /**
   * Fornece o identificador do barramento em que o segredo pode ser utilizado.
   * 
   * @return O identificador.
   */
  String busid();

  /**
   * M�todo que cancela o segredo, tornando-o inutiliz�vel.
   * <p>
   * Cancela o segredo caso ainda esteja ativo, de forma que n�o poder� mais
   * ser utilizado.
   */
  void cancel();
}
