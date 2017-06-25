// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


package codeu.chat.server;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedList;
import java.util.ArrayList;

import codeu.chat.common.ConversationHeader;
import codeu.chat.common.ConversationPayload;
import codeu.chat.common.LinearUuidGenerator;
import codeu.chat.common.Message;
import codeu.chat.common.NetworkCode;
import codeu.chat.common.Relay;
import codeu.chat.common.Secret;
import codeu.chat.common.ServerInfo;
import codeu.chat.common.User;
import codeu.chat.util.Logger;
import codeu.chat.util.Serializers;
import codeu.chat.util.Time;
import codeu.chat.util.Timeline;
import codeu.chat.util.Uuid;
import codeu.chat.util.connections.Connection;

import com.google.gson.Gson;

public final class Server {

  private interface Command {
    void onMessage(InputStream in, OutputStream out) throws IOException;
  }

  private static final Logger.Log LOG = Logger.newLog(Server.class);

  private static final int RELAY_REFRESH_MS = 5000;  // 5 seconds
  private static final int SAVE_SERVER_MS = 30000;

  private static final ServerInfo info = new ServerInfo();

  private final Timeline timeline = new Timeline();

  private final Gson gson = new Gson();

  private final File dataStorage = new File("src\\codeu\\chat\\server\\JsonData.txt");

  private FileWriter fileWriter = null;
  private BufferedWriter bufferedWriter = null;
  private FileReader fileReader = null;
  private BufferedReader bufferedReader = null;

  private ArrayList<Message> messageStorage = new ArrayList<>();
  private ArrayList<ConversationHeader> conversationHeaderStorage = new ArrayList<>();
  private ArrayList<User> userStorage = new ArrayList<>();

  private LinkedList<String> dataQueue = new LinkedList<>();

  private final Map<Integer, Command> commands = new HashMap<>();

  private final Uuid id;
  private final Secret secret;

  private final Model model = new Model();
  private final View view = new View(model);
  private final Controller controller;

  private final Relay relay;
  private Uuid lastSeen = Uuid.NULL;

  public Server(final Uuid id, final Secret secret, final Relay relay) {

    this.id = id;
    this.secret = secret;
    this.controller = new Controller(id, model);
    this.relay = relay;

    // New Message - A client wants to add a new message to the back end.
    this.commands.put(NetworkCode.NEW_MESSAGE_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final Uuid author = Uuid.SERIALIZER.read(in);
        final Uuid conversation = Uuid.SERIALIZER.read(in);
        final String content = Serializers.STRING.read(in);

        final Message message = controller.newMessage(author, conversation, content);

        // Append an "identifier" before the object itself so that the reader can
        // split, identify, and create the proper object later
        final String stringMessage = "Message;" + gson.toJson(message);
        dataQueue.add(stringMessage);

        Serializers.INTEGER.write(out, NetworkCode.NEW_MESSAGE_RESPONSE);
        Serializers.nullable(Message.SERIALIZER).write(out, message);

        timeline.scheduleNow(createSendToRelayEvent(
            author,
            conversation,
            message.id));
      }
    });

    // New User - A client wants to add a new user to the back end.
    this.commands.put(NetworkCode.NEW_USER_REQUEST,  new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final String name = Serializers.STRING.read(in);
        final User user = controller.newUser(name);

        // Append an "identifier" before the object itself so that the reader can
        // split, identify, and create the proper object later
        final String stringUser = "User;" + gson.toJson(user);
        dataQueue.add(stringUser);

        Serializers.INTEGER.write(out, NetworkCode.NEW_USER_RESPONSE);
        Serializers.nullable(User.SERIALIZER).write(out, user);
      }
    });

    // New Conversation - A client wants to add a new conversation to the back end.
    this.commands.put(NetworkCode.NEW_CONVERSATION_REQUEST,  new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final String title = Serializers.STRING.read(in);
        final Uuid owner = Uuid.SERIALIZER.read(in);
        final ConversationHeader conversation = controller.newConversation(title, owner);

        // Append an "identifier" before the object itself so that the reader can
        // split, identify, and create the proper object later
        final String stringConvo = "Convo;" + gson.toJson(conversation);
        dataQueue.add(stringConvo);

        Serializers.INTEGER.write(out, NetworkCode.NEW_CONVERSATION_RESPONSE);
        Serializers.nullable(ConversationHeader.SERIALIZER).write(out, conversation);
      }
    });

    // Get Users - A client wants to get all the users from the back end.
    this.commands.put(NetworkCode.GET_USERS_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final Collection<User> users = view.getUsers();

        Serializers.INTEGER.write(out, NetworkCode.GET_USERS_RESPONSE);
        Serializers.collection(User.SERIALIZER).write(out, users);
      }
    });

    // Get Conversations - A client wants to get all the conversations from the back end.
    this.commands.put(NetworkCode.GET_ALL_CONVERSATIONS_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final Collection<ConversationHeader> conversations = view.getConversations();

        Serializers.INTEGER.write(out, NetworkCode.GET_ALL_CONVERSATIONS_RESPONSE);
        Serializers.collection(ConversationHeader.SERIALIZER).write(out, conversations);
      }
    });

    // Get Conversations By Id - A client wants to get a subset of the converations from
    //                           the back end. Normally this will be done after calling
    //                           Get Conversations to get all the headers and now the client
    //                           wants to get a subset of the payloads.
    this.commands.put(NetworkCode.GET_CONVERSATIONS_BY_ID_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final Collection<Uuid> ids = Serializers.collection(Uuid.SERIALIZER).read(in);
        final Collection<ConversationPayload> conversations = view.getConversationPayloads(ids);

        Serializers.INTEGER.write(out, NetworkCode.GET_CONVERSATIONS_BY_ID_RESPONSE);
        Serializers.collection(ConversationPayload.SERIALIZER).write(out, conversations);
      }
    });

    // Get Messages By Id - A client wants to get a subset of the messages from the back end.
    this.commands.put(NetworkCode.GET_MESSAGES_BY_ID_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final Collection<Uuid> ids = Serializers.collection(Uuid.SERIALIZER).read(in);
        final Collection<Message> messages = view.getMessages(ids);

        Serializers.INTEGER.write(out, NetworkCode.GET_MESSAGES_BY_ID_RESPONSE);
        Serializers.collection(Message.SERIALIZER).write(out, messages);
      }
    });

    // Get Server Info - A client wants to get server info from the back end
    this.commands.put (NetworkCode.SERVER_INFO_REQUEST, new Command() {
      @Override
      public void onMessage (InputStream in, OutputStream out) throws IOException {
        Serializers.INTEGER.write (out, NetworkCode.SERVER_INFO_RESPONSE);
        Uuid.SERIALIZER.write(out, info.version);
        Time.SERIALIZER.write(out, info.startTime);
      }
    });

    this.timeline.scheduleNow(new Runnable() {
      @Override
      public void run() {
        try {

          LOG.info("Reading update from relay...");

          for (final Relay.Bundle bundle : relay.read(id, secret, lastSeen, 32)) {
            onBundle(bundle);
            lastSeen = bundle.id();
          }

        } catch (Exception ex) {

          LOG.error(ex, "Failed to read update from relay.");

        }

        timeline.scheduleIn(RELAY_REFRESH_MS, this);
      }
    });
  }

  // Planning to call in ServerMain after Server is created
  public void saveServer() {
    timeline.scheduleIn(SAVE_SERVER_MS, new Runnable() {
      @Override
      public void run() {
        try {
          fileWriter = new FileWriter(dataStorage);
          bufferedWriter = new BufferedWriter(fileWriter);

          LOG.info("Writing sever content.");

          for (int i = 0; i < dataQueue.size(); i++) {
            // Ends up looking like: <Identifier>;<JSON Object> on every line
            bufferedWriter.write(dataQueue.get(i));
            bufferedWriter.newLine();
          }
        } catch (IOException ex) {
          LOG.error(ex, "There was an exception while writing server content.");
        } finally {
          try {
            if (bufferedWriter != null) {
              bufferedWriter.close();
            }

            if (fileWriter != null) {
              fileWriter.close();
            }
          } catch (IOException ex) {
            LOG.error(ex, "There was an exception while closing writers.");
          }
        }

        timeline.scheduleIn(SAVE_SERVER_MS, this);
      }
    });
  }

  // Planning to call upon server creation
  public void restoreServer() {
    timeline.scheduleNow(new Runnable() {
      @Override
      public void run() {
        try {
          LOG.info("Restoring server content");
          fileReader = new FileReader(dataStorage);
          bufferedReader = new BufferedReader(fileReader);

          String currentLine;

          while ((currentLine = bufferedReader.readLine()) != null) {
            // Split identifier from JSON and put both elements into an array
            String[] lineElements = currentLine.split(";");
            LOG.info(lineElements[0]);

            // lineElements[0] will contain the proper identifier (Convo, Message, User)
            // Then, according to the identifier, the proper object will be created
            // and restored
            switch (lineElements[0]) {
              case "User":
                User loadUser = gson.fromJson(lineElements[1], User.class);
                model.add(loadUser);
                break;
              case "Convo":
                ConversationHeader loadConvo = gson.fromJson(lineElements[1], ConversationHeader.class);
                model.add(loadConvo);
                break;
              case "Message":
                Message loadMessage = gson.fromJson(lineElements[1], Message.class);
                model.add(loadMessage);
                break;
              default:
                break;
            }
          }
        } catch (IOException ex) {
          LOG.error(ex, "There was an exception while restoring server content.");
        } finally {
          try {
              if (bufferedReader != null) {
                bufferedReader.close();
              }

              if (fileReader != null) {
                fileReader.close();
              }
            } catch (IOException ex) {
              LOG.error(ex, "There was an exception while closing readers.");
          }
        }
        LOG.info("End restoring server.");
      }
    });
  }

  public void handleConnection(final Connection connection) {
    timeline.scheduleNow(new Runnable() {
      @Override
      public void run() {
        try {

          LOG.info("Handling connection...");

          final int type = Serializers.INTEGER.read(connection.in());
          final Command command = commands.get(type);

          if (command == null) {
            // The message type cannot be handled so return a dummy message.
            Serializers.INTEGER.write(connection.out(), NetworkCode.NO_MESSAGE);
            LOG.info("Connection rejected");
          } else {
            command.onMessage(connection.in(), connection.out());
            LOG.info("Connection accepted");
          }

        } catch (Exception ex) {

          LOG.error(ex, "Exception while handling connection.");

        }

        try {
          connection.close();
        } catch (Exception ex) {
          LOG.error(ex, "Exception while closing connection.");
        }
      }
    });
  }

  private void onBundle(Relay.Bundle bundle) {

    final Relay.Bundle.Component relayUser = bundle.user();
    final Relay.Bundle.Component relayConversation = bundle.conversation();
    final Relay.Bundle.Component relayMessage = bundle.user();

    User user = model.userById().first(relayUser.id());

    if (user == null) {
      user = controller.newUser(relayUser.id(), relayUser.text(), relayUser.time());
    }

    ConversationHeader conversation = model.conversationById().first(relayConversation.id());

    if (conversation == null) {

      // As the relay does not tell us who made the conversation - the first person who
      // has a message in the conversation will get ownership over this server's copy
      // of the conversation.
      conversation = controller.newConversation(relayConversation.id(),
                                                relayConversation.text(),
                                                user.id,
                                                relayConversation.time());
    }

    Message message = model.messageById().first(relayMessage.id());

    if (message == null) {
      message = controller.newMessage(relayMessage.id(),
                                      user.id,
                                      conversation.id,
                                      relayMessage.text(),
                                      relayMessage.time());
    }
  }

  private Runnable createSendToRelayEvent(final Uuid userId,
                                          final Uuid conversationId,
                                          final Uuid messageId) {
    return new Runnable() {
      @Override
      public void run() {
        final User user = view.findUser(userId);
        final ConversationHeader conversation = view.findConversation(conversationId);
        final Message message = view.findMessage(messageId);
        relay.write(id,
                    secret,
                    relay.pack(user.id, user.name, user.creation),
                    relay.pack(conversation.id, conversation.title, conversation.creation),
                    relay.pack(message.id, message.content, message.creation));
      }
    };
  }
}
