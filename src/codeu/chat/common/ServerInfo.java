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
    try {
      this.version = Uuid.parse(SERVER_VERSION);
    } catch (Exception ex) {
      LOG.error(ex, "There was an error in retrieving the server version.");
    }
    try {
      this.startTime = Time.now();
    } catch (Exception ex) {
      LOG.error(ex, "There was an error in retrieving the current time.");
    }
  }
  public ServerInfo(UUid version, Time startTime) {
    this.version = version;
    this.startTime = startTime;
  }
  public ServerInfo(Uuid version) {
    this.version = version;
  }
  public ServerInfo(Time startTime) {
		this.startTime = startTime;
	}
}
