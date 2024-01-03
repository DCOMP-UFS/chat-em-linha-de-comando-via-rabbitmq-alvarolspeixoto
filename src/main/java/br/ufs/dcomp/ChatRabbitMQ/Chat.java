package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;
import java.io.IOException;
import java.util.Scanner;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import java.text.MessageFormat;

public class Chat {

    public static String promptText = "";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("18.208.131.240"); // Alterar
    factory.setUsername("admin"); // Alterar
    factory.setPassword("password"); // Alterar
    factory.setVirtualHost("/");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    
    // Identificador para saber se as mensagens digitadas
    // devem ser enviadas para alguém
    int promptMode = 0;
    // Saber se é a primeira iteração pra ajustar o print do prompt
    // caso haja mensagens acumuladas
    boolean isFirst = true;
    String myUser;
    String promptSymbol = ">>";
    String recipient = "";
    String input;
    String messageToSend;
    Scanner scanner = new Scanner(System.in);
    
    System.out.print("User: ");
    myUser = scanner.nextLine().trim();
    promptText = myUser + promptSymbol;
    
    String QUEUE_NAME = myUser;
                      //(queue-name, durable, exclusive, auto-delete, params); 
    channel.queueDeclare(QUEUE_NAME, false,   false,     false,       null);
    
    Consumer consumer = new DefaultConsumer(channel) {
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)           throws IOException {

        String message = new String(body, "UTF-8");
        System.out.println("\n" + message);
        System.out.print("@" + promptText);

      }
    };
                      //(queue-name, autoAck, consumer);    
    channel.basicConsume(QUEUE_NAME, true,    consumer);
    
    do {
      if (isFirst)
      {
        System.out.println();
        isFirst = false;
      }
      System.out.print("@" + promptText);
      input = scanner.nextLine().trim();
      // Checa o input para saber se é um usuário e checar se é diferente do
      // meu próprio usuário
      if (input.startsWith("@") && input.length() > 1 && !input.substring(1).equals(myUser))
      {
        // Pega o destinatário sem o @
        recipient = input.substring(1);
        // Atualiza o que deve ser printado no prompt
        promptText = recipient + promptSymbol;
        // Cria uma fila com o nome do novo destinatário
        channel.queueDeclare(recipient, false, false, false, null);
        // Muda o modo de prompt. Agora tudo o que é digitado no prompt
        // irá ser enviado por destinatário atual
        promptMode = 1;
        
        // Checa o modo do prompt e evita que "quit" seja enviado para o
        // destinatário
      } else if (promptMode == 1 && !input.equals("quit")) {
        
        // Obtém a hora do Brasil e formata a mensagem a ser enviada
        String zonaBr = "America/Sao_Paulo";
        ZonedDateTime myDateTime = ZonedDateTime.now(java.time.ZoneId.of(zonaBr));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
        String formattedDateTime = myDateTime.format(formatter);
        messageToSend = MessageFormat.format("({0}) {1} diz: {2}", formattedDateTime, myUser, input);
        
        // Envia a mensagem
        channel.basicPublish("", recipient, null, messageToSend.getBytes("UTF-8"));
      }
    } while (!input.equals("quit"));
    
    channel.close();
    connection.close();
    
  }
}