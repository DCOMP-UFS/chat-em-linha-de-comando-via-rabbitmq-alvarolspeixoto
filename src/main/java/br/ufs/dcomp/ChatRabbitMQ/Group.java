package br.ufs.dcomp.ChatRabbitMQ;

import java.io.IOException;

import com.rabbitmq.client.*;

public class Group {

    private static Channel channel;

    public static void setChannel(Channel channel) {
        Group.channel = channel;
    }

    public static void addGroup(String group, String creator) throws IOException {

        channel.exchangeDeclare(group, "fanout", false, false, null);
        channel.queueBind(creator, group, "");
    }

    public static void addUser(String group, String username) throws IOException {
        channel.queueBind(username, group, "");
    }

    public static void sendMessage(String group, String sender, String message) throws IOException {
        // temporário
        message = "Mensagem do grupo " + group + " enviada por " + sender + ": " + message;
        channel.basicPublish(group, "", null, message.getBytes("UTF-8"));
    }

    public static void delFromGroup(String group, String username) throws IOException {
        channel.queueUnbind(username, group, "");
    }

    public static void removeGroup(String group) throws IOException {
        channel.exchangeDelete(group);
    }

}
