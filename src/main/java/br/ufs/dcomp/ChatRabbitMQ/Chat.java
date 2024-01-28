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

  public static void main(String[] argv) throws Exception {
    String host = "54.89.54.95";
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(host); // Alterar
    factory.setUsername("admin"); // Alterar
    factory.setPassword("password"); // Alterar
    factory.setVirtualHost("/");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    // Identificador para saber se as mensagens digitadas
    // devem ser enviadas para alguém
    int promptMode = 0;
    String myUser;
    String promptSymbol = ">>";
    String recipient = "";
    String currentGroup = "";
    String input;
    Scanner scanner = new Scanner(System.in);

    System.out.print("User: ");
    myUser = scanner.nextLine().trim();
    Client client = new Client(myUser, channel);
    client.startClient();
    Group.setChannel(channel);

    promptText = "@" + myUser + promptSymbol;

    do {
      System.out.print(promptText);
      input = scanner.nextLine().trim();
      // Checa o input para saber se é um usuário e checar se é diferente do
      // meu próprio usuário
      if (input.startsWith("@") && input.length() > 1 && !input.substring(1).equals(myUser)) {
        // Pega o destinatário sem o @
        recipient = input.substring(1);
        client.setRecipient(recipient);
        // Atualiza o que deve ser printado no prompt
        promptText = "@" + recipient + promptSymbol;
        // Muda o modo de prompt. Agora tudo o que é digitado no prompt
        // irá ser enviado por destinatário atual
        promptMode = 1;

        // Checa o modo do prompt e evita que "quit" seja enviado para o
        // destinatário
      } else if (input.charAt(0) == '!') {

        String[] args = input.split(" ");
        String methodStr = args[0].substring(1);

        if (Arrays.asList(Chat.commands).contains(methodStr)) {

          Method method;
          List<Object> temp = Arrays.asList((Object[]) args);
          List<Object> argsList = new ArrayList<Object>(temp);

          argsList.remove(0);
          if (methodStr.equals("removeGroup")) {

            method = Group.class.getDeclaredMethod(methodStr, String.class);
          } else {
            method = Group.class.getDeclaredMethod(methodStr, String.class, String.class);
          }

          Object[] test = argsList.toArray();

          method.invoke(null, test);
          System.out.println("tá chegando aqui!");
        }
      } else if (input.startsWith("#") && input.length() > 1) {

        promptMode = 2;
        currentGroup = input.substring(1);
        promptText = "#" + currentGroup + promptSymbol;

      } else if (promptMode == 2 && !input.equals("quit")) {

        Group.sendMessage(currentGroup, myUser, input);

      } else if (promptMode == 1 && !input.equals("quit")) {

        client.sendMessage(input);

      }
    } while (!input.equals("quit"));

    scanner.close();
    channel.close();
    connection.close();
  }
}