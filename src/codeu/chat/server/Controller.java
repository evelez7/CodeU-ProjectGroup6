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
  public void addUserInterest(String name, Uuid owner) {

    final User foundOwner = model.userById().first(owner);
    final User foundUser = model.userByText().first(name);
    
    LOG.info("User: " + foundOwner.name);
    LOG.info("User ID: " + foundOwner.id);
    LOG.info("Interest (User): " + foundUser.name);
    LOG.info("Interest ID: " + foundUser.id);

    if(foundOwner.UserSet.contains(foundUser.id)) {
      LOG.info("ERROR: User already in interests.");
    }
    else {
      foundOwner.UserSet.add(foundUser.id);
      LOG.info(foundUser.name + " added to interests!");
    }
    
  }

  @Override
  public void removeUserInterest(String name, Uuid owner) {

    final User foundOwner = model.userById().first(owner);
    final User foundUser = model.userByText().first(name);
    
    LOG.info("User: " + foundOwner.name);
    LOG.info("User ID: " + foundOwner.id);
    LOG.info("Interest (User): " + foundUser.name);
    LOG.info("Interest ID: " + foundUser.id);

    if(foundOwner.UserSet.contains(foundUser.id)) {
      foundOwner.UserSet.remove(foundUser.id);
      LOG.info(foundUser.name + " removed from interests!");
    }
    else {
      LOG.info("ERROR: User not found in interests.");
    }
    
  }

  @Override
  public void addConversationInterest(String title, Uuid owner) {

    final User foundOwner = model.userById().first(owner);
    final ConversationHeader foundConversation = model.conversationByText().first(title);
    
    LOG.info("User: " + foundOwner.name);
    LOG.info("User ID: " + foundOwner.id);
    LOG.info("Interest (Conversation): " + foundConversation.title);
    LOG.info("Interest ID: " + foundConversation.id);

    if(foundOwner.ConvoSet.contains(foundConversation.id)) {
      LOG.info("ERROR: Conversation already in interests.");
    }
    else {
      foundOwner.ConvoSet.add(foundConversation.id);
      LOG.info(foundConversation.title + " added to interests!");
    }
  
  }

  @Override
  public void removeConversationInterest(String title, Uuid owner) {

    final User foundOwner = model.userById().first(owner);
    final ConversationHeader foundConversation = model.conversationByText().first(title);

    LOG.info("User: " + foundOwner.name);
    LOG.info("User ID: " + foundOwner.id);
    LOG.info("Interest (Conversation): " + foundConversation.title);
    LOG.info("Interest ID: " + foundConversation.id);

    if(foundOwner.ConvoSet.contains(foundConversation.id)) {
      foundOwner.ConvoSet.remove(foundConversation.id);
      LOG.info(foundConversation.title + " removed from interests!");
    }
    else {
      LOG.info("ERROR: Conversation not found in interests.");
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
