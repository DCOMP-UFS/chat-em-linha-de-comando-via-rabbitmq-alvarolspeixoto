package br.ufs.dcomp.ChatRabbitMQ;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
            channel.queueUnbind(username + "-file", group, "f");
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

    public static void listUsers(String group) {

        try {

            String usernameAndPassword = Chat.getUser() + ":" + Chat.getPassword();
            String authorizationHeaderName = "Authorization";
            String authorizationHeaderValue = "Basic "
                    + java.util.Base64.getEncoder().encodeToString(usernameAndPassword.getBytes());

            // Perform a request
            String restResource = "http://rabbitmq-load-balancer-b6fe6ab2cdb806fd.elb.us-east-1.amazonaws.com";
            javax.ws.rs.client.Client client = ClientBuilder.newClient();
            Response resposta = client.target(restResource)
                    // .path("/api/exchanges/%2f/ufs/bindings/source") // lista todos os binds que
                    // tem o exchange "ufs" como source
                    .path("/api/exchanges/%2F/" + group + "/bindings/source")
                    .request(MediaType.APPLICATION_JSON)
                    .header(authorizationHeaderName, authorizationHeaderValue) // The basic authentication header goes
                                                                               // here
                    .get(); // Perform a post with the form values

            if (resposta.getStatus() == 200) {
                String json = resposta.readEntity(String.class);
                JSONArray jsonArr = new JSONArray(json);
                
                List<String> users = new ArrayList<>();

                for (int i = 0; i < jsonArr.length(); i++) {
                    JSONObject obj = jsonArr.getJSONObject(i);

                    String currentUser = obj.getString("destination").split("-")[0];

                    if (!users.contains(currentUser)) {
                        users.add(currentUser);
                    }

                }

                String usersStr = String.join(", ", users);

                System.out.println(usersStr);

            } else {
                System.out.println(resposta.getStatus());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void listGroups() {

    }

}
