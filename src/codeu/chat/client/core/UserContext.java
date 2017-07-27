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

package codeu.chat.client.core;

import java.util.ArrayList;
import java.util.Collection;

import codeu.chat.common.BasicController;
import codeu.chat.common.BasicView;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.User;
import codeu.chat.util.Uuid;

public final class UserContext {

  public final User user;
  private final BasicView view;
  private final BasicController controller;

  public UserContext(User user, BasicView view, BasicController controller) {
    this.user = user;
    this.view = view;
    this.controller = controller;
  }

  public ConversationContext start(String name) {
    final ConversationHeader conversation = controller.newConversation(name, user.id);
    return conversation == null ?
        null :
        new ConversationContext(user, conversation, view, controller);
  }

  public Iterable<ConversationContext> conversations() {

    // Use all the ids to get all the conversations and convert them to
    // Conversation Contexts.
    final Collection<ConversationContext> all = new ArrayList<>();
    for (final ConversationHeader conversation : view.getConversations()) {
      all.add(new ConversationContext(user, conversation, view, controller));
    }

    return all;
  }

  public enum response {
    NO_ERROR, ERROR_ALREADY_CURRENT_SETTING, ERROR_NOT_FOUND
  }

  public response addUserInterest(String name) {
    switch(controller.addUserInterest(name, user.id)) {
      default:
        return response.ERROR_NOT_FOUND;
      case 0:
        return response.NO_ERROR;
      case -1:
        return response.ERROR_ALREADY_CURRENT_SETTING;
    }
  }

  public response removeUserInterest(String name) {
    switch(controller.removeUserInterest(name, user.id)) {
      default:
        return response.ERROR_NOT_FOUND;
      case 0:
        return response.NO_ERROR;
      case -1:
        return response.ERROR_ALREADY_CURRENT_SETTING;
    }
  }

  public response addConversationInterest(String title) {
    switch(controller.addConversationInterest(title, user.id)) {
      default:
        return response.ERROR_NOT_FOUND;
      case 0:
        return response.NO_ERROR;
      case -1:
        return response.ERROR_ALREADY_CURRENT_SETTING;
    }
  }

  public response removeConversationInterest(String title) {
    switch(controller.removeConversationInterest(title, user.id)) {
      default:
        return response.ERROR_NOT_FOUND;
      case 0:
        return response.NO_ERROR;
      case -1:
        return response.ERROR_ALREADY_CURRENT_SETTING;
    }
  }

  public enum permissionResponse {
    NO_ERROR, ERROR_NOT_ALLOWED, ERROR_UNEXPECTED_RESPONSE
  }

  public permissionResponse attemptJoinConversation(String title) {
    switch(view.attemptJoinConversation(title, user.id)) {
      default:
        return permissionResponse.ERROR_UNEXPECTED_RESPONSE;
      case 0:
        return permissionResponse.NO_ERROR;
      case -1:
        return permissionResponse.ERROR_NOT_ALLOWED;
    }
  }

  public Collection<String> userStatusUpdate(String name) {
    return view.userStatusUpdate(name, user.id);
  }

  public int conversationStatusUpdate(String title) {
    return view.conversationStatusUpdate(title, user.id);
  }

}
