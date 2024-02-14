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
        channel.queueBind("text-" + Client.getUsername(), group, "t");
        channel.queueBind("file-" + Client.getUsername(), group, "f");
    }

    public static void addUser(String username, String group) throws IOException {
        if (checkIfUserExists(username) && checkIfGroupExists(group)) {
            channel.queueBind("text-" + username, group, "t");
            channel.queueBind("file-" + username, group, "f");
        }
    }

    public static void delFromGroup(String username, String group) throws IOException {
        if (checkIfUserExists(username) && checkIfGroupExists(group)) {
            channel.queueUnbind("text-" + username, group, "t");
            channel.queueUnbind("file-" + username, group, "f");
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
            Group.channel = newChannel;
            Client.setChannel(newChannel);
            return false;
        }
    }

    private static boolean checkIfUserExists(String username) throws IOException {
        try {
            channel.queueDeclarePassive("text-" + username);
            return true;
        } catch (IOException e) {
            System.out.println("[!] O usuário \"" + username + "\" não existe.");
            Channel newChannel = connection.createChannel();
            Group.channel = newChannel;
            Client.setChannel(newChannel);
            return false;
        }
    }

}
