package tecgraf.openbus.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.NO_PERMISSION;

import tecgraf.openbus.core.v2_00.OctetSeqHolder;
import tecgraf.openbus.core.v2_00.services.ServiceFailure;
import tecgraf.openbus.core.v2_00.services.access_control.InvalidLogins;
import tecgraf.openbus.core.v2_00.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_00.services.access_control.UnverifiedLoginCode;
import tecgraf.openbus.util.LRUCache;

/**
 * Cache de logins utilizado pelo interceptador cliente.
 * 
 * @author Tecgraf
 */
class LoginCache {

  private Map<String, LoginEntry> logins;

  LoginCache(int cacheSize) {
    this.logins =
      Collections.synchronizedMap(new LRUCache<String, LoginEntry>(cacheSize));
  }

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

    List<String> ids = new ArrayList<String>(this.logins.keySet());
    if (!contains) {
      ids.add(loginId);
    }
    time = System.currentTimeMillis();
    int[] validitys =
      conn.logins().getValidity(ids.toArray(new String[ids.size()]));
    int i = 0;
    boolean isValid = false;
    for (String id : ids) {
      int validity = validitys[i];
      if (validity > 0) {
        LoginEntry loginEntry = this.logins.get(id);
        if (loginEntry == null) {
          loginEntry = new LoginEntry();
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

  synchronized String getLoginEntity(String loginId, OctetSeqHolder pubkey,
    ConnectionImpl conn) throws InvalidLogins, ServiceFailure {
    try {
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
      throw new NO_PERMISSION(UnverifiedLoginCode.value,
        CompletionStatus.COMPLETED_NO);
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  /**
   * Valor do mapa de logins da cache.
   * 
   * @author Tecgraf
   */
  private class LoginEntry {
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
