package br.ufs.dcomp.ChatRabbitMQ;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.io.IOException;

import com.google.protobuf.ByteString;
import com.rabbitmq.client.Channel;

public class FileSender extends Thread {

    private String filePath;
    private String sender;
    private String recipient;
    private String group;
    private Channel channel;

    public FileSender(String filePath, String sender, String recipient, String group) {
        this.filePath = filePath;
        this.sender = sender;
        this.recipient = recipient;
        this.group = group;
        try {
            this.channel = Chat.getConnection().createChannel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendFile(String filePath, String sender, String recipient) throws IOException, Exception {
        String date = Utils.getFormattedDate();
        String time = Utils.getFormattedTime();

        Path source = Paths.get(filePath);
        String mimeType = Files.probeContentType(source);
        if (mimeType == null) {
            throw new Exception("Arquivo inexistente ou caminho incorreto.");
        }
        String[] folders = filePath.split("/");
        String name = folders[folders.length - 1];

        MessageProto.Content.Builder content = MessageProto.Content.newBuilder();
        content.setType(mimeType);
        content.setName(name);
        try {
            byte[] body = Files.readAllBytes(new File(filePath).toPath());
            content.setBody(ByteString.copyFrom(body));
        } catch (IOException e) {
            // e.printStackTrace();
            throw new Exception("Erro ao ler o arquivo:"  + e.getMessage());
        }

        MessageProto.Message.Builder messageProto = MessageProto.Message.newBuilder();
        messageProto.setSender(sender);
        messageProto.setDate(date);
        messageProto.setTime(time);
        messageProto.setGroup(group);
        messageProto.setContent(content);

        MessageProto.Message message = messageProto.build();

        if (group.equals("")) {
            channel.basicPublish("", recipient + "-file", null, message.toByteArray());
        } else {
            channel.basicPublish(group, "f", null, message.toByteArray());
        }

    }

    public void run() {
        try {
            String[] folders = filePath.split("/");
            String fileName = folders[folders.length - 1];
            if (group.equals("")) {
                System.out.println("Enviando \"" + fileName + "\" para @" + recipient);
            } else {
                System.out.println("Enviando \"" + fileName + "\" para o grupo " + group);
            }
            System.out.print(Chat.getPromptText());

            sendFile(filePath, sender, recipient);

            if (group.equals("")) {
                System.out.println("Arquivo \"" + fileName + "\" foi enviado para @" + recipient);
            } else {

                System.out.println("Arquivo \"" + fileName + "\" foi enviado para o grupo " + group);
            }
            System.out.print(Chat.getPromptText());


        } catch (Exception e) {
            // System.out.println("Ocorreu um erro ao enviar o arquivo: " + e);
            System.out.println(e.getMessage());
            System.out.print(Chat.getPromptText());

        }
    }

}
