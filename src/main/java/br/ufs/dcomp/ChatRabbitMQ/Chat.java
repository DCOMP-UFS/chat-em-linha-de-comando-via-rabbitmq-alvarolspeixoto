package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Chat {

  public static String promptText = "";
  public static String[] commands = { "addGroup", "addUser", "delFromGroup", "removeGroup" };

  public enum PromptMode {
    NONE,
    PRIVATE,
    GROUP
  }

  public static void main(String[] argv) throws Exception {
    String host = "107.22.85.20";
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(host); // Alterar
    factory.setUsername("admin"); // Alterar
    factory.setPassword("password"); // Alterar
    factory.setVirtualHost("/");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    // Identificador para saber se as mensagens digitadas
    // devem ser enviadas para alguém
    PromptMode promptMode = PromptMode.NONE;
    String promptSymbol = ">>";
    String currentUser = "";
    String currentRecipient = "";
    String currentGroup = "";
    String input;
    Scanner scanner = new Scanner(System.in);

    System.out.print("User: ");
    currentUser = scanner.nextLine().trim();
    Client client = new Client(currentUser, channel);
    client.startClient();
    Group.setChannel(channel);
    Group.setConnection(connection);

    promptText = "@" + currentUser + promptSymbol;

    do {
      System.out.print(promptText);
      input = scanner.nextLine().trim();

      // Checa o input para saber se é um usuário e checar se é diferente do
      // meu próprio usuário
      if (input.startsWith("@") && input.length() > 1 && !input.substring(1).equals(currentUser)) {
        // Pega o destinatário sem o @
        currentRecipient = input.substring(1);
        client.setRecipient(currentRecipient);
        // Atualiza o que deve ser printado no prompt
        promptText = "@" + currentRecipient + promptSymbol;
        // Muda o modo de prompt. Agora tudo o que é digitado no prompt
        // irá ser enviado por destinatário atual
        promptMode = PromptMode.PRIVATE;

        // Checa o modo do prompt e evita que "quit" seja enviado para o
        // destinatário
      } else if (input.charAt(0) == '!') {

        String[] args = input.split(" ");
        String methodStr = args[0].substring(1);

        if (Arrays.asList(Chat.commands).contains(methodStr)) {

          Method method;
          List<Object> temp = Arrays.asList((Object[]) args);
          List<Object> argList = new ArrayList<Object>(temp);

          argList.remove(0);
          if (methodStr.equals("removeGroup") || methodStr.equals("addGroup")) {

            method = Group.class.getDeclaredMethod(methodStr, String.class);
            promptMode = PromptMode.NONE;
            promptText = "@" + currentUser + promptSymbol;
            
          } else {
            method = Group.class.getDeclaredMethod(methodStr, String.class, String.class);
          }

          Object[] objArgs = argList.toArray();

          try {

            method.invoke(null, objArgs);

          } catch (Exception e) {
            System.out.println("Erro ao chamar o método \"" + methodStr + "\". " + "Confira o número de argumentos.");
          }
        } else {
          System.out.println("[!] O comando \"" + methodStr + "\" não existe.");
        }
      } else if (input.startsWith("#") && input.length() > 1) {

        if (Group.checkIfGroupExists(input.substring(1))) {
          promptMode = PromptMode.GROUP;
          currentGroup = input.substring(1);
          promptText = "#" + currentGroup + promptSymbol;
        }

      } else if (promptMode == PromptMode.GROUP && !input.equals("quit")) {

        client.sendMessage(input, currentUser, currentGroup);

      } else if (promptMode == PromptMode.PRIVATE && !input.equals("quit")) {

        client.sendMessage(input, currentUser, "");

      }
    } while (!input.equals("quit"));

    scanner.close();
    channel.close();
    connection.close();
  }
}