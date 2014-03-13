package tecgraf.openbus.core;

import java.util.Properties;

import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;

import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidChainCode;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidRemoteCode;
import tecgraf.openbus.core.v2_0.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_0.services.access_control.UnknownBusCode;
import tecgraf.openbus.core.v2_0.services.access_control.UnverifiedLoginCode;

/**
 * Inicializador de {@link ORB}s para acesso a barramentos OpenBus.
 * <p>
 * Esse objeto � utilizado para obten��o de {@link ORB}s CORBA a ser utilizados
 * exclusimamente para chamadas atrav�s de barramentos OpenBus.
 * 
 * @author Tecgraf
 */
public class ORBInitializer {

  /**
   * Inicializa um {@link ORB} utilizado exclusivamente para chamadas atrav�s de
   * barramentos OpenBus.
   * <p>
   * Inicializa um {@link ORB} utilizado exclusivamente para chamadas atrav�s de
   * barramentos OpenBus, ou seja, esse ORB n�o pode ser utilizado para fazer
   * chamadas CORBA ordin�rias sem o controle de acesso do OpenBus que permite
   * identifica��o da origem das chamadas. Esse controle de acesso � feito
   * atrav�s conex�es, que s�o obtidas e manipuladas atrav�s de um
   * {@link OpenBusContext}. Cada ORB possui um OpenBusContext associado, que
   * pode ser obitido atrav�s do comando:
   * {@link ORB#resolve_initial_references(String)
   * resolve_initial_reference("OpenBusContext")}
   * <p>
   * O ORB � inicializado da mesma forma feita pela opera��o {@link ORB#init}
   * definida pelo padr�o CORBA. Em particular, algumas implementa��es de CORBA
   * n�o permitem inicializa��o de m�ltiplos ORBs num mesmo processo.
   * <p>
   * Chamadas realizadas e recebidas atrav�s deste ORB s�o interceptadas pela
   * biblioteca de acesso do OpenBus e podem lan�ar exce��es de sistema de CORBA
   * definidas pelo OpenBus. A seguir s�o apresentadas essas exce��es:
   * <ul>
   * <li>{@link NO_PERMISSION}[{@link NoLoginCode}]: Nenhuma conex�o "Requester"
   * com login v�lido est� associada ao contexto atual, ou seja, a conex�o
   * "Requester" corrente est� desautenticada.
   * <li>{@link NO_PERMISSION}[{@link InvalidChainCode}]: A cadeia de chamadas
   * associada ao contexto atual n�o � compat�vel com o login da conex�o
   * "Requester" desse mesmo contexto. Isso ocorre pois n�o � poss�vel fazer
   * chamadas dentro de uma cadeia recebida por uma conex�o com um login
   * diferente.
   * <li>{@link NO_PERMISSION}[{@link UnknownBusCode}]: O ORB remoto que recebeu
   * a chamada indicou que n�o possui uma conex�o com login v�lido no barramento
   * atrav�s do qual a chamada foi realizada, portanto n�o � capaz de validar a
   * chamada para que esta seja processada.
   * <li>{@link NO_PERMISSION}[{@link UnverifiedLoginCode}]: O ORB remoto que
   * recebeu a chamada indicou que n�o n�o � capaz de validar a chamada para que
   * esta seja processada. Isso indica que o lado remoto tem problemas de acesso
   * aos servi�os n�cleo do barramento.
   * <li>{@link NO_PERMISSION}[{@link InvalidRemoteCode}]: O ORB remoto que
   * recebeu a chamada n�o est� se comportando de acordo com o protocolo OpenBus
   * 2.0, o que indica que est� mal implementado e tipicamente representa um bug
   * no servidor sendo chamado ou um erro de implanta��o do barramento.
   * </ul>
   * 
   * @return O {@link ORB} inicializado, similar � opera��o {@link ORB#init}
   *         definida pelo padr�o CORBA.
   */
  public static ORB initORB() {
    return ORBInitializer.initORB(null, null);
  }

  /**
   * Inicializa um {@link ORB} utilizado exclusivamente para chamadas atrav�s de
   * barramentos OpenBus.
   * <p>
   * Inicializa um {@link ORB} utilizado exclusivamente para chamadas atrav�s de
   * barramentos OpenBus, ou seja, esse ORB n�o pode ser utilizado para fazer
   * chamadas CORBA ordin�rias sem o controle de acesso do OpenBus que permite
   * identifica��o da origem das chamadas. Esse controle de acesso � feito
   * atrav�s conex�es, que s�o obtidas e manipuladas atrav�s de um
   * {@link OpenBusContext}. Cada ORB possui um OpenBusContext associado, que
   * pode ser obitido atrav�s do comando:
   * {@link ORB#resolve_initial_references(String)
   * resolve_initial_reference("OpenBusContext")}
   * <p>
   * O ORB � inicializado da mesma forma feita pela opera��o {@link ORB#init}
   * definida pelo padr�o CORBA. Em particular, algumas implementa��es de CORBA
   * n�o permitem inicializa��o de m�ltiplos ORBs num mesmo processo.
   * <p>
   * Chamadas realizadas e recebidas atrav�s deste ORB s�o interceptadas pela
   * biblioteca de acesso do OpenBus e podem lan�ar exce��es de sistema de CORBA
   * definidas pelo OpenBus. A seguir s�o apresentadas essas exce��es:
   * <ul>
   * <li>{@link NO_PERMISSION}[{@link NoLoginCode}]: Nenhuma conex�o "Requester"
   * com login v�lido est� associada ao contexto atual, ou seja, a conex�o
   * "Requester" corrente est� desautenticada.
   * <li>{@link NO_PERMISSION}[{@link InvalidChainCode}]: A cadeia de chamadas
   * associada ao contexto atual n�o � compat�vel com o login da conex�o
   * "Requester" desse mesmo contexto. Isso ocorre pois n�o � poss�vel fazer
   * chamadas dentro de uma cadeia recebida por uma conex�o com um login
   * diferente.
   * <li>{@link NO_PERMISSION}[{@link UnknownBusCode}]: O ORB remoto que recebeu
   * a chamada indicou que n�o possui uma conex�o com login v�lido no barramento
   * atrav�s do qual a chamada foi realizada, portanto n�o � capaz de validar a
   * chamada para que esta seja processada.
   * <li>{@link NO_PERMISSION}[{@link UnverifiedLoginCode}]: O ORB remoto que
   * recebeu a chamada indicou que n�o n�o � capaz de validar a chamada para que
   * esta seja processada por alguma falha ao acessar os servi�os n�cleo do
   * barramento. Isso tipicamente indica que o lado remoto tem problemas de
   * acesso aos servi�os n�cleo do barramento.
   * <li>{@link NO_PERMISSION}[{@link InvalidRemoteCode}]: O ORB remoto que
   * recebeu a chamada n�o est� se comportando de acordo com o protocolo OpenBus
   * 2.0, o que indica que est� mal implementado e tipicamente representa um bug
   * no servidor sendo chamado ou um erro de implanta��o do barramento.
   * </ul>
   * 
   * @param args Par�metros usados na inicializa��o do {@link ORB}, similar �
   *        opera��o {@link ORB#init} definida pelo padr�o CORBA.
   * 
   * @return O {@link ORB} inicializado, similar � opera��o {@link ORB#init}
   *         definida pelo padr�o CORBA.
   */
  public static ORB initORB(String[] args) {
    return ORBInitializer.initORB(args, null);
  }

  /**
   * Inicializa um {@link ORB} utilizado exclusivamente para chamadas atrav�s de
   * barramentos OpenBus.
   * <p>
   * Inicializa um {@link ORB} utilizado exclusivamente para chamadas atrav�s de
   * barramentos OpenBus, ou seja, esse ORB n�o pode ser utilizado para fazer
   * chamadas CORBA ordin�rias sem o controle de acesso do OpenBus que permite
   * identifica��o da origem das chamadas. Esse controle de acesso � feito
   * atrav�s conex�es, que s�o obtidas e manipuladas atrav�s de um
   * {@link OpenBusContext}. Cada ORB possui um OpenBusContext associado, que
   * pode ser obitido atrav�s do comando:
   * {@link ORB#resolve_initial_references(String)
   * resolve_initial_reference("OpenBusContext")}
   * <p>
   * O ORB � inicializado da mesma forma feita pela opera��o {@link ORB#init}
   * definida pelo padr�o CORBA. Em particular, algumas implementa��es de CORBA
   * n�o permitem inicializa��o de m�ltiplos ORBs num mesmo processo.
   * <p>
   * Chamadas realizadas e recebidas atrav�s deste ORB s�o interceptadas pela
   * biblioteca de acesso do OpenBus e podem lan�ar exce��es de sistema de CORBA
   * definidas pelo OpenBus. A seguir s�o apresentadas essas exce��es:
   * <ul>
   * <li>{@link NO_PERMISSION}[{@link NoLoginCode}]: Nenhuma conex�o "Requester"
   * com login v�lido est� associada ao contexto atual, ou seja, a conex�o
   * "Requester" corrente est� desautenticada.
   * <li>{@link NO_PERMISSION}[{@link InvalidChainCode}]: A cadeia de chamadas
   * associada ao contexto atual n�o � compat�vel com o login da conex�o
   * "Requester" desse mesmo contexto. Isso ocorre pois n�o � poss�vel fazer
   * chamadas dentro de uma cadeia recebida por uma conex�o com um login
   * diferente.
   * <li>{@link NO_PERMISSION}[{@link UnknownBusCode}]: O ORB remoto que recebeu
   * a chamada indicou que n�o possui uma conex�o com login v�lido no barramento
   * atrav�s do qual a chamada foi realizada, portanto n�o � capaz de validar a
   * chamada para que esta seja processada.
   * <li>{@link NO_PERMISSION}[{@link UnverifiedLoginCode}]: O ORB remoto que
   * recebeu a chamada indicou que n�o n�o � capaz de validar a chamada para que
   * esta seja processada por alguma falha ao acessar os servi�os n�cleo do
   * barramento. Isso tipicamente indica que o lado remoto tem problemas de
   * acesso aos servi�os n�cleo do barramento.
   * <li>{@link NO_PERMISSION}[{@link InvalidRemoteCode}]: O ORB remoto que
   * recebeu a chamada n�o est� se comportando de acordo com o protocolo OpenBus
   * 2.0, o que indica que est� mal implementado e tipicamente representa um bug
   * no servidor sendo chamado ou um erro de implanta��o do barramento.
   * </ul>
   * 
   * @param args Par�metros usados na inicializa��o do {@link ORB}, similar �
   *        opera��o {@link ORB#init} definida pelo padr�o CORBA.
   * @param props Propriedades usados na inicializa��o do {@link ORB}, similar �
   *        opera��o {@link ORB#init} definida pelo padr�o CORBA.
   * 
   * @return O {@link ORB} inicializado, similar � opera��o {@link ORB#init}
   *         definida pelo padr�o CORBA.
   */
  public static ORB initORB(String[] args, Properties props) {
    ORB orb = createORB(args, props);
    ORBMediator mediator = ORBUtils.getMediator(orb);
    mediator.setORB(orb);
    OpenBusContextImpl context = ORBUtils.getOpenBusContext(orb);
    context.setORB(orb);
    return orb;
  }

  /**
   * Cria o ORB.
   * 
   * @param args argumentos
   * @param props propriedades
   * @return o ORB
   */
  private static ORB createORB(String[] args, Properties props) {
    ORBBuilder orbBuilder = new ORBBuilder(args, props);
    orbBuilder.addInitializer(new ORBInitializerInfo(
      InternalJacORBInitializer.class));
    return orbBuilder.build();
  }

}
