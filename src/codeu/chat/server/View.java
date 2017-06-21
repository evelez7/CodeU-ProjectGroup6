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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import codeu.chat.common.BasicView;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.ConversationPayload;
import codeu.chat.common.Message;
import codeu.chat.common.SinglesView;
import codeu.chat.common.User;
import codeu.chat.util.Logger;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;
import codeu.chat.util.store.StoreAccessor;

public final class View implements BasicView, SinglesView {

  private final static Logger.Log LOG = Logger.newLog(View.class);

  private final Model model;

  public View(Model model) {
    this.model = model;
  }


  @Override
  public Collection<User> getUsers() {
    return all(model.userById());
  }

  @Override
  public Collection<ConversationHeader> getConversations() {
    return all(model.conversationById());
  }

  @Override
  public Collection<ConversationPayload> getConversationPayloads(Collection<Uuid> ids) {
    return intersect(model.conversationPayloadById(), ids);
  }

  @Override
  public Collection<Message> getMessages(Collection<Uuid> ids) {
    return intersect(model.messageById(), ids);
  }

  @Override
  public User findUser(Uuid id) { return model.userById().first(id); }

  @Override
  public ConversationHeader findConversation(Uuid id) { return model.conversationById().first(id); }

  @Override
  public Message findMessage(Uuid id) { return model.messageById().first(id); }

  @Override
  public Collection<String> userStatusUpdate(String name, Uuid owner) {

    //placeholder

    Collection<ConversationPayload> allConversations = all(model.conversationPayloadById());
    Collection<String> contributions = new ArrayList<String>();

    final User foundOwner = model.userById().first(owner);
    final User foundUser = model.userByText().first(name);
    final Time lastUpdate = foundOwner.UserUpdateMap.get(foundUser.id);

    for(ConversationPayload conversationPayload : allConversations) {
      Message currentMessage = model.messageById().first(conversationPayload.firstMessage);
      boolean foundMessage = false;
      while(currentMessage != null && foundMessage == false) {
        if(lastUpdate.compareTo(currentMessage.creation) < 0 && currentMessage.author.equals(foundUser.id)) {
          contributions.add(model.conversationById().first(conversationPayload.id).title);
          foundMessage = true;
        }
        currentMessage = model.messageById().first(currentMessage.next);
      }
      foundOwner.UserUpdateMap.put(foundUser.id, Time.now());
    }

    if(contributions.isEmpty()) {
      contributions.add("(No recent conversations)");
    }
    return contributions;

  }

  @Override
  public int conversationStatusUpdate(String title, Uuid owner) {

    int newMessages = 0;

    final User foundOwner = model.userById().first(owner);
    final ConversationHeader foundConversation = model.conversationByText().first(title);
    final ConversationPayload foundConversationPayload = model.conversationPayloadById().first(foundConversation.id);
    final Time lastUpdate = foundOwner.ConvoUpdateMap.get(foundConversation.id);

    if(foundOwner.ConvoSet.contains(foundConversation.id)) {
        Message currentMessage = model.messageById().first(foundConversationPayload.firstMessage);
        while(currentMessage != null) {
          if(lastUpdate.compareTo(currentMessage.creation) < 0) {
            newMessages++;
          }
          currentMessage = model.messageById().first(currentMessage.next);
        }
      foundOwner.ConvoUpdateMap.put(foundConversation.id, Time.now());
    } else {
      newMessages = -1;
    }

    return newMessages;
  }

  private static <S,T> Collection<T> all(StoreAccessor<S,T> store) {

    final Collection<T> all = new ArrayList<>();

    for (final T value : store.all()) {
        all.add(value);
    }

    return all;
  }

  private static <T> Collection<T> intersect(StoreAccessor<Uuid, T> store, Collection<Uuid> ids) {

    // Use a set to hold the found users as this will prevent duplicate ids from
    // yielding duplicates in the result.

    final Collection<T> found = new HashSet<>();

    for (final Uuid id : ids) {

      final T t = store.first(id);

      if (t == null) {
        LOG.warning("Unmapped id %s", id);
      } else if (found.add(t)) {
        // do nothing
      } else {
        LOG.warning("Duplicate id %s", id);
      }
    }

    return found;
  }
}
