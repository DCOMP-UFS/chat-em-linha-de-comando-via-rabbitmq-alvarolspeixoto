package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import java.text.MessageFormat;

public class Client {

    private String username;
    private String recipient;
    private Channel channel;
    private final String QUEUE_NAME;

    public Client(String username, Channel channel) throws IOException {
        this.username = username;
        this.QUEUE_NAME = username;
        this.channel = channel;
    }

    public void startClient() throws IOException {
        // Cria a fila
        // (queue-name, durable, exclusive, auto-delete, params);
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        // Declara o usuário atual como um consumidor
        Consumer consumer = new DefaultConsumer(channel) {
            // Função que roda o tempo todo monitorando as mensagens que são recebidas
            // e lida com elas.
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                    byte[] body) throws IOException {

                String message = new String(body, "UTF-8");
                System.out.println("\n" + message);
                if (!Chat.promptText.equals("")) {
                    System.out.print(Chat.promptText);
                }

            }
        };

        // (queue-name, autoAck, consumer);
        channel.basicConsume(QUEUE_NAME, true, consumer);
    }

    public void setRecipient(String recipient) throws IOException {
        this.recipient = recipient;

        // Cria uma fila com o nome do novo destinatário
        channel.queueDeclare(recipient, false, false, false, null);
    }

    public void sendMessage(String message) throws UnsupportedEncodingException, IOException {
        message = formatMessage(message);
        // Envia a mensagem
        channel.basicPublish("", recipient, null, message.getBytes("UTF-8"));
    }

    private String formatMessage(String message) {
        // Obtém a hora do Brasil e formata a mensagem a ser enviada
        String zonaBr = "America/Sao_Paulo";
        ZonedDateTime myDateTime = ZonedDateTime.now(java.time.ZoneId.of(zonaBr));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
        String formattedDateTime = myDateTime.format(formatter);
        message = MessageFormat.format("({0}) {1} diz: {2}", formattedDateTime, username, message);

        return message;
    }

}