package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.HashMap;

import com.google.protobuf.*;

public class Client {

    private static String username;
    private String recipient;
    private static Channel channel;
    private static String textQueueName;
    private static String fileQueueName;
    private static Consumer textConsumer;
    private static Consumer fileConsumer;
    private HashMap<String, Object> queueCreationArgs;

    public Client(String username, Channel channel) throws IOException {
        Client.username = username;
        Client.textQueueName = username + "-text";
        Client.fileQueueName = username + "-file";
        Client.channel = channel;
        queueCreationArgs = new HashMap<>();
        queueCreationArgs.put("x-queue-type", "quorum");
        queueCreationArgs.put("x-quorum-initial-group-size", 3);
        queueCreationArgs.put("x-quorum-group-size", 3);
    }

    public void startClient() throws IOException {

        // (queue-name, durable, exclusive, auto-delete, params);
        if (!Group.checkIfUserExists(username, false, true)) {
            createQueueDefault(textQueueName);
            createQueueDefault(fileQueueName);
        }

        Consumer textConsumer = new DefaultConsumer(channel) {
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                    byte[] body) throws IOException {

                MessageProto.Message message = MessageProto.Message.parseFrom(body);

                if (message.getSender().equals(username)) {
                    return;
                }

                String formattedMessage = formatMessage(message);

                System.out.println(formattedMessage);

                if (!Chat.getPromptText().equals("")) {
                    System.out.print(Chat.getPromptText());
                }

            }
        };

        // (queue-name, autoAck, consumer);
        channel.basicConsume(textQueueName, true, textConsumer);
        Client.textConsumer = textConsumer;

        Consumer fileConsumer = new DefaultConsumer(channel) {
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                    byte[] body) throws IOException {

                MessageProto.Message message = MessageProto.Message.parseFrom(body);
                String sender = message.getSender();

                if (message.getSender().equals(username)) {
                    return;
                }

                // refatorar num método de formatar mensagem de arquivo recebido
                String date = message.getDate();
                String time = message.getTime();
                String group = message.getGroup();

                MessageProto.Content content = message.getContent();
                byte[] file = content.getBody().toByteArray();
                String fileName = content.getName();

                String defaultOutputPath = "/home/alvaro022/files";

                try {
                    Path outputPath = Paths.get(defaultOutputPath, fileName);

                    if (!Files.exists(outputPath.getParent())) {
                        Files.createDirectories(outputPath.getParent());
                    }

                    Files.write(outputPath, file);
                    String messageToPrint;
                    if (group.equals("")) {

                        messageToPrint = MessageFormat.format("\n({0} às {1}) Arquivo {2} recebido de @{3}!",
                                date, time, fileName, sender);
                    } else {
                        messageToPrint = MessageFormat.format("\n({0} às {1}) Arquivo {2} recebido do grupo @{3}!",
                                date, time, fileName, group);
                    }
                    System.out.println(messageToPrint);
                    System.out.println("Arquivo salvo em: " + outputPath);
                } catch (IOException e) {
                    // e.printStackTrace();
                    System.err.println("Erro ao salvar o arquivo " + e.getMessage());
                }

                if (!Chat.getPromptText().equals("")) {
                    System.out.print(Chat.getPromptText());
                }

            }
        };

        channel.basicConsume(fileQueueName, true, fileConsumer);
        Client.fileConsumer = fileConsumer;

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
        if (!Group.checkIfUserExists(recipient, false, false)) {
            createQueueDefault(recipient + "-text");
            createQueueDefault(recipient + "-file");
        }
    }

    public void sendMessage(String body, String sender, String group) throws UnsupportedEncodingException, IOException {

        String date = Utils.getFormattedDate();
        String time = Utils.getFormattedTime();

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
            channel.basicPublish("", recipient + "-text", null, message.toByteArray());
        } else {
            channel.basicPublish(group, "t", null, message.toByteArray());
        }

    }

    public void sendFile(String filePath, String currentUser, String currentRecipient, String currentGroup) {
        new FileSender(filePath, currentUser, currentRecipient, currentGroup)
                .start();
    }

    private void createQueueDefault(String queueName) throws IOException {
        channel.queueDeclare(queueName, true, false, false, queueCreationArgs);
    }

    public static void setChannel(Channel channel) {
        Client.channel = channel;
    }

    public static Channel getChannel() {
        return Client.channel;
    }

    public static String getUsername() {
        return username;
    }

    public static String getTextQueueName() {
        return textQueueName;
    }

    public static String getFileQueueName() {
        return fileQueueName;
    }

    public static Consumer getTextConsumer() {
        return textConsumer;
    }

    public static Consumer getFileConsumer() {
        return fileConsumer;
    }

}