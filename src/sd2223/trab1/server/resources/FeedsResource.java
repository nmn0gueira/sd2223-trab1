package sd2223.trab1.server.resources;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import sd2223.trab1.api.Message;
import sd2223.trab1.api.rest.FeedsService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class FeedsResource implements FeedsService {

    private final Map<String, Map<Long,String>> subs;
    private final Map<String, Map<Long,Message>> personalFeeds;
    private final UsersResource ur;

    private static Logger Log = Logger.getLogger(FeedsResource.class.getName());

    public FeedsResource() {
        subs = new HashMap<>(); // Isto começa sem os users definidos
        personalFeeds = new HashMap<>(); // Isto começa sem os users definidos
        ur = new UsersResource(); // N sei se isto esta certo desta maneira
    }

    @Override  // Check if user exists through usersResource to check password
    public long postMessage(String user, String pwd, Message msg) {
        Log.info("postMessage : user = " + user + "; pwd = " + pwd + "; msg = " + msg);

        ur.getUser(user, pwd);  // Check if user exists and password is correct

        // Check if message is valid (PODE SER QUE NAO SEJA NECESSARIO)
        if (msg == null) {
            Log.info("Message object invalid.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        personalFeeds.computeIfAbsent(user, k -> new HashMap<>()).put(msg.getId(), msg);

        return msg.getId();
    }

    @Override
    public void removeFromPersonalFeed(String user, long mid, String pwd) {
        Log.info("removeFromPersonalFeed : user = " + user + "; mid = " + mid + "; pwd = " + pwd);

        ur.getUser(user, pwd);  // Check if user exists and password is correct

        Map<Long, Message> messages = personalFeeds.get(user);

        // DOUBLE CHECK IF THIS IS CORRECT
        if (messages == null || !messages.remove(mid, messages.get(mid))) {
            Log.info("Message does not exist.");
            throw new WebApplicationException(Status.NOT_FOUND);
        }
    }

    @Override
    public Message getMessage(String user, long mid) {
        Log.info("getMessage : user = " + user + "; mid = " + mid);

        Map<Long, Message> messages = personalFeeds.get(user);

        // DOUBLE CHECK IF THERE IS A BETTER WAY
        if (messages == null) {
            Log.info("User does not exist or has no messages.");
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        // DOUBLE CHECK IF THERE IS A BETTER WAY
        Message msg = messages.get(mid);
        if (msg == null) {
            Log.info("Message does not exist.");
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        return msg;
    }

    @Override // ESTA ERRADO
    public List<Message> getMessages(String user, long time) {
        Log.info("getMessages : user = " + user + "; time = " + time);

        Map<Long, Message> messages = personalFeeds.get(user);

        if (messages == null) {
            Log.info("User does not exist or has no messages.");
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        return null;
    }

    @Override
    public void subUser(String user, String userSub, String pwd) {

    }

    @Override
    public void unsubscribeUser(String user, String userSub, String pwd) {

    }

    @Override
    public List<String> listSubs(String user) {
        return null;
    }
}
