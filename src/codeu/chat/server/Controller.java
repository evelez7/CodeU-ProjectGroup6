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

import java.io.IOException;

import java.util.Collection;

import codeu.chat.common.BasicController;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.ConversationPayload;
import codeu.chat.common.Message;
import codeu.chat.common.RandomUuidGenerator;
import codeu.chat.common.RawController;
import codeu.chat.common.User;
import codeu.chat.util.Logger;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;

public final class Controller implements RawController, BasicController {

  private final static Logger.Log LOG = Logger.newLog(Controller.class);

  private final Model model;
  private final Uuid.Generator uuidGenerator;

  public Controller(Uuid serverId, Model model) {
    this.model = model;
    this.uuidGenerator = new RandomUuidGenerator(serverId, System.currentTimeMillis());
  }

  @Override
  public Message newMessage(Uuid author, Uuid conversation, String body) {
    return newMessage(createId(), author, conversation, body, Time.now());
  }

  @Override
  public User newUser(String name) {
    return newUser(createId(), name, Time.now());
  }

  @Override
  public ConversationHeader newConversation(String title, Uuid owner) {
    return newConversation(createId(), title, owner, Time.now());
  }

  @Override
  public Message newMessage(Uuid id, Uuid author, Uuid conversation, String body, Time creationTime) {

    final User foundUser = model.userById().first(author);
    final ConversationPayload foundConversation = model.conversationPayloadById().first(conversation);

    Message message = null;

    if (foundUser != null && foundConversation != null && isIdFree(id)) {

      message = new Message(id, Uuid.NULL, Uuid.NULL, creationTime, author, body);
      model.add(message);
      LOG.info("Message added: %s", message.id);

      // Find and update the previous "last" message so that it's "next" value
      // will point to the new message.

      if (Uuid.equals(foundConversation.lastMessage, Uuid.NULL)) {

        // The conversation has no messages in it, that's why the last message is NULL (the first
        // message should be NULL too. Since there is no last message, then it is not possible
        // to update the last message's "next" value.

      } else {
        final Message lastMessage = model.messageById().first(foundConversation.lastMessage);
        lastMessage.next = message.id;
      }

      // If the first message points to NULL it means that the conversation was empty and that
      // the first message should be set to the new message. Otherwise the message should
      // not change.

      foundConversation.firstMessage =
          Uuid.equals(foundConversation.firstMessage, Uuid.NULL) ?
          message.id :
          foundConversation.firstMessage;

      // Update the conversation to point to the new last message as it has changed.

      foundConversation.lastMessage = message.id;
    }

    return message;
  }

  @Override
  public User newUser(Uuid id, String name, Time creationTime) {

    User user = null;

    if (isIdFree(id)) {

      user = new User(id, name, creationTime);
      model.add(user);

      LOG.info(
          "newUser success (user.id=%s user.name=%s user.time=%s)",
          id,
          name,
          creationTime);

    } else {

      LOG.info(
          "newUser fail - id in use (user.id=%s user.name=%s user.time=%s)",
          id,
          name,
          creationTime);
    }

    return user;
  }

  @Override
  public ConversationHeader newConversation(Uuid id, String title, Uuid owner, Time creationTime) {

    final User foundOwner = model.userById().first(owner);

    ConversationHeader conversation = null;

    if (foundOwner != null && isIdFree(id)) {
      conversation = new ConversationHeader(id, owner, creationTime, title);
      model.add(conversation);
      LOG.info("Conversation added: " + id);
    }

    return conversation;
  }

  @Override
  public int addUserInterest(String name, Uuid owner) {

    final User foundOwner = model.userById().first(owner);
    final User foundUser = model.userByText().first(name);

    if(foundUser != null) {
      if(foundOwner.UserSet.contains(foundUser.id)) {
        LOG.info("ERROR: User already in interests.");
        return -1;
      } else {
        foundOwner.UserSet.add(foundUser.id);
        foundOwner.UserUpdateMap.put(foundUser.id, Time.now());
        LOG.info("User Interest added: " + foundUser.id);
        return 0;
      }
    } else {
      LOG.info("ERROR: User not found.");
      return -2;
    }
  }

  @Override
  public int removeUserInterest(String name, Uuid owner) {

    final User foundOwner = model.userById().first(owner);
    final User foundUser = model.userByText().first(name);

    if(foundUser != null) {
      if(foundOwner.UserSet.contains(foundUser.id)) {
        foundOwner.UserSet.remove(foundUser.id);
        foundOwner.UserUpdateMap.remove(foundUser.id);
        LOG.info("User Interest removed: " + foundUser.id);
        return 0;
      } else {
        LOG.info("ERROR: User not found in interests.");
        return -1;
      }
    } else {
      LOG.info("ERROR: User not found.");
      return -2;
    }
  }

  @Override
  public int addConversationInterest(String title, Uuid owner) {

    final User foundOwner = model.userById().first(owner);
    final ConversationHeader foundConversation = model.conversationByText().first(title);

    if(foundConversation != null) {
      if(foundOwner.ConvoSet.contains(foundConversation.id)) {
        LOG.info("ERROR: Conversation already in interests.");
        return -1;
      } else {
        foundOwner.ConvoSet.add(foundConversation.id);
        foundOwner.ConvoUpdateMap.put(foundConversation.id, Time.now());
        LOG.info("Conversation Interest added: " + foundConversation.id);
        return 0;
      }
    } else {
      LOG.info("ERROR: Conversation not found.");
      return -2;
    }
  }

  @Override
  public int removeConversationInterest(String title, Uuid owner) {

    final User foundOwner = model.userById().first(owner);
    final ConversationHeader foundConversation = model.conversationByText().first(title);

    if(foundConversation != null) {
      if(foundOwner.ConvoSet.contains(foundConversation.id)) {
        foundOwner.ConvoSet.remove(foundConversation.id);
        foundOwner.ConvoUpdateMap.remove(foundConversation.id);
        LOG.info("Conversation Interest removed: " + foundConversation.id);
        return 0;
      } else {
        LOG.info("ERROR: Conversation not found in interests.");
        return -1;
      }
    } else {
      LOG.info("ERROR: Conversation not found.");
      return -2;
    }
  }

  @Override
  public int addUserToConversation(String name, String title, Uuid currentUser) {



    final User foundUser = verifyUser(name);
    final ConversationHeader foundConversation = model.conversationByText().first(title);

    if (foundUser != null && foundConversation != null) {
       final int verificationResponse = verifyAddUserToConversation(foundUser, foundConversation, currentUser);

      switch(verificationResponse) {
        case 0:
          foundConversation.userCategory.put(foundUser.id, 1);
          LOG.info("User " + name + " added to the conversation.");
          return 0;
        case -1:
          LOG.info("ERROR: User is already in the conversation.");
          return -1;
        case -2:
          LOG.info("ERROR: User attempting command does not have permission to change.");
          return -2;
        }
      } else {
        LOG.info("ERROR: User or conversation not found.");
        return -3;
      }

      return -3;
    }

    private int verifyAddUserToConversation(User foundUser, ConversationHeader foundConversation, Uuid currentUser) {
      final int currentPermissionLevel = foundConversation.userCategory.get(currentUser);

      if (foundConversation.userCategory.containsKey(foundUser.id)) {
        return -1;
      } else if (currentPermissionLevel < 2){
        return -2;
      } else {
        return 0;
      }
    }

    public int changePermissionLevel(String name, String title, int permissionLevel, Uuid currentUser) {

      final User foundUser = verifyUser(name);
      final ConversationHeader foundConversation = model.conversationByText().first(title);

      if (foundUser != null && foundConversation != null ) {
        final int verificationResponse = verifyPermissionLevelChange(foundUser, foundConversation, permissionLevel, currentUser);

        switch (verificationResponse) {
          case 0:
            foundConversation.userCategory.put(foundUser.id, permissionLevel);
            LOG.info("Permission level of user " + name + " changed to " + permissionLevel +".");
            return 0;
          case -1:
            LOG.info("ERROR: Specified user already at permission level.");
            return -1;
          case -2:
            LOG.info("ERROR: User attempting command does not have permission to change.");
            return -2;
          case -3:
            LOG.info("ERROR: User not allowed to change own permissions.");
            return -3;
          default:
            break;
          }
        } else {
          LOG.info("ERROR: User or conversation not found.");
          return -4;
        }

        return -4;
      }

    private int verifyPermissionLevelChange(User foundUser, ConversationHeader foundConversation, int permissionLevel, Uuid currentUser) {
      final int currentPermissionLevel = foundConversation.userCategory.get(currentUser);

      if(foundUser.id.equals(currentUser)) {
        return -3;
      }

      if (foundConversation.userCategory.containsKey(foundUser.id)) {
        switch (currentPermissionLevel) {
          case 1:
            return -2;
          case 2:
            if (permissionLevel < currentPermissionLevel) {
              if (foundConversation.userCategory.get(foundUser.id) == permissionLevel) {
                return -1;
              } else {
                return 0;
              }
            } else {
              return -2;
            }
          case 3:
            if (permissionLevel < currentPermissionLevel) {
              if (foundConversation.userCategory.get(foundUser.id) == permissionLevel) {
                return -1;
              } else {
                return 0;
              }
            } else {
              return -2;
            }
          default:
            break;
          }
        }

        return -4;
      }

  private User verifyUser(String inputText) {
    if(inputText.length() == 11 && inputText.charAt(1) == '.') {
      try {
        return model.userById().first(Uuid.parse(inputText));
      } catch (IOException e) {
        return model.userByText().first(inputText);
      }
    } else {
      return model.userByText().first(inputText);
    }
  }

  private Uuid createId() {

    Uuid candidate;

    for (candidate = uuidGenerator.make();
         isIdInUse(candidate);
         candidate = uuidGenerator.make()) {

     // Assuming that "randomUuid" is actually well implemented, this
     // loop should never be needed, but just incase make sure that the
     // Uuid is not actually in use before returning it.

    }

    return candidate;
  }

  private boolean isIdInUse(Uuid id) {
    return model.messageById().first(id) != null ||
           model.conversationById().first(id) != null ||
           model.userById().first(id) != null;
  }

  private boolean isIdFree(Uuid id) { return !isIdInUse(id); }

}
