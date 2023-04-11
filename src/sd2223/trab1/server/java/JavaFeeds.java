package sd2223.trab1.server.java;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import sd2223.trab1.api.Message;
import sd2223.trab1.api.java.Feeds;
import sd2223.trab1.api.java.Result;
import sd2223.trab1.api.java.Result.ErrorCode;
import sd2223.trab1.api.rest.UsersService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


public class JavaFeeds implements Feeds {

    private final Map<String, Map<Long,String>> subs = new HashMap<>();
    private final Map<String, Map<Long,Message>> personalFeeds = new HashMap<>();
    private Client client;
    private WebTarget target;

    private static final Logger Log = Logger.getLogger(JavaFeeds.class.getName());

    @Override  // Check if user exists through usersResource to check password
    public Result<Long> postMessage(String user, String pwd, Message msg)  {
        Log.info("postMessage : user = " + user + "; pwd = " + pwd + "; msg = " + msg);

        /* HttpURLConnection con = (HttpURLConnection) new URL("http://0.0.0.0:8080/rest/users/" + user + "?pwd=" + pwd).openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", MediaType.APPLICATION_JSON);
        con.setDoOutput(true);
        con.setDoInput(true);
        con.connect();*/

        //ur.getUser(user, pwd);  // Check if user exists and password is correct

        target = client.target("http://0.0.0.0:8080/rest/users/");
        Response r = target.path( user )
                .queryParam(UsersService.PWD, pwd).request()
                .accept(MediaType.APPLICATION_JSON)
                .get();

        if (r.getStatus() == Status.NOT_FOUND.getStatusCode())
            throw new WebApplicationException(Status.NOT_FOUND);

        if (r.getStatus() == Status.FORBIDDEN.getStatusCode())
            throw new WebApplicationException(Status.FORBIDDEN);

        // Check if message is valid (PODE SER QUE NAO SEJA NECESSARIO)
        if (msg == null) {
            Log.info("Message object invalid.");
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        personalFeeds.computeIfAbsent(user, k -> new HashMap<>()).put(msg.getId(), msg);

        return Result.ok(msg.getId());
    }

    @Override
    public Result<Void> removeFromPersonalFeed(String user, long mid, String pwd) {
        Log.info("removeFromPersonalFeed : user = " + user + "; mid = " + mid + "; pwd = " + pwd);

        //ur.getUser(user, pwd);  // Check if user exists and password is correct

        target = client.target("http://0.0.0.0:8080/rest/users/");
        Response r = target.path( user )
                .queryParam(UsersService.PWD, pwd).request()
                .accept(MediaType.APPLICATION_JSON)
                .get();

        if (r.getStatus() == Status.NOT_FOUND.getStatusCode())
            return Result.error(ErrorCode.NOT_FOUND);

        if (r.getStatus() == Status.FORBIDDEN.getStatusCode())
            return Result.error(ErrorCode.FORBIDDEN);

        Map<Long, Message> messages = personalFeeds.get(user);

        // DOUBLE CHECK IF THIS IS CORRECT
        if (messages == null || !messages.remove(mid, messages.get(mid))) {
            Log.info("Message does not exist.");
            return Result.error(ErrorCode.NOT_FOUND);
        }

        return Result.ok();
    }

    @Override
    public Result<Message> getMessage(String user, long mid) {
        Log.info("getMessage : user = " + user + "; mid = " + mid);

        Map<Long, Message> messages = personalFeeds.get(user);

        // DOUBLE CHECK IF THERE IS A BETTER WAY
        if (messages == null) {
            Log.info("User does not exist or has no messages.");
            return Result.error(ErrorCode.NOT_FOUND);
        }
        // DOUBLE CHECK IF THERE IS A BETTER WAY
        Message msg = messages.get(mid);
        if (msg == null) {
            Log.info("Message does not exist.");
            return Result.error(ErrorCode.NOT_FOUND);
        }

        return Result.ok(msg);
    }

    @Override // ESTA ERRADO
    public Result<List> getMessages(String user, long time) {
        Log.info("getMessages : user = " + user + "; time = " + time);

        Map<Long, Message> messages = personalFeeds.get(user);

        if (messages == null) {
            Log.info("User does not exist or has no messages.");
            return Result.error(ErrorCode.NOT_FOUND);
        }

        List<Message> list = new ArrayList<>();

        for (Message m : messages.values())
            if (m.getCreationTime() >= time)
                list.add(m);

        return Result.ok(list);
    }

    @Override
    public Result<Void> subUser(String user, String userSub, String pwd) {
        return null;
    }

    @Override
    public Result<Void> unsubscribeUser(String user, String userSub, String pwd) {
        return null;
    }

    @Override
    public Result<List> listSubs(String user) {
        return null;
    }
}
