package tecgraf.openbus.demo.util;


/**
 * Classe utilit�ria para os demos Java.
 * 
 * @author Tecgraf
 */
public class Usage {

  public static final String clientUsage =
    "Usage: 'demo' <host> <port> <entity> [password] [domain] %s\n"
      + "  - host = � o host do barramento\n"
      + "  - port = � a porta do barramento\n"
      + "  - entity = � a entidade a ser autenticada\n"
      + "  - password = senha (opcional)\n"
      + "  - domain = dom�nio de autentica��o (opecional) %s";

  public static final String serverUsage =
    "Usage: 'demo' <host> <port> <entity> <privatekeypath> %s\n"
      + "  - host = � o host do barramento\n"
      + "  - port = � a porta do barramento\n"
      + "  - entity = � a entidade a ser autenticada\n"
      + "  - privatekeypath = � o caminho da chave privada de autentica��o da entidade %s";

  public static final String port = "Valor de <port> deve ser um n�mero";
  public static final String keypath =
    "<privatekeypath> deve apontar para uma chave v�lida.";

}
