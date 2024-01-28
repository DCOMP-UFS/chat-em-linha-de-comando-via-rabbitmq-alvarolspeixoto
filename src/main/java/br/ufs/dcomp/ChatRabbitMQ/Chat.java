package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;
import java.util.Scanner;

public class Chat {

  public static String promptText = "";

  public static void main(String[] argv) throws Exception {
    String host = "44.223.29.7";
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(host); // Alterar
    factory.setUsername("admin"); // Alterar
    factory.setPassword("password"); // Alterar
    factory.setVirtualHost("/");
    Connection connection = factory.newConnection();

    // Identificador para saber se as mensagens digitadas
    // devem ser enviadas para alguém
    int promptMode = 0;
    // Saber se é a primeira iteração pra ajustar o print do prompt
    // caso haja mensagens acumuladas
    // boolean isFirst = true;
    String myUser;
    String promptSymbol = ">>";
    String recipient = "";
    String input;
    Scanner scanner = new Scanner(System.in);

    System.out.print("User: ");
    myUser = scanner.nextLine().trim();
    Client client = new Client(myUser, connection);
    client.startClient();

    promptText = myUser + promptSymbol;

    do {
      /*
       * if (isFirst)
       * {
       * System.out.println();
       * isFirst = false;
       * }
       */
      System.out.print("@" + promptText);
      input = scanner.nextLine().trim();
      // Checa o input para saber se é um usuário e checar se é diferente do
      // meu próprio usuário
      if (input.startsWith("@") && input.length() > 1 && !input.substring(1).equals(myUser)) {
        // Pega o destinatário sem o @
        recipient = input.substring(1);
        client.setRecipient(recipient);
        // Atualiza o que deve ser printado no prompt
        promptText = recipient + promptSymbol;
        // Muda o modo de prompt. Agora tudo o que é digitado no prompt
        // irá ser enviado por destinatário atual
        promptMode = 1;

        // Checa o modo do prompt e evita que "quit" seja enviado para o
        // destinatário
      } else if (promptMode == 1 && !input.equals("quit")) {

        client.sendMessage(input);

      }
    } while (!input.equals("quit"));

    scanner.close();
    client.closeConnection();

  }
}