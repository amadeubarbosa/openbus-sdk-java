package tecgraf.openbus.core;

enum LoginEvent {
  /** Login feito ou refeito no barramento. */
  LOGGED_IN,
  /** Login desfeito. */
  LOGGED_OUT,
  /** Login refeito */
  RELOGIN,
}
