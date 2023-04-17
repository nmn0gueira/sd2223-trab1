package sd2223.trab1.server.java;

import sd2223.trab1.api.Message;
import sd2223.trab1.api.java.Feeds;
import sd2223.trab1.api.java.Result;
import sd2223.trab1.api.java.Result.ErrorCode;
import sd2223.trab1.client.UsersClientFactory;
import sd2223.trab1.server.util.Discovery;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


public class JavaFeeds implements Feeds {

    private final Map<String, List<String>> subscribers = new HashMap<>(); // Users that follow a given user
    private final Map<String, List<String>> subscribedTo = new HashMap<>(); // Users that a given user is subscribed to
    private final Map<String, Map<Long,Message>> personalFeeds = new HashMap<>();
    private final Discovery discovery = Discovery.getInstance();
    private final int serverId;
    private long seqNum = 1;


    private static final Logger Log = Logger.getLogger(JavaFeeds.class.getName());

    public JavaFeeds(int serverId) {
        this.serverId = serverId;
    }

    @Override  // Check if user exists through usersResource to check password ADICIONAR AOS FEEDS PESSOAIS DOS SUBSCRIBERS
    public Result<Long> postMessage(String user, String pwd, Message msg)  {
        Log.info("postMessage : user = " + user + "; pwd = " + pwd + "; msg = " + msg);

        Result<Void> res = verifyUser(user, pwd);
        if (!res.isOK())
            return Result.error(res.error());
        // Check if message is valid
        if (msg == null) {
            Log.info("Message object invalid.");
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        msg.setId(seqNum * 256 + serverId); // Set new message id
        seqNum++;

        // Add message to own personal feed of user
        personalFeeds.computeIfAbsent(user, k -> new HashMap<>()).put(msg.getId(), msg);

        // Add message to personal feeds of subscribers (PODE PROPAGAR A MENSAGEM PARA USERS FORA DO DOMINIO)
        List<String> subs = subscribers.get(user);
        if (subs != null) {
            for (String s : subs) {
                personalFeeds.computeIfAbsent(s, k -> new HashMap<>()).put(msg.getId(), msg);
            }
        }

        return Result.ok(msg.getId());
    }

    @Override
    public Result<Void> removeFromPersonalFeed(String user, long mid, String pwd) {
        Log.info("removeFromPersonalFeed : user = " + user + "; mid = " + mid + "; pwd = " + pwd);

        Result<Void> res = verifyUser(user, pwd);
        if (!res.isOK())
            return Result.error(res.error());

        Map<Long, Message> messages = personalFeeds.get(user);

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
        Message msg;

        // This will check if the user exists/has no messages or if the message does not exist
        if (messages == null || (msg = messages.get(mid)) == null) {
            Log.info("User or message do not exist.");
            return Result.error(ErrorCode.NOT_FOUND);
        }

        return Result.ok(msg);
    }

    @Override // ESTA ERRADO
    public Result<List<Message>> getMessages(String user, long time) {
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
        Log.info("subUser : user = " + user + "; userSub = " + userSub + "; pwd = " + pwd);

        Result<Void> res = verifyUser(user, pwd);
        if (!res.isOK())
            return Result.error(res.error());

        // Check if userSub exists (CAN BE OUSTIDE DOMAIN)
        if (!personalFeeds.containsKey(userSub)) {
            Log.info("User to subscribe to does not exist.");
            return Result.error(ErrorCode.NOT_FOUND);
        }

        // Add user subscription
        subscribers.computeIfAbsent(userSub, k -> new ArrayList<>()).add(user);
        subscribedTo.computeIfAbsent(user, k -> new ArrayList<>()).add(userSub);

        return Result.ok();
    }

    @Override
    public Result<Void> unsubscribeUser(String user, String userSub, String pwd) {
        Log.info("unsubscribeUser : user = " + user + "; userSub = " + userSub + "; pwd = " + pwd);
        return null;
    }

    @Override
    public Result<List<String>> listSubs(String user) {
        Log.info("listSubs : user = " + user);

        List<String> subs = subscribedTo.get(user);

        if (subs == null) {
            Log.info("User does not exist");
            return Result.error(ErrorCode.NOT_FOUND);
        }

        return Result.ok(subs);
    }

    @Override
    public Result<Void> createFeed(String user) {
        return null;
    }

    @Override
    public Result<Void> deleteFeed(String user) {
        return null;
    }

    private Result<Void> verifyUser(String user, String pwd) {
        String[] userInfo = user.split("@");
        String userName = userInfo[0];
        String domain = userInfo[1];
        URI uri = discovery.knownUrisOf("users".concat("." + domain), 1)[0];

        var users = UsersClientFactory.get(uri);

        var res = users.verifyPassword(userName, pwd);
        if (!res.isOK()) {  // If request failed throw given error
            return Result.error(res.error());
        }
        return Result.ok();
    }
}
