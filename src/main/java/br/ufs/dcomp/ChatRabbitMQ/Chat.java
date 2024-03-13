package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class Chat {

  private static String promptText = "";
  private static String[] commands = { "addGroup", "addUser", "delFromGroup", "removeGroup", "listUsers", "listGroups" };
  private static Connection connection;
  private static Channel channel;
  private final static String promptSymbol = ">>";
  private static PromptMode promptMode = PromptMode.NONE;
  private static String currentUser = "";
  private static String currentRecipient = "";
  private static String currentGroup = "";
  private static String host;
  private static String user;
  private static String password;


  public static void setup(String host, String username, String password) throws IOException, TimeoutException {
    ConnectionFactory factory = new ConnectionFactory();
    Chat.host = host;
    Chat.user = username;
    Chat.password = password;
    factory.setHost(host);
    factory.setPort(5672);
    factory.setUsername(username);
    factory.setPassword(password);
    factory.setVirtualHost("/");
    Chat.connection = factory.newConnection();
    Chat.channel = connection.createChannel();
  }

  public static String getPromptText() {
    return promptText;
  }

  public static String getPromptSymbol() {
    return promptSymbol;
  }

  public static Connection getConnection() {
    return connection;
  }

  public static String getPassword() {
    return password;
  }

  public static String getUser() {
    return user;
  }

  public static String getHost() {
    return host;
  }

  public enum PromptMode {
    NONE, // o que é digitado não é enviado para ninguém
    PRIVATE, // enviado p/ destinatário específico
    GROUP // enviado p/ grupo
  }

  public static void main(String[] argv) throws Exception {

    Properties properties = Utils.getProps();
    
    String host = properties.getProperty("rabbitmq.host");
    String user = properties.getProperty("rabbitmq.user");
    String password = properties.getProperty("rabbitmq.password");

    Chat.setup(host, user, password);
    String input;
    Scanner scanner = new Scanner(System.in);

    System.out.print("User: ");
    currentUser = scanner.nextLine().trim();
    Client client = new Client(currentUser, channel);
    client.startClient();
    Group.setChannel(channel);
    Group.setConnection(connection);

    promptText = promptSymbol;

    do {
      System.out.print(promptText);
      input = scanner.nextLine().trim();

      if (input.length() == 0) {
        continue;
      }

      if (input.startsWith("@") && input.length() > 1 && !input.substring(1).equals(currentUser)) {

        currentRecipient = input.substring(1);
        client.setRecipient(currentRecipient);
        currentGroup = "";
        promptText = "@" + currentRecipient + promptSymbol;
        promptMode = PromptMode.PRIVATE;

      } else if (input.charAt(0) == '!') {

        String[] args = input.split(" ");
        String methodStr = args[0].substring(1);

        if (Arrays.asList(Chat.commands).contains(methodStr)) {

          Method method;
          List<Object> temp = Arrays.asList((Object[]) args);
          List<Object> argList = new ArrayList<Object>(temp);

          argList.remove(0);
          
          String[] test = {"removeGroup", "addGroup", "listUsers", "listGroups"};

          if (Arrays.asList(test).contains(methodStr)) {

            method = Group.class.getDeclaredMethod(methodStr, String.class);
            promptMode = PromptMode.NONE;
            promptText = promptSymbol;
            currentGroup = "";

          } else {
            method = Group.class.getDeclaredMethod(methodStr, String.class, String.class);
          }

          Object[] objArgs = argList.toArray();

          try {

            method.invoke(null, objArgs);

          } catch (Exception e) {
            System.out.println("Erro ao chamar o método \"" + methodStr + "\". " + "Confira o número de argumentos.");
          }
        } else if (methodStr.equals("upload")) {
          if (args.length < 2) {
            System.out.println("Informe o caminho do arquivo!");
            continue;
          }
          if (promptMode == PromptMode.NONE) {
            System.out.println("[!] O prompt não está direcionado a nenhum grupo ou usuário.");
            continue;
          }
          String filePath = args[1];
          client.sendFile(filePath, currentUser, currentRecipient, currentGroup);
        } else {
          System.out.println("[!] O comando \"" + methodStr + "\" não existe.");
        }
      } else if (input.startsWith("#") && input.length() > 1) {

        if (Group.checkIfGroupExists(input.substring(1))) {
          promptMode = PromptMode.GROUP;
          currentGroup = input.substring(1);
          promptText = "#" + currentGroup + promptSymbol;
        }

      } else if (promptMode != PromptMode.NONE && !input.equals("quit")) {

        client.sendMessage(input, currentUser, currentGroup);

      }
    } while (!input.equals("quit"));

    scanner.close();
    channel.close();
    connection.close();
  }
}