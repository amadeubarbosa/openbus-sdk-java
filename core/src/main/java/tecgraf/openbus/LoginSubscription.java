package tecgraf.openbus;

import java.util.List;

import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidLogins;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;

/**
 * Representa uma inscri��o de um observador de logins. Essa inscri��o ser�
 * mantida no barramento pelo registro de logins do qual se originou,
 * utilizando a conex�o que o originou, at� que a aplica��o remova-a ou
 * realize um logout proposital.
 *
 * Esta inscri��o permite a configura��o do conjunto logins observ�veis, onde os
 * poss�veis eventos gerados s�o definidos pela interface {@link LoginObserver}.
 *
 * @author Tecgraf
 */
public interface LoginSubscription {

  /**
   * Fornece o observador inscrito pela aplica��o.
   * 
   * @return o observador.
   */
  LoginObserver observer();

  /**
   * Adiciona um login � lista de logins observados. A monitora��o dos
   * logins pertencentes � lista ser� mantida pelo registro de logins local at�
   * que haja um logout expl�cito, que o login seja removido da lista pela
   * aplica��o ou que o observador seja removido pela aplica��o.
   *
   * Essa opera��o incorre em uma chamada remota ao barramento e � bloqueante.
   *
   * Essa opera��o pode bloquear enquanto n�o houver um login ativo. Caso seja
   * interrompida, retornar� {@code false} e se manter� interrompida.
   *
   * @param loginId Identificador do login a ser observado.
   * @return {@code True} caso o loginId especificado seja v�lido,
   * {@code false} caso contr�rio.
   * @throws ServiceFailure Caso haja algum problema inesperado ao executar a
   * a��o no barramento.
   */
  boolean watchLogin(String loginId) throws ServiceFailure;

  /**
   * Remove um login da lista de logins observados. Caso o login informado n�o
   * esteja na lista de logins observados ou for um login inv�lido, essa
   * opera��o n�o ter� efeito algum.
   *
   * Essa opera��o incorre em uma chamada remota ao barramento e � bloqueante.
   *
   * Essa opera��o pode bloquear enquanto n�o houver um login ativo. Caso seja
   * interrompida, retornar� e se manter� interrompida.
   *
   * @param loginId Identificador do login a ser removido.
   * @throws ServiceFailure Caso haja algum problema inesperado ao executar a
   * a��o no barramento.
   */
  void forgetLogin(String loginId) throws ServiceFailure;

  /**
   * Adiciona uma sequ�ncia de logins � lista de logins observados. A
   * monitora��o dos logins pertencentes � lista ser� mantida pelo registro
   * de logins local at� que haja um logout expl�cito, que a sequ�ncia (ou
   * parte dela) de logins seja removida da lista pela aplica��o ou que o
   * observador seja removido pela aplica��o.
   *
   * Essa opera��o incorre em uma chamada remota ao barramento e � bloqueante.
   *
   * Essa opera��o pode bloquear enquanto n�o houver um login ativo. Caso seja
   * interrompida, retornar� e se manter� interrompida.
   *
   * @param loginIds Uma sequ�ncia de identificadores de logins a serem
   *        observados.
   * @throws InvalidLogins Caso um ou mais logins pertencentes � sequ�ncia
   * fornecida estejam inv�lidos no barramento. A exce��o conter� os logins
   * definidos como inv�lidos.
   * @throws ServiceFailure Caso haja algum problema inesperado ao executar a
   * a��o no barramento.
   */
  void watchLogins(List<String> loginIds) throws InvalidLogins, ServiceFailure;

  /**
   * Remove uma sequ�ncia de logins da lista de logins observados. Caso algum
   * dos logins informados n�o esteja na lista de logins observados ou for um
   * login inv�lido, essa opera��o n�o ter� efeito algum para esse login
   * espec�fico.
   *
   * Essa opera��o incorre em uma chamada remota ao barramento e � bloqueante.
   *
   * Essa opera��o pode bloquear enquanto n�o houver um login ativo. Caso seja
   * interrompida, retornar� e se manter� interrompida.
   *
   * @param loginIds A sequ�ncia de identificadores de logins a serem removidos.
   * @throws ServiceFailure Caso haja algum problema inesperado ao executar a
   * a��o no barramento.
   */
  void forgetLogins(List<String> loginIds) throws ServiceFailure;

  /**
   * Obt�m a lista dos logins observados por esse observador.
   *
   * Essa opera��o incorre em uma chamada remota ao barramento e � bloqueante.
   *
   * Essa opera��o pode bloquear enquanto n�o houver um login ativo. Caso seja
   * interrompida, retornar� uma lista vazia e se manter� interrompida.
   *
   * @return A lista de logins observados.
   */
  List<LoginInfo> watchedLogins();

  /**
   * Remove a inscri��o desse observador localmente, fazendo com que mais
   * nenhum evento sobre os logins observados seja notificado atrav�s dele.
   * Caso seja necess�rio remover o observador do barramento, uma outra
   * thread ser� encarregada desse trabalho.
   *
   * Essa opera��o pode bloquear enquanto n�o houver um login ativo. Caso seja
   * interrompida, retornar� e se manter� interrompida.
   */
  void remove();
}