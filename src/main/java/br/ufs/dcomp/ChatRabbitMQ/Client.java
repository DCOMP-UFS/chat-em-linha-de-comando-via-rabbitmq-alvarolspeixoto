package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import java.text.MessageFormat;

import com.google.protobuf.*;

public class Client {

    private static String username;
    private String recipient;
    private static Channel channel;
    private final String QUEUE_NAME;

    public Client(String username, Channel channel) throws IOException {
        Client.username = username;
        this.QUEUE_NAME = username;
        Client.channel = channel;
    }

    public void startClient() throws IOException {

        // (queue-name, durable, exclusive, auto-delete, params);
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        Consumer consumer = new DefaultConsumer(channel) {
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                    byte[] body) throws IOException {

                MessageProto.Message message = MessageProto.Message.parseFrom(body);

                if (message.getSender().equals(username)) {
                    return;
                }
                
                String formattedMessage = formatMessage(message);

                System.out.println(formattedMessage);

                if (!Chat.promptText.equals("")) {
                    System.out.print(Chat.promptText);
                }

            }
        };

        // (queue-name, autoAck, consumer);
        channel.basicConsume(QUEUE_NAME, true, consumer);
    }

    private String formatMessage(MessageProto.Message message) {

        String sender = message.getSender();
        String date = message.getDate();
        String time = message.getTime();
        String group = message.getGroup();

        MessageProto.Content content = message.getContent();
        String messageBody = content.getBody().toStringUtf8();

        if (group.equals("")) {
            return MessageFormat.format("\n({0} às {1}) {2} diz: {3}", date, time, sender, messageBody);
        }

        return MessageFormat.format("\n({0} às {1}) {2}#{3} diz: {4}", date, time, sender, group, messageBody);

    }

    public void setRecipient(String recipient) throws IOException {
        this.recipient = recipient;
        channel.queueDeclare(recipient, false, false, false, null);
    }

    public void sendMessage(String body, String sender, String group) throws UnsupportedEncodingException, IOException {

        String zone = "America/Sao_Paulo";
        ZonedDateTime dateTime = ZonedDateTime.now(java.time.ZoneId.of(zone));
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        String date = dateTime.format(dateFormatter);
        String time = dateTime.format(timeFormatter);

        MessageProto.Content.Builder content = MessageProto.Content.newBuilder();
        content.setType("text/plain");
        content.setBody(ByteString.copyFromUtf8(body));
        content.setName("");

        MessageProto.Message.Builder messageProto = MessageProto.Message.newBuilder();
        messageProto.setSender(sender);
        messageProto.setDate(date);
        messageProto.setTime(time);
        messageProto.setGroup(group);
        messageProto.setContent(content);

        MessageProto.Message message = messageProto.build();

        if (group.equals("")) {
            channel.basicPublish("", recipient, null, message.toByteArray());
        } else {
            channel.basicPublish(group, "", null, message.toByteArray());
        }

    }

    public static void setChannel(Channel channel) {
        Client.channel = channel;
    }

    public static String getUsername() {
        return username;
    }

}