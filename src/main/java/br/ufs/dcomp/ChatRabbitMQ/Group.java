package br.ufs.dcomp.ChatRabbitMQ;

import java.io.IOException;

import com.rabbitmq.client.*;

public class Group {

    private static Channel channel;
    private static Connection connection;

    public static void setChannel(Channel channel) {
        Group.channel = channel;
    }

    public static void setConnection(Connection connection) {
        Group.connection = connection;
    }

    public static void addGroup(String group) throws IOException {

        channel.exchangeDeclare(group, "direct", false, false, null);
        channel.queueBind(Client.getUsername() + "-text", group, "t");
        channel.queueBind(Client.getUsername() + "-file", group, "f");
    }

    public static void addUser(String username, String group) throws IOException {
        if (checkIfUserExists(username) && checkIfGroupExists(group)) {
            channel.queueBind(username + "-text", group, "t");
            channel.queueBind(username + "-file", group, "f");
        }
    }

    public static void delFromGroup(String username, String group) throws IOException {
        if (checkIfUserExists(username) && checkIfGroupExists(group)) {
            channel.queueUnbind(username + "-text", group, "t");
            channel.queueUnbind(username + "-file" , group, "f");
        }
    }

    public static void removeGroup(String group) {
        try {
            channel.exchangeDelete(group);
        } catch (IOException e) {
            System.out.println("[!] Erro ao remover grupo \"" + group + "\".");
        }
    }

    public static boolean checkIfGroupExists(String group) throws IOException {

        try {
            channel.exchangeDeclarePassive(group);
            return true;
        } catch (IOException e) {
            System.out.println("[!] O grupo \"" + group + "\" não existe.");
            Channel newChannel = connection.createChannel();
            newChannel.basicConsume(Client.getTextQueueName(), true, Client.getTextConsumer());
            newChannel.basicConsume(Client.getFileQueueName(), true, Client.getFileConsumer());
            Group.channel = newChannel;
            Client.setChannel(newChannel);
            return false;
        }
    }

    private static boolean checkIfUserExists(String username) throws IOException {
        try {
            channel.queueDeclarePassive(username + "-text");
            return true;
        } catch (IOException e) {
            System.out.println("[!] O usuário \"" + username + "\" não existe.");
            Channel newChannel = connection.createChannel();
            newChannel.basicConsume(Client.getTextQueueName(), true, Client.getTextConsumer());
            newChannel.basicConsume(Client.getFileQueueName(), true, Client.getFileConsumer());
            Group.channel = newChannel;
            Client.setChannel(newChannel);
            return false;
        }
    }

}
