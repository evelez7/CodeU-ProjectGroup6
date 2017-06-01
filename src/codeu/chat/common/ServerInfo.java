package codeu.chat.common;

import codeu.chat.util.Logger;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;

// Holds information about server such as current version
public final class ServerInfo {
  private final static Logger.Log LOG = Logger.newLog(ServerInfo.class);
  private final static String SERVER_VERSION = "1.0.0";
  
  public final Time startTime;
  public Uuid version;
  
  public ServerInfo() {
    this.startTime = Time.now();
    try {
      this.version = Uuid.parse(SERVER_VERSION);
    } catch (Exception ex) {
      LOG.error(ex, "There was an error in retrieving the server version.");
    }
  }
  public ServerInfo(Uuid version, Time startTime) {
    this.version = version;
    this.startTime = startTime;
  }
}
