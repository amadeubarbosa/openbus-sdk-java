package tecgraf.openbus.assistant;

import tecgraf.openbus.core.OpenBusPrivateKey;
import tecgraf.openbus.core.v2_0.services.access_control.LoginProcess;

public class AuthArgs {

  enum AuthMode {
    AuthByPassword,
    AuthByCertificate,
    AuthBySharing;
  }

  AuthMode mode;
  String entity;
  byte[] password;
  OpenBusPrivateKey privkey;
  LoginProcess attempt;
  byte[] secret;

  public AuthArgs(String entity, byte[] password) {
    if (entity == null || password == null) {
      throw new IllegalArgumentException(
        "Entidade e senha devem ser diferentes de nulo.");
    }
    this.entity = entity;
    this.password = password;
    this.mode = AuthMode.AuthByPassword;
  }

  public AuthArgs(String entity, OpenBusPrivateKey privkey) {
    if (entity == null || privkey == null) {
      throw new IllegalArgumentException(
        "Entidade e chave privada devem ser diferentes de nulo.");
    }
    this.entity = entity;
    this.privkey = privkey;
    this.mode = AuthMode.AuthByCertificate;
  }

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
