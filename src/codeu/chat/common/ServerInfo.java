package codeu.chat.common;

import codeu.chat.util.Logger;
import codeu.chat.util.Uuid;

public final class ServerInfo {
  private final static Logger.Log LOG = Logger.newLog(ServerInfo.class);

  private final static String SERVER_VERSION = "1.0.0";

  public Uuid version;
  public ServerInfo() {
    try {
      this.version = Uuid.parse(SERVER_VERSION);
    } catch (Exception ex) {
      LOG.error(ex, "There was an error in retrieving the server version.");
    }
  }
  public ServerInfo(Uuid version) {
    this.version = version;
  }
}
