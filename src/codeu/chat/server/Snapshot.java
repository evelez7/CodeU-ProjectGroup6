package codeu.chat.server;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.ArrayList;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import codeu.chat.common.ConversationHeader;
import codeu.chat.common.ConversationPayload;
import codeu.chat.util.Logger;

import com.google.gson.Gson;

public class Snapshot {
  private final Model model = new Model();
  private View newView = new View(model);

  Queue<Gson> buffer = new LinkedList<>();
  Gson gson = new Gson();

  FileWriter fileWriter;
  BufferedWriter bufferedWriter;

  File dataStorage = new File("src\\codeu\\chat\\server\\JsonData.txt");

  private void createWriters() {
    try {
      newFileWriter = new FileWriter(dataStorage);
      writer = new BufferedWriter(fileWriter);
    } catch (IOException ex) {
      LOG.error(ex, "Exception during creation of writers");
    }
  }

  String conversationJson = gson.toJson(newView.getConversations());
  String userJson = gson.toJson(newView.getUsers());

  private void writeToFile() {
    try {
      writer.write(conversationJson);
    } catch (IOException ex) {
      LOG.error(ex, "Exception while writing to data file.");
    }
  }
}
