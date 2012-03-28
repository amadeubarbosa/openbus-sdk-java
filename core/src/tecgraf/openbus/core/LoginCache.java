package tecgraf.openbus.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import tecgraf.openbus.core.v2_00.services.ServiceFailure;
import tecgraf.openbus.util.LRUCache;

class LoginCache {

  private static final Logger logger = Logger.getLogger(LoginCache.class
    .getName());

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
        logger.finest(String.format("Login é valido ainda na cache: %s ",
          loginId));
        return true;
      }
      else {
        logger.finest(String.format("Login esta na cache mas esta expirado",
          loginId));
      }
    }

    List<String> ids = new ArrayList<String>(this.logins.keySet());
    if (!contains) {
      logger.finest(String.format("Login não esta na cache", loginId));
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

  private class LoginEntry {
    public int validity;
    public long lastTime;
  }
}
