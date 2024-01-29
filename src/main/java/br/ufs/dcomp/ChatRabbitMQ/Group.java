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

        channel.exchangeDeclare(group, "fanout", false, false, null);
        channel.queueBind(Client.getUsername(), group, "");
    }

    public static void addUser(String username, String group) {
        try {
            channel.queueBind(username, group, "");
        } catch (IOException e) {
            System.out.println("[!] Erro ao adicionar usuário \"" + username + "\". Usuário/grupo inválidos.");
        }
    }

    public static void sendMessage(String group, String sender, String message) throws IOException {
        // temporário
        message = "Mensagem do grupo " + group + " enviada por " + sender + ": " + message;
        channel.basicPublish(group, "", null, message.getBytes("UTF-8"));
    }

    public static void delFromGroup(String username, String group) {
        try {
            channel.queueUnbind(username, group, "");
        } catch (IOException e) {
            System.out.println("[!] Erro ao remover usuário \"" + username + "\" do grupo \"" + group + "\".");
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

}
