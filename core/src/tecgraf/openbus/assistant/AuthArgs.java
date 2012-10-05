package tecgraf.openbus.assistant;

import tecgraf.openbus.PrivateKey;
import tecgraf.openbus.core.v2_0.services.access_control.LoginProcess;

/**
 * Informa��es de autentica��o de entidades.
 * 
 * @author Tecgraf
 */
public class AuthArgs {

  /**
   * Enumera��o dos tipos de autentica��o suportados pelo assistente.
   * 
   * @author Tecgraf
   */
  enum AuthMode {
    /** Autentica��o por senha */
    AuthByPassword,
    /** Autentica��o por certificado */
    AuthByCertificate,
    /** Autentica��o compartilhada */
    AuthBySharing;
  }

  /** Modo de autentica��o */
  AuthMode mode;
  /** Entidade */
  String entity;
  /** Senha */
  byte[] password;
  /** Chave privada */
  PrivateKey privkey;
  /** Processo de compartilhamento de login */
  LoginProcess attempt;
  /** Segredo do compartilhamento de login */
  byte[] secret;

  /**
   * Construtor para realizar autentica��o por senha
   * 
   * @param entity Identificador da entidade a ser autenticada.
   * @param password Senha de autentica��o no barramento da entidade.
   */
  public AuthArgs(String entity, byte[] password) {
    if (entity == null || password == null) {
      throw new IllegalArgumentException(
        "Entidade e senha devem ser diferentes de nulo.");
    }
    this.entity = entity;
    this.password = password;
    this.mode = AuthMode.AuthByPassword;
  }

  /**
   * Construtor para realizar autentica��o por senha
   * 
   * @param entity Identificador da entidade a ser autenticada.
   * @param privkey Chave privada correspondente ao certificado registrado a ser
   *        utilizada na autentica��o.
   */
  public AuthArgs(String entity, PrivateKey privkey) {
    if (entity == null || privkey == null) {
      throw new IllegalArgumentException(
        "Entidade e chave privada devem ser diferentes de nulo.");
    }
    this.entity = entity;
    this.privkey = privkey;
    this.mode = AuthMode.AuthByCertificate;
  }

  /**
   * Construtor para realizar autentica��o compartilhada
   * 
   * @param attempt Objeto que represeta o processo de login iniciado.
   * @param secret Segredo a ser fornecido na conclus�o do processo de login.
   */
  public AuthArgs(LoginProcess attempt, byte[] secret) {
    if (attempt == null || secret == null) {
      throw new IllegalArgumentException(
        "Processo de login e segredo devem ser diferentes de nulo.");
    }
    this.attempt = attempt;
    this.secret = secret;
    this.mode = AuthMode.AuthBySharing;
  }
}
