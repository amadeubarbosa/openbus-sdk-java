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
   * Recupear o idenficiador do barramento em que o segredo pode ser utilizado.
   * 
   * @return o identificador.
   */
  String busid();

  /**
   * Método que cancela o segredo, tornando-o inutilizável.
   * <p>
   * Cancela o segredo caso esse ainda esteja ativo, de forma que ele não poderá
   * ser mais utilizado.
   */
  void cancel();

};
