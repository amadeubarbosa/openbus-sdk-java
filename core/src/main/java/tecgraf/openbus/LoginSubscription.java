package tecgraf.openbus;

import java.util.List;

import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidLogins;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;

/**
 * Representa uma inscrição de um observador de logins. Essa inscrição será
 * mantida no barramento pelo registro de logins do qual se originou,
 * utilizando a conexão que o originou, até que a aplicação remova-a ou
 * realize um logout proposital.
 *
 * Esta inscrição permite a configuração do conjunto logins observáveis, onde os
 * possíveis eventos gerados são definidos pela interface {@link LoginObserver}.
 *
 * @author Tecgraf
 */
public interface LoginSubscription {

  /**
   * Fornece o observador inscrito pela aplicação.
   * 
   * @return o observador.
   */
  LoginObserver observer();

  /**
   * Adiciona um login à lista de logins observados. A monitoração dos
   * logins pertencentes à lista será mantida pelo registro de logins local até
   * que haja um logout explícito, que o login seja removido da lista pela
   * aplicação ou que o observador seja removido pela aplicação.
   *
   * Essa operação incorre em uma chamada remota ao barramento e é bloqueante.
   *
   * Essa operação pode bloquear enquanto não houver um login ativo. Caso seja
   * interrompida, retornará {@code false} e se manterá interrompida.
   *
   * @param loginId Identificador do login a ser observado.
   * @return {@code True} caso o loginId especificado seja válido,
   * {@code false} caso contrário.
   * @throws ServiceFailure Caso haja algum problema inesperado ao executar a
   * ação no barramento.
   */
  boolean watchLogin(String loginId) throws ServiceFailure;

  /**
   * Remove um login da lista de logins observados. Caso o login informado não
   * esteja na lista de logins observados ou for um login inválido, essa
   * operação não terá efeito algum.
   *
   * Essa operação incorre em uma chamada remota ao barramento e é bloqueante.
   *
   * Essa operação pode bloquear enquanto não houver um login ativo. Caso seja
   * interrompida, retornará e se manterá interrompida.
   *
   * @param loginId Identificador do login a ser removido.
   * @throws ServiceFailure Caso haja algum problema inesperado ao executar a
   * ação no barramento.
   */
  void forgetLogin(String loginId) throws ServiceFailure;

  /**
   * Adiciona uma sequência de logins à lista de logins observados. A
   * monitoração dos logins pertencentes à lista será mantida pelo registro
   * de logins local até que haja um logout explícito, que a sequência (ou
   * parte dela) de logins seja removida da lista pela aplicação ou que o
   * observador seja removido pela aplicação.
   *
   * Essa operação incorre em uma chamada remota ao barramento e é bloqueante.
   *
   * Essa operação pode bloquear enquanto não houver um login ativo. Caso seja
   * interrompida, retornará e se manterá interrompida.
   *
   * @param loginIds Uma sequência de identificadores de logins a serem
   *        observados.
   * @throws InvalidLogins Caso um ou mais logins pertencentes à sequência
   * fornecida estejam inválidos no barramento. A exceção conterá os logins
   * definidos como inválidos.
   * @throws ServiceFailure Caso haja algum problema inesperado ao executar a
   * ação no barramento.
   */
  void watchLogins(List<String> loginIds) throws InvalidLogins, ServiceFailure;

  /**
   * Remove uma sequência de logins da lista de logins observados. Caso algum
   * dos logins informados não esteja na lista de logins observados ou for um
   * login inválido, essa operação não terá efeito algum para esse login
   * específico.
   *
   * Essa operação incorre em uma chamada remota ao barramento e é bloqueante.
   *
   * Essa operação pode bloquear enquanto não houver um login ativo. Caso seja
   * interrompida, retornará e se manterá interrompida.
   *
   * @param loginIds A sequência de identificadores de logins a serem removidos.
   * @throws ServiceFailure Caso haja algum problema inesperado ao executar a
   * ação no barramento.
   */
  void forgetLogins(List<String> loginIds) throws ServiceFailure;

  /**
   * Obtém a lista dos logins observados por esse observador.
   *
   * Essa operação incorre em uma chamada remota ao barramento e é bloqueante.
   *
   * Essa operação pode bloquear enquanto não houver um login ativo. Caso seja
   * interrompida, retornará uma lista vazia e se manterá interrompida.
   *
   * @return A lista de logins observados.
   */
  List<LoginInfo> watchedLogins();

  /**
   * Remove a inscrição desse observador localmente, fazendo com que mais
   * nenhum evento sobre os logins observados seja notificado através dele.
   * Caso seja necessário remover o observador do barramento, uma outra
   * thread será encarregada desse trabalho.
   *
   * Essa operação pode bloquear enquanto não houver um login ativo. Caso seja
   * interrompida, retornará e se manterá interrompida.
   */
  void remove();
}