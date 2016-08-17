package tecgraf.openbus.core;

import java.security.interfaces.RSAPrivateKey;

import tecgraf.openbus.SharedAuthSecret;

/**
 * Informa��es de autentica��o de entidades.
 *
 * @author Tecgraf
 */
public class AuthArgs {

  /**
   * Enumera��o dos tipos de autentica��o suportados.
   *
   * @author Tecgraf
   */
  enum AuthMode {
    /** Autentica��o por senha */
    AuthByPassword,
    /** Autentica��o por certificado */
    AuthByPrivateKey,
    /** Autentica��o compartilhada */
    AuthBySharedSecret,
  }

  /** Modo de autentica��o */
  AuthMode mode;
  /** Entidade */
  String entity;
  /** Senha */
  byte[] password;
  /** Dom�nio */
  String domain;
  /** Chave privada */
  RSAPrivateKey privkey;
  /** Segredo do compartilhamento de login */
  SharedAuthSecret secret;

  /**
   * Construtor para realizar autentica��o por senha.
   *
   * @param entity Identificador da entidade a ser autenticada.
   * @param password Senha de autentica��o da entidade no barramento.
   */
  public AuthArgs(String entity, byte[] password, String domain) {
    if (entity == null || password == null || domain == null) {
      throw new IllegalArgumentException(
        "Entidade, senha e dom�nio devem ser diferentes de nulo.");
    }
    this.entity = entity;
    this.password = password;
    this.domain = domain;
    this.mode = AuthMode.AuthByPassword;
  }

  /**
   * Construtor para realizar autentica��o por chave privada.
   *
   * @param entity Identificador da entidade a ser autenticada.
   * @param privateKey Chave privada correspondente ao certificado
   *                   registrado, a ser utilizada na autentica��o.
   */
  public AuthArgs(String entity, RSAPrivateKey privateKey) {
    if (entity == null || privateKey == null) {
      throw new IllegalArgumentException(
        "Entidade e chave privada devem ser diferentes de nulo.");
    }
    this.entity = entity;
    this.privkey = privateKey;
    this.mode = AuthMode.AuthByPrivateKey;
  }

  /**
   * Construtor para realizar autentica��o compartilhada.
   *
   * @param secret Segredo para compartilhamento de autentica��o.
   */
  public AuthArgs(SharedAuthSecret secret) {
    if (secret == null) {
      throw new IllegalArgumentException("O segredo deve ser diferente de " +
        "nulo.");
    }
    this.secret = secret;
    this.mode = AuthMode.AuthBySharedSecret;
  }
}