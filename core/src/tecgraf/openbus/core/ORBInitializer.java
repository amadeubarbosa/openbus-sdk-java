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
 * Esse objeto é utilizado para obtenção de {@link ORB}s CORBA a ser utilizados
 * exclusimamente para chamadas através de barramentos OpenBus.
 * 
 * @author Tecgraf
 */
public class ORBInitializer {

  /**
   * Inicializa um {@link ORB} utilizado exclusivamente para chamadas através de
   * barramentos OpenBus.
   * <p>
   * Inicializa um {@link ORB} utilizado exclusivamente para chamadas através de
   * barramentos OpenBus, ou seja, esse ORB não pode ser utilizado para fazer
   * chamadas CORBA ordinárias sem o controle de acesso do OpenBus que permite
   * identificação da origem das chamadas. Esse controle de acesso é feito
   * através conexões, que são obtidas e manipuladas através de um
   * {@link OpenBusContext}. Cada ORB possui um OpenBusContext associado, que
   * pode ser obitido através do comando:
   * {@link ORB#resolve_initial_references(String)
   * resolve_initial_reference("OpenBusContext")}
   * <p>
   * O ORB é inicializado da mesma forma feita pela operação {@link ORB#init}
   * definida pelo padrão CORBA. Em particular, algumas implementações de CORBA
   * não permitem inicialização de múltiplos ORBs num mesmo processo.
   * <p>
   * Chamadas realizadas e recebidas através deste ORB são interceptadas pela
   * biblioteca de acesso do OpenBus e podem lançar exceções de sistema de CORBA
   * definidas pelo OpenBus. A seguir são apresentadas essas exceções:
   * <ul>
   * <li>{@link NO_PERMISSION}[{@link NoLoginCode}]: Nenhuma conexão "Requester"
   * com login válido está associada ao contexto atual, ou seja, a conexão
   * "Requester" corrente está desautenticada.
   * <li>{@link NO_PERMISSION}[{@link InvalidChainCode}]: A cadeia de chamadas
   * associada ao contexto atual não é compatível com o login da conexão
   * "Requester" desse mesmo contexto. Isso ocorre pois não é possível fazer
   * chamadas dentro de uma cadeia recebida por uma conexão com um login
   * diferente.
   * <li>{@link NO_PERMISSION}[{@link UnknownBusCode}]: O ORB remoto que recebeu
   * a chamada indicou que não possui uma conexão com login válido no barramento
   * através do qual a chamada foi realizada, portanto não é capaz de validar a
   * chamada para que esta seja processada.
   * <li>{@link NO_PERMISSION}[{@link UnverifiedLoginCode}]: O ORB remoto que
   * recebeu a chamada indicou que não não é capaz de validar a chamada para que
   * esta seja processada. Isso indica que o lado remoto tem problemas de acesso
   * aos serviços núcleo do barramento.
   * <li>{@link NO_PERMISSION}[{@link InvalidRemoteCode}]: O ORB remoto que
   * recebeu a chamada não está se comportando de acordo com o protocolo OpenBus
   * 2.0, o que indica que está mal implementado e tipicamente representa um bug
   * no servidor sendo chamado ou um erro de implantação do barramento.
   * </ul>
   * 
   * @return O {@link ORB} inicializado, similar à operação {@link ORB#init}
   *         definida pelo padrão CORBA.
   */
  public static ORB initORB() {
    return ORBInitializer.initORB(null, null);
  }

  /**
   * Inicializa um {@link ORB} utilizado exclusivamente para chamadas através de
   * barramentos OpenBus.
   * <p>
   * Inicializa um {@link ORB} utilizado exclusivamente para chamadas através de
   * barramentos OpenBus, ou seja, esse ORB não pode ser utilizado para fazer
   * chamadas CORBA ordinárias sem o controle de acesso do OpenBus que permite
   * identificação da origem das chamadas. Esse controle de acesso é feito
   * através conexões, que são obtidas e manipuladas através de um
   * {@link OpenBusContext}. Cada ORB possui um OpenBusContext associado, que
   * pode ser obitido através do comando:
   * {@link ORB#resolve_initial_references(String)
   * resolve_initial_reference("OpenBusContext")}
   * <p>
   * O ORB é inicializado da mesma forma feita pela operação {@link ORB#init}
   * definida pelo padrão CORBA. Em particular, algumas implementações de CORBA
   * não permitem inicialização de múltiplos ORBs num mesmo processo.
   * <p>
   * Chamadas realizadas e recebidas através deste ORB são interceptadas pela
   * biblioteca de acesso do OpenBus e podem lançar exceções de sistema de CORBA
   * definidas pelo OpenBus. A seguir são apresentadas essas exceções:
   * <ul>
   * <li>{@link NO_PERMISSION}[{@link NoLoginCode}]: Nenhuma conexão "Requester"
   * com login válido está associada ao contexto atual, ou seja, a conexão
   * "Requester" corrente está desautenticada.
   * <li>{@link NO_PERMISSION}[{@link InvalidChainCode}]: A cadeia de chamadas
   * associada ao contexto atual não é compatível com o login da conexão
   * "Requester" desse mesmo contexto. Isso ocorre pois não é possível fazer
   * chamadas dentro de uma cadeia recebida por uma conexão com um login
   * diferente.
   * <li>{@link NO_PERMISSION}[{@link UnknownBusCode}]: O ORB remoto que recebeu
   * a chamada indicou que não possui uma conexão com login válido no barramento
   * através do qual a chamada foi realizada, portanto não é capaz de validar a
   * chamada para que esta seja processada.
   * <li>{@link NO_PERMISSION}[{@link UnverifiedLoginCode}]: O ORB remoto que
   * recebeu a chamada indicou que não não é capaz de validar a chamada para que
   * esta seja processada por alguma falha ao acessar os serviços núcleo do
   * barramento. Isso tipicamente indica que o lado remoto tem problemas de
   * acesso aos serviços núcleo do barramento.
   * <li>{@link NO_PERMISSION}[{@link InvalidRemoteCode}]: O ORB remoto que
   * recebeu a chamada não está se comportando de acordo com o protocolo OpenBus
   * 2.0, o que indica que está mal implementado e tipicamente representa um bug
   * no servidor sendo chamado ou um erro de implantação do barramento.
   * </ul>
   * 
   * @param args Parâmetros usados na inicialização do {@link ORB}, similar à
   *        operação {@link ORB#init} definida pelo padrão CORBA.
   * 
   * @return O {@link ORB} inicializado, similar à operação {@link ORB#init}
   *         definida pelo padrão CORBA.
   */
  public static ORB initORB(String[] args) {
    return ORBInitializer.initORB(args, null);
  }

  /**
   * Inicializa um {@link ORB} utilizado exclusivamente para chamadas através de
   * barramentos OpenBus.
   * <p>
   * Inicializa um {@link ORB} utilizado exclusivamente para chamadas através de
   * barramentos OpenBus, ou seja, esse ORB não pode ser utilizado para fazer
   * chamadas CORBA ordinárias sem o controle de acesso do OpenBus que permite
   * identificação da origem das chamadas. Esse controle de acesso é feito
   * através conexões, que são obtidas e manipuladas através de um
   * {@link OpenBusContext}. Cada ORB possui um OpenBusContext associado, que
   * pode ser obitido através do comando:
   * {@link ORB#resolve_initial_references(String)
   * resolve_initial_reference("OpenBusContext")}
   * <p>
   * O ORB é inicializado da mesma forma feita pela operação {@link ORB#init}
   * definida pelo padrão CORBA. Em particular, algumas implementações de CORBA
   * não permitem inicialização de múltiplos ORBs num mesmo processo.
   * <p>
   * Chamadas realizadas e recebidas através deste ORB são interceptadas pela
   * biblioteca de acesso do OpenBus e podem lançar exceções de sistema de CORBA
   * definidas pelo OpenBus. A seguir são apresentadas essas exceções:
   * <ul>
   * <li>{@link NO_PERMISSION}[{@link NoLoginCode}]: Nenhuma conexão "Requester"
   * com login válido está associada ao contexto atual, ou seja, a conexão
   * "Requester" corrente está desautenticada.
   * <li>{@link NO_PERMISSION}[{@link InvalidChainCode}]: A cadeia de chamadas
   * associada ao contexto atual não é compatível com o login da conexão
   * "Requester" desse mesmo contexto. Isso ocorre pois não é possível fazer
   * chamadas dentro de uma cadeia recebida por uma conexão com um login
   * diferente.
   * <li>{@link NO_PERMISSION}[{@link UnknownBusCode}]: O ORB remoto que recebeu
   * a chamada indicou que não possui uma conexão com login válido no barramento
   * através do qual a chamada foi realizada, portanto não é capaz de validar a
   * chamada para que esta seja processada.
   * <li>{@link NO_PERMISSION}[{@link UnverifiedLoginCode}]: O ORB remoto que
   * recebeu a chamada indicou que não não é capaz de validar a chamada para que
   * esta seja processada por alguma falha ao acessar os serviços núcleo do
   * barramento. Isso tipicamente indica que o lado remoto tem problemas de
   * acesso aos serviços núcleo do barramento.
   * <li>{@link NO_PERMISSION}[{@link InvalidRemoteCode}]: O ORB remoto que
   * recebeu a chamada não está se comportando de acordo com o protocolo OpenBus
   * 2.0, o que indica que está mal implementado e tipicamente representa um bug
   * no servidor sendo chamado ou um erro de implantação do barramento.
   * </ul>
   * 
   * @param args Parâmetros usados na inicialização do {@link ORB}, similar à
   *        operação {@link ORB#init} definida pelo padrão CORBA.
   * @param props Propriedades usados na inicialização do {@link ORB}, similar à
   *        operação {@link ORB#init} definida pelo padrão CORBA.
   * 
   * @return O {@link ORB} inicializado, similar à operação {@link ORB#init}
   *         definida pelo padrão CORBA.
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
