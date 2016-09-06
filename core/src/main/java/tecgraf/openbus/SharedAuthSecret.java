package tecgraf.openbus;

/**
 * Segredo para compartilhamento de autenticação.
 * <p>
 * Objeto que representa uma tentativa de compartilhamento de autenticação
 * através do compartilhamento de um segredo, que pode ser utilizado para
 * realizar uma autenticação junto ao barramento em nome da mesma entidade que
 * gerou e compartilhou o segredo.
 *
 * Cada segredo de autenticação compartilhada pertence a um único barramento e
 * só pode utilizado em uma única autenticação.
 */
public interface SharedAuthSecret {

  /**
   * Fornece o identificador do barramento em que o segredo pode ser utilizado.
   * 
   * @return O identificador.
   */
  String busid();

  /**
   * Método que cancela o segredo, tornando-o inutilizável.
   * <p>
   * Cancela o segredo caso ainda esteja ativo, de forma que não poderá mais
   * ser utilizado.
   */
  void cancel();
}
