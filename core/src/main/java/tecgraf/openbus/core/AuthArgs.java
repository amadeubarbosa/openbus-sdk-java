package tecgraf.openbus.core;

import java.security.interfaces.RSAPrivateKey;

import tecgraf.openbus.SharedAuthSecret;

/**
 * Informações de autenticação de entidades.
 *
 * @author Tecgraf
 */
public class AuthArgs {

  /**
   * Enumeração dos tipos de autenticação suportados.
   *
   * @author Tecgraf
   */
  enum AuthMode {
    /** Autenticação por senha */
    AuthByPassword,
    /** Autenticação por certificado */
    AuthByPrivateKey,
    /** Autenticação compartilhada */
    AuthBySharedSecret,
  }

  /** Modo de autenticação */
  AuthMode mode;
  /** Entidade */
  String entity;
  /** Senha */
  byte[] password;
  /** Domínio */
  String domain;
  /** Chave privada */
  RSAPrivateKey privkey;
  /** Segredo do compartilhamento de login */
  SharedAuthSecret secret;

  /**
   * Construtor para realizar autenticação por senha.
   *
   * @param entity Identificador da entidade a ser autenticada.
   * @param password Senha de autenticação da entidade no barramento.
   */
  public AuthArgs(String entity, byte[] password, String domain) {
    if (entity == null || password == null || domain == null) {
      throw new IllegalArgumentException(
        "Entidade, senha e domínio devem ser diferentes de nulo.");
    }
    this.entity = entity;
    this.password = password;
    this.domain = domain;
    this.mode = AuthMode.AuthByPassword;
  }

  /**
   * Construtor para realizar autenticação por chave privada.
   *
   * @param entity Identificador da entidade a ser autenticada.
   * @param privateKey Chave privada correspondente ao certificado
   *                   registrado, a ser utilizada na autenticação.
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
   * Construtor para realizar autenticação compartilhada.
   *
   * @param secret Segredo para compartilhamento de autenticação.
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