package codeu.chat.common;

import codeu.chat.common.BasicView;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.ConversationPayload;
import codeu.chat.common.Message;
import codeu.chat.common.NetworkCode;
import codeu.chat.common.User;
import codeu.chat.util.Logger;
import codeu.chat.util.Serializers;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;
import codeu.chat.util.connections.Connection;
import codeu.chat.util.connections.ConnectionSource;

public final class ServerInfo {
  private final static String SERVER_VERSION = "1.0.0";
  private final static Logger.Log LOG = Logger.newLog(ServerInfo.class);

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
