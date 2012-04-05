package tecgraf.openbus.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import tecgraf.openbus.core.v2_00.OctetSeqHolder;
import tecgraf.openbus.core.v2_00.services.ServiceFailure;
import tecgraf.openbus.core.v2_00.services.access_control.InvalidLogins;
import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;
import tecgraf.openbus.util.LRUCache;

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
  synchronized boolean validateLogin(String loginId, ConnectionImpl conn)
    throws ServiceFailure {
    long time = System.currentTimeMillis();
    LoginEntry entry = this.logins.get(loginId);
    boolean contains = false;
    if (entry != null) {
      contains = true;
      Long elapsed = (time - entry.lastTime) / 1000;
      if (elapsed.intValue() <= entry.validity) {
        // login é valido
        return true;
      }
    }

    String busid = conn.busid();
    List<String> ids = new ArrayList<String>();
    List<LoginEntry> logins = new ArrayList<LoginEntry>(this.logins.values());
    for (LoginEntry lEntry : logins) {
      if (lEntry.busId.equals(busid)) {
        ids.add(lEntry.loginId);
      }
    }
    if (!contains) {
      ids.add(loginId);
    }
    time = System.currentTimeMillis();
    int[] validitys =
      conn.logins().getValidity(ids.toArray(new String[ids.size()]));

    boolean isValid = false;
    for (int i = 0; i < ids.size(); i++) {
      String id = ids.get(i);
      int validity = validitys[i];
      if (validity > 0) {
        LoginEntry loginEntry = this.logins.get(id);
        if (loginEntry == null) {
          loginEntry = new LoginEntry();
          loginEntry.loginId = id;
          loginEntry.busId = busid;
          loginEntry.lastTime = time;
          loginEntry.validity = validity;
          this.logins.put(id, loginEntry);
        }
        loginEntry.lastTime = time;
        loginEntry.validity = validity;
        if (id == loginId) {
          isValid = true;
        }
      }
      else {
        this.logins.remove(id);
      }
    }
    return isValid;
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
  synchronized String getLoginEntity(String loginId, OctetSeqHolder pubkey,
    ConnectionImpl conn) throws InvalidLogins, ServiceFailure {
    LoginEntry entry = this.logins.get(loginId);
    if (entry != null) {
      if (entry.entity != null) {
        pubkey.value = entry.pubkey;
        return entry.entity;
      }
      else {
        LoginInfo info = conn.logins().getLoginInfo(loginId, pubkey);
        entry.entity = info.entity;
        entry.pubkey = pubkey.value;
        return entry.entity;
      }
    }
    else {
      LoginInfo info = conn.logins().getLoginInfo(loginId, pubkey);
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
    public int validity;
    /**
     * Tempo em milisegundos de quando as informações foram atualizadas.
     */
    public long lastTime;
    /**
     * Nome da entidade
     */
    public String entity;
    /**
     * Chave pública da entidade
     */
    public byte[] pubkey;
  }
}
