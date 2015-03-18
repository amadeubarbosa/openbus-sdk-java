package tecgraf.openbus.demo.util;


/**
 * Classe utilitária para os demos Java.
 * 
 * @author Tecgraf
 */
public class Usage {

  public static final String clientUsage =
    "Usage: 'demo' <host> <port> <entity> [password] [domain] %s\n"
      + "  - host = é o host do barramento\n"
      + "  - port = é a porta do barramento\n"
      + "  - entity = é a entidade a ser autenticada\n"
      + "  - password = senha (opcional)\n"
      + "  - domain = domínio de autenticação (opecional) %s";

  public static final String serverUsage =
    "Usage: 'demo' <host> <port> <entity> <privatekeypath> %s\n"
      + "  - host = é o host do barramento\n"
      + "  - port = é a porta do barramento\n"
      + "  - entity = é a entidade a ser autenticada\n"
      + "  - privatekeypath = é o caminho da chave privada de autenticação da entidade %s";

  public static final String port = "Valor de <port> deve ser um número";
  public static final String keypath =
    "<privatekeypath> deve apontar para uma chave válida.";

}
