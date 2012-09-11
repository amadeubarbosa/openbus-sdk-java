package tecgraf.openbus.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import tecgraf.openbus.core.v2_0.OctetSeqHolder;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidLogins;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;

/**
 * Cache de logins utilizado pelo interceptador cliente.
 * 
 * @author Tecgraf
 */
class LoginCache {

  /**
   * O mapa da cache de logins.
   */
  private Map<String, LoginEntry> logins;

  /**
   * Construtor.
   * 
   * @param cacheSize tamanho da cache.
   */
  LoginCache(int cacheSize) {
    this.logins =
      Collections.synchronizedMap(new LRUCache<String, LoginEntry>(cacheSize));
  }

  /**
   * Realiza a validação do Login.
   * 
   * @param loginId o login.
   * @param conn qual a conexão em uso.
   * @return <code>true</code> caso o login seja válido, e <code>false</code>
   *         caso contrário.
   * @throws ServiceFailure
   */
  boolean validateLogin(String loginId, ConnectionImpl conn)
    throws ServiceFailure {
    LoginEntry entry = this.logins.get(loginId);
    long time;
    if (entry != null) {
      synchronized (this.logins) {
        time = System.currentTimeMillis();
        Long elapsed = (time - entry.lastTime) / 1000;
        if (elapsed.intValue() <= entry.validity) {
          // login é valido
          return true;
        }
      }
    }

    String busid = conn.busid();
    List<String> ids = new ArrayList<String>();
    ids.add(loginId);
    time = System.currentTimeMillis();
    int validity = conn.logins().getLoginValidity(loginId);
    synchronized (this.logins) {
      LoginEntry loginEntry = this.logins.get(loginId);
      if (loginEntry == null) {
        loginEntry = new LoginEntry();
        loginEntry.loginId = loginId;
        loginEntry.busId = busid;
        loginEntry.lastTime = time;
        loginEntry.validity = validity;
        this.logins.put(loginId, loginEntry);
      }
      else {
        if (loginEntry.lastTime < time) {
          loginEntry.lastTime = time;
          loginEntry.validity = validity;
        }
        else {
          validity = loginEntry.validity;
        }
      }
    }
    return validity > 0;
  }

  /**
   * Recupera o nome da entidade do login.
   * 
   * @param loginId o login.
   * @param pubkey holder para a chave pública do login.
   * @param conn a conexão em uso.
   * @return O nome da entidade do login e a chave pública do mesmo atráves do
   *         holder de entrada pubkey.
   * @throws InvalidLogins
   * @throws ServiceFailure
   */
  String getLoginEntity(String loginId, OctetSeqHolder pubkey,
    ConnectionImpl conn) throws InvalidLogins, ServiceFailure {
    LoginEntry entry = this.logins.get(loginId);
    if (entry != null) {
      synchronized (this.logins) {
        if (entry.entity != null) {
          pubkey.value = entry.pubkey;
          return entry.entity;
        }
      }
    }
    LoginInfo info = conn.logins().getLoginInfo(loginId, pubkey);
    synchronized (this.logins) {
      entry = this.logins.get(loginId);
      if (entry == null) {
        entry = new LoginEntry();
        entry.busId = conn.busid();
        entry.loginId = info.id;
        entry.entity = info.entity;
        entry.pubkey = pubkey.value;
        entry.lastTime = System.currentTimeMillis();
        entry.validity = 0;
        this.logins.put(loginId, entry);
        return entry.entity;
      }
      else {
        entry.entity = info.entity;
        entry.pubkey = pubkey.value;
        return entry.entity;
      }
    }
  }

  /**
   * Valor do mapa de logins da cache.
   * 
   * @author Tecgraf
   */
  private class LoginEntry {
    /**
     * Identificação do login.
     */
    public String loginId;
    /**
     * Barramento ao qual o login pertence.
     */
    public String busId;
    /**
     * Tempo de validade.
     */
    public Integer validity;
    /**
     * Tempo em milisegundos de quando as informações foram atualizadas.
     */
    public Long lastTime;
    /**
     * Nome da entidade
     */
    public String entity;
    /**
     * Chave pública da entidade
     */
    public byte[] pubkey;
  }

  /**
   * Limpa a cache de logins.
   */
  public void clear() {
    this.logins.clear();
  }
}
