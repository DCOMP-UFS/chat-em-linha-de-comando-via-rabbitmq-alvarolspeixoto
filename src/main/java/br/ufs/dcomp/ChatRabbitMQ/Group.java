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

    private static Response makeHttpRequest(String path) {

        try {

            String usernameAndPassword = Chat.getUser() + ":" + Chat.getPassword();
            String authorizationHeaderName = "Authorization";
            String authorizationHeaderValue = "Basic "
                    + java.util.Base64.getEncoder().encodeToString(usernameAndPassword.getBytes());

            // Perform a request
            String restResource = "http://rabbitmq-load-balancer-b6fe6ab2cdb806fd.elb.us-east-1.amazonaws.com";
            javax.ws.rs.client.Client client = ClientBuilder.newClient();
            Response response = client.target(restResource)
                    // .path("/api/exchanges/%2f/ufs/bindings/source") // lista todos os binds que
                    // tem o exchange "ufs" como source
                    .path(path)
                    .request(MediaType.APPLICATION_JSON)
                    .header(authorizationHeaderName, authorizationHeaderValue)                                                          // // here
                    .get();
            return response;
        } // Perform a post with the form values
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    public static void listUsers(String group) {

        Response response = makeHttpRequest("/api/exchanges/%2F/" + group + "/bindings/source");

        if (response == null) {
            return;
        }

        if (response.getStatus() == 200) {
            String json = response.readEntity(String.class);
            JSONArray jsonArr = new JSONArray(json);

            List<String> users = new ArrayList<>();

            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject obj = jsonArr.getJSONObject(i);

                String currentUser = obj.getString("destination").split("-")[0];

                if (!users.contains(currentUser))
                    users.add(currentUser);

            }

            String usersStr = Group.formatListInStr(users);


            System.out.println(usersStr);

        } else {
            System.out.println(response.getStatus());
        }
    }

    public static void listGroups() {

        Response response = makeHttpRequest("/api/queues/%2F/" + Chat.getCurrentUser() + "-text" + "/bindings");

        if (response == null) {
            return;
        }

        if (response.getStatus() == 200) {
            String json = response.readEntity(String.class);
            JSONArray jsonArr = new JSONArray(json);

            List<String> groups = new ArrayList<>();

            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject obj = jsonArr.getJSONObject(i);

                String currentGroup = obj.getString("source").split("-")[0];

                if (!currentGroup.equals(""))
                    groups.add(currentGroup);

            }

            String groupsStr = Group.formatListInStr(groups);
            System.out.println(groupsStr);

        } else {
            System.out.println(response.getStatus());
        }

    }

    private static String formatListInStr(List<String> list) {
        return list.size() == 0 ? "[!] Esse usuário não faz parte de nenhum grupo" :
        String.join(", ", list);
    }

}
