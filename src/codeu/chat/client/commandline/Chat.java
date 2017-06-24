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

package codeu.chat.client.commandline;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;
import codeu.chat.util.Tokenizer;
import java.io.IOException;

import codeu.chat.common.ServerInfo;
import codeu.chat.client.core.Context;
import codeu.chat.client.core.ConversationContext;
import codeu.chat.client.core.MessageContext;
import codeu.chat.client.core.UserContext;

public final class Chat {

  // PANELS
  //
  // We are going to use a stack of panels to track where in the application
  // we are. The command will always be routed to the panel at the top of the
  // stack. When a command wants to go to another panel, it will add a new
  // panel to the top of the stack. When a command wants to go to the previous
  // panel all it needs to do is pop the top panel.
  private final Stack<Panel> panels = new Stack<>();

  public Chat(Context context) {
    this.panels.push(createRootPanel(context));
  }

  // HANDLE COMMAND
  //
  // Take a single line of input and parse a command from it. If the system
  // is willing to take another command, the function will return true. If
  // the system wants to exit, the function will return false.
  //
  public boolean handleCommand(String line) {

    final List<String> args = new ArrayList<>();
    final Tokenizer tokenizer = new Tokenizer(line);
    try {
          for (String token = tokenizer.next(); token != null; token = tokenizer.next()) {
              args.add(token);
          }
      }
      catch (IOException e) {

      }
    final String command = args.get(0);
    args.remove(0);


    // Because "exit" and "back" are applicable to every panel, handle
    // those commands here to avoid having to implement them for each
    // panel.

    if ("exit".equals(command)) {
      // The user does not want to process any more commands
      return false;
    }

    // Do not allow the root panel to be removed.
    if ("back".equals(command) && panels.size() > 1) {
      panels.pop();
      return true;
    }

    if (panels.peek().handleCommand(command, args)) {
      // the command was handled
      return true;
    }

    // If we get to here it means that the command was not correctly handled
    // so we should let the user know. Still return true as we want to continue
    // processing future commands.
    System.out.println("ERROR: Unsupported command");
    return true;
  }

  // CREATE ROOT PANEL
  //
  // Create a panel for the root of the application. Root in this context means
  // the first panel and the only panel that should always be at the bottom of
  // the panels stack.
  //
  // The root panel is for commands that require no specific contextual information.
  // This is before a user has signed in. Most commands handled by the root panel
  // will be user selection focused.
  //
  private Panel createRootPanel(final Context context) {

    final Panel panel = new Panel();

    // HELP
    //
    // Add a command to print a list of all commands and their description when
    // the user for "help" while on the root panel.
    //
    panel.register("help", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("ROOT MODE");
        System.out.println("  u-list");
        System.out.println("    List all users.");
        System.out.println("  u-add <name>");
        System.out.println("    Add a new user with the given name.");
        System.out.println("  u-sign-in <name>");
        System.out.println("    Sign in as the user with the given name.");
        System.out.println("  info");
        System.out.println("    Get server information.");
        System.out.println("  exit");
        System.out.println("    Exit the program.");
      }
    });

    // U-LIST (user list)
    //
    // Add a command to print all users registered on the server when the user
    // enters "u-list" while on the root panel.
    //
    panel.register("u-list", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        for (final UserContext user : context.allUsers()) {
          System.out.format(
              "USER %s (UUID:%s)\n",
              user.user.name,
              user.user.id);
        }
      }
    });

    // U-ADD (add user)
    //
    // Add a command to add and sign-in as a new user when the user enters
    // "u-add" while on the root panel.
    //
    panel.register("u-add", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        for(String token : args){
          final String name = token;
          if (name.length() > 0) {
            if (context.create(name) == null) {
              System.out.println("ERROR: Failed to create new user");
            }
          } else {
            System.out.println("ERROR: Missing <username>");
          }
        }
      }
    });

    // U-SIGN-IN (sign in user)
    //
    // Add a command to sign-in as a user when the user enters "u-sign-in"
    // while on the root panel.
    //
    panel.register("u-sign-in", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        for(String token : args){
          final String name = token;
          if (name.length() > 0) {
            final UserContext user = findUser(name);
            if (user == null) {
              System.out.format("ERROR: Failed to sign in as '%s'\n", name);
            } else {
              panels.push(createUserPanel(context, user));
            }
          } else {
            System.out.println("ERROR: Missing <username>");
          }
        }
      }

      // Find the first user with the given name and return a user context
      // for that user. If no user is found, the function will return null.
      private UserContext findUser(String name) {
        for (final UserContext user : context.allUsers()) {
          if (user.user.name.equals(name)) {
            return user;
          }
        }
        return null;
      }
    });

    // INFO (get server info)
    //
    // Add a command to get server info when the user enters "info" while on
    // the root panel.
    //
    panel.register("info", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final ServerInfo info = context.getInfo();
        if (info == null) {
          System.out.println("ERROR: The server did not send us a valid info object.");
        } else {
          System.out.println("Current server version: " + info.version);
          System.out.println("Start Time: " + info.startTime);
        }
      }
    });

    // Now that the panel has all its commands registered, return the panel
    // so that it can be used.
    return panel;
  }

  private Panel createUserPanel(final Context context, final UserContext user) {

    final Panel panel = new Panel();

    // HELP
    //
    // Add a command that will print a list of all commands and their
    // descriptions when the user enters "help" while on the user panel.
    //
    panel.register("help", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("USER MODE");
        System.out.println("  c-list");
        System.out.println("    List all conversations that the current user can interact with.");
        System.out.println("  c-add <title>");
        System.out.println("    Add a new conversation with the given title and join it as the current user.");
        System.out.println("  c-join <title>");
        System.out.println("    Join the conversation as the current user.");
        System.out.println("  i-u-add <name>");
        System.out.println("    Add a user to the current user's interests.");
        System.out.println("  i-u-remove <name>");
        System.out.println("    Remove a user from the current user's interests.");
        System.out.println("  i-c-add <title>");
        System.out.println("    Add a conversation to the current user's interests.");
        System.out.println("  i-c-remove <title>");
        System.out.println("    Remove a conversation from the current user's interests.");
        System.out.println("  status-update-u <name>");
        System.out.println("    Call a status update on the specified user interest.");
        System.out.println("  status-update-c <title>");
        System.out.println("    Call a status update on the specified conversation interest.");
        System.out.println("  info");
        System.out.println("    Display all info for the current user");
        System.out.println("  back");
        System.out.println("    Go back to ROOT MODE.");
        System.out.println("  exit");
        System.out.println("    Exit the program.");
      }
    });

    // C-LIST (list conversations)
    //
    // Add a command that will print all conversations when the user enters
    // "c-list" while on the user panel.
    //
    panel.register("c-list", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        for (final ConversationContext conversation : user.conversations()) {
          System.out.format(
              "CONVERSATION %s (UUID:%s)\n",
              conversation.conversation.title,
              conversation.conversation.id);
        }
      }
    });

    // C-ADD (add conversation)
    //
    // Add a command that will create and join a new conversation when the user
    // enters "c-add" while on the user panel.
    //
    panel.register("c-add", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        for (String token : args){
          final String name = token;
          if (name.length() > 0) {
            final ConversationContext conversation = user.start(name);
            if (conversation == null) {
              System.out.println("ERROR: Failed to create new conversation");
            } else {
              panels.push(createConversationPanel(conversation));
            }
          } else {
            System.out.println("ERROR: Missing <title>");
          }
        }
      }
    });

    // C-JOIN (join conversation)
    //
    // Add a command that will joing a conversation when the user enters
    // "c-join" while on the user panel.
    //
    panel.register("c-join", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        for(String token : args){
          final String name = token;
          if (name.length() > 0) {
            final ConversationContext conversation = find(name);
            if (conversation == null) {
              System.out.format("ERROR: No conversation with name '%s'\n", name);
            } else {
              panels.push(createConversationPanel(conversation));
            }
          } else {
            System.out.println("ERROR: Missing <title>");
          }
        }
      }

      // Find the first conversation with the given name and return its context.
      // If no conversation has the given name, this will return null.
      private ConversationContext find(String title) {
        for (final ConversationContext conversation : user.conversations()) {
          if (title.equals(conversation.conversation.title)) {
            return conversation;
          }
        }
        return null;
      }
    });

    // I-C-ADD (add conversation interest)
    //
    // Add a command that will add a conversation to the current user's
    // interests when the user enters "i-c-add" while on the user panel
    //
    panel.register("i-c-add", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        for(String token : args){
          final String title = token;
          if (title.length() > 0) {
            int response = user.addConversationInterest(title);
            if (response == 1) {
              System.out.println("Conversation \"" + title + "\" added to interests");
            } else if (response == 0) {
              System.out.println("ERROR: Conversation \"" + title + "\" already in interests");
            } else {
              System.out.format("ERROR: No conversation with name '%s'\n", title);
            }
          } else {
            System.out.println("ERROR: Missing <title>");
          }
        }
      }
    });

    // I-C-REMOVE (remove conversation interest)
    //
    // Add a command that will remove a conversation to the current user's
    // interests when the user enters "i-c-remove" while on the user panel
    //
    panel.register("i-c-remove", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        for(String token : args){
          final String title = token;
          if (title.length() > 0) {
            int response = user.removeConversationInterest(title);
            if (response == 1) {
              System.out.println("Conversation \"" + title + "\" removed from interests");
            } else if (response == 0) {
              System.out.println("ERROR: Conversation \"" + title + "\" not in interests");
            } else {
              System.out.format("ERROR: No conversation with name '%s'\n", title);
            }
          } else {
            System.out.println("ERROR: Missing <title>");
          }
        }
      }
    });

    // STATUS-UPDATE-C (conversation status update)
    //
    // Add a command that will check for new messages in one of the user's conversation
    // interests when the user enters "status-update-c" while on the user panel
    //
    panel.register("status-update-c", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        for(String token : args){
          final String title = token;
          if (title.length() > 0) {
            final int newMessages = user.conversationStatusUpdate(title);
              if (newMessages == -1) {
                System.out.println("ERROR: Conversation \"" + title + "\" not in interests");
              } else if (newMessages == -2) {
                System.out.format("ERROR: No conversation with name '%s'\n", title);
              } else if (newMessages == 0) {
                System.out.println("No new messages in conversation \"" + title +"\"");
              } else {
                System.out.println(newMessages + " new messages in conversation \"" + title + "\"");
              }
          } else {
            System.out.println("ERROR: Missing <title>");
          }
        }
      }
    });

    // I-U-ADD (add user interest)
    //
    // Add a command that will add a user to the current user's
    // interests when the user enters "i-u-add" while on the user panel
    //
    panel.register("i-u-add", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        for(String token : args){
          final String name = token;
          if (name.length() > 0) {
            int response = user.addUserInterest(name);
            if (response == 1) {
              System.out.println("User \"" + name + "\" added to interests");
            } else if (response == 0) {
              System.out.println("ERROR: User \"" + name + "\" already in interests");
            } else {
              System.out.format("ERROR: No user with name '%s'\n", name);
            }
          } else {
            System.out.println("ERROR: Missing <username>");
          }
        }
      }
    });

    // I-U-REMOVE (Remove user interest)
    //
    // Add a command that will remove a user to the current user's
    // interests when the user enters "i-u-remove" while on the user panel
    //
    panel.register("i-u-remove", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        for(String token : args){
          final String name = token;
          if (name.length() > 0) {
            int response = user.removeUserInterest(name);
            if (response == 1) {
              System.out.println("User \"" + name + "\" removed from interests");
            } else if (response == 0) {
              System.out.println("ERROR: User \"" + name + "\" not in interests");
            } else {
              System.out.format("ERROR: No user with name '%s'\n", name);
            }
          } else {
            System.out.println("ERROR: Missing <username>");
          }
        }
      }
    });

    // STATUS-UPDATE-U (user status update)
    //
    // Add a command that will add check new conversations created by and
    //  contributed to by one of the current user's user interests when the
    //  user enters "status-update-u" while on the user panel
    //
    panel.register("status-update-u", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        for(String token : args){
          final String name = token;
          if (name.length() > 0) {
            final UserContext foundUser = findUser(name);
            if (foundUser == null) {
              System.out.format("ERROR: No user with name '%s'\n", name);
            } else {
              System.out.println("User \"" + name + "\" has contributed to:");
              for(final String contribution : user.userStatusUpdate(name)) {
                System.out.println(contribution);
              }
            }
          } else {
            System.out.println("ERROR: Missing <username>");
          }
        }
      }

      // Find the first user with the given name and return a user context
      // for that user. If no user is found, the function will return null.
      private UserContext findUser(String name) {
        for (final UserContext user : context.allUsers()) {
          if (user.user.name.equals(name)) {
            return user;
          }
        }
        return null;
      }
    });

    // INFO
    //
    // Add a command that will print info about the current context when the
    // user enters "info" while on the user panel.
    //
    panel.register("info", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("User Info:");
        System.out.format("  Name : %s\n", user.user.name);
        System.out.format("  Id   : UUID:%s\n", user.user.id);
      }
    });

    // Now that the panel has all its commands registered, return the panel
    // so that it can be used.
    return panel;
  }

  private Panel createConversationPanel(final ConversationContext conversation) {

    final Panel panel = new Panel();

    // HELP
    //
    // Add a command that will print all the commands and their descriptions
    // when the user enters "help" while on the conversation panel.
    //
    panel.register("help", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("USER MODE");
        System.out.println("  m-list");
        System.out.println("    List all messages in the current conversation.");
        System.out.println("  m-add <message>");
        System.out.println("    Add a new message to the current conversation as the current user.");
        System.out.println("  info");
        System.out.println("    Display all info about the current conversation.");
        System.out.println("  back");
        System.out.println("    Go back to USER MODE.");
        System.out.println("  exit");
        System.out.println("    Exit the program.");
      }
    });

    // M-LIST (list messages)
    //
    // Add a command to print all messages in the current conversation when the
    // user enters "m-list" while on the conversation panel.
    //
    panel.register("m-list", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("--- start of conversation ---");
        for (MessageContext message = conversation.firstMessage();
                            message != null;
                            message = message.next()) {
          System.out.println();
          System.out.format("USER : %s\n", message.message.author);
          System.out.format("SENT : %s\n", message.message.creation);
          System.out.println();
          System.out.println(message.message.content);
          System.out.println();
        }
        System.out.println("---  end of conversation  ---");
      }
    });

    // M-ADD (add message)
    //
    // Add a command to add a new message to the current conversation when the
    // user enters "m-add" while on the conversation panel.
    //
    panel.register("m-add", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        for(String token : args){
          final String message = token;
          if (message.length() > 0) {
            conversation.add(message);
          } else {
            System.out.println("ERROR: Messages must contain text");
          }
        }
      }
    });

    // INFO
    //
    // Add a command to print info about the current conversation when the user
    // enters "info" while on the conversation panel.
    //
    panel.register("info", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("Conversation Info:");
        System.out.format("  Title : %s\n", conversation.conversation.title);
        System.out.format("  Id    : UUID:%s\n", conversation.conversation.id);
        System.out.format("  Owner : %s\n", conversation.conversation.owner);
      }
    });

    // Now that the panel has all its commands registered, return the panel
    // so that it can be used.
    return panel;
  }
}
