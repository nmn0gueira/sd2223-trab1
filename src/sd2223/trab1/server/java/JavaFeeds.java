package sd2223.trab1.server.java;

import sd2223.trab1.api.Message;
import sd2223.trab1.api.java.Feeds;
import sd2223.trab1.api.java.Result;
import sd2223.trab1.api.java.Result.ErrorCode;
import sd2223.trab1.api.java.Users;
import sd2223.trab1.client.FeedsClientFactory;
import sd2223.trab1.client.UsersClientFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


public class JavaFeeds implements Feeds {

    private static final String AT = "@"; // Used to concatenate name and domain
    private static final int MESSAGE_ID_FACTOR = 256; // Used to generate message id

    private final Map<String, Map<String, Set<String>>> subscribers = new ConcurrentHashMap<>(); // User with subscribers -> Domain -> Set of users from domain
    private final Map<String, Set<String>> subscribedTo = new ConcurrentHashMap<>(); // Users-> Set of users subscribed
    private final Map<String, Map<Long,Message>> personalFeeds = new ConcurrentHashMap<>();
    private final Map<String, Users> userClients = new ConcurrentHashMap<>(); // Domain -> UsersClient
    private final Map<String, Feeds> feedClients = new ConcurrentHashMap<>(); // Domain -> FeedsClient
    private final int serverId;
    private final String serviceDomain;
    private long seqNum = 1;


    private static final Logger Log = Logger.getLogger(JavaFeeds.class.getName());

    public JavaFeeds(int serverId, String serviceDomain) {
        this.serverId = serverId;
        this.serviceDomain = serviceDomain;
    }

    @Override
    public Result<Long> postMessage(String user, String pwd, Message msg)  {
        Log.info("postMessage : user = " + user + "; pwd = " + pwd + "; msg = " + msg);

        // Check if user or message is valid (if it is null or if domain does not match)
        if (user == null || msg == null || !user.split(AT)[1].equals(msg.getDomain())) {
            Log.info("Message object invalid.");
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        Result<Void> res = verifyUser(user, pwd);
        if (!res.isOK())
            return Result.error(res.error());

        msg.setId(seqNum * MESSAGE_ID_FACTOR + serverId); // Formula used to generate new message id
        seqNum++;

        // Add message to own personal feed of user
        personalFeeds.get(user).put(msg.getId(), msg);

        // Propagate messages to subscribers
        new Thread(() -> {
            Set<String> domains = subscribers.get(user).keySet();

            domains.stream()
                    .parallel()
                    .forEach(d -> feedClients
                            .computeIfAbsent(d, k -> FeedsClientFactory.get(d))
                            .addMessageToUsers(msg, String.join(",",subscribers.get(user).get(d))));
        }).start();

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

        String userDomain = user.split(AT)[1];

        // If user is supposed to be in this domain
        if (userDomain.equals(serviceDomain)) {

            Map<Long, Message> messages = personalFeeds.get(user);
            Message msg;
            // This will check if the user exists or if the message does not exist
            if (messages == null || (msg = messages.get(mid)) == null) {
                Log.info("User or message do not exist.");
                return Result.error(ErrorCode.NOT_FOUND);
            }
            return Result.ok(msg);
        }
        //Otherwise, forward request to right domain
        return feedClients
                .computeIfAbsent(userDomain, k -> FeedsClientFactory.get(userDomain))
                .getMessage(user, mid);
    }

    @Override
    public Result<List<Message>> getMessages(String user, long time) {
        Log.info("getMessages : user = " + user + "; time = " + time);

        String userDomain = user.split(AT)[1];

        if (userDomain.equals(serviceDomain)) {

            Map<Long, Message> messages = personalFeeds.get(user);
            if (messages == null) {
                Log.info("User does not exist.");
                return Result.error(ErrorCode.NOT_FOUND);
            }

            List<Message> list = new ArrayList<>();

            for (Message m : messages.values())
                if (m.getCreationTime() > time)
                    list.add(m);

            return Result.ok(list);
        }

        //Otherwise, forward request to right domain
        return feedClients
                .computeIfAbsent(userDomain, k -> FeedsClientFactory.get(userDomain))
                .getMessages(user, time);
    }

    @Override
    public Result<Void> subUser(String user, String userSub, String pwd) {
        Log.info("subUser : user = " + user + "; userSub = " + userSub + "; pwd = " + pwd);

        Result<Void> res1 = verifyUser(user, pwd);
        if (!res1.isOK())
            return Result.error(res1.error());

        // Check if userSub exists (if it is in another domain)
        Result<Void> res2 = verifyUser(userSub, "");
        if (res2.error() == ErrorCode.NOT_FOUND) {
            Log.info("User to subscribe to does not exist.");
            return Result.error(ErrorCode.NOT_FOUND);
        }

        //Propagate subscription
        String userSubDomain = userSub.split(AT)[1];
        new Thread (() -> feedClients
                .computeIfAbsent(userSubDomain, k -> FeedsClientFactory.get(userSubDomain))
                .changeSubStatus(user, userSub, true)).start();

        subscribedTo.get(user).add(userSub);

        return Result.ok();
    }

    @Override
    public Result<Void> unsubscribeUser(String user, String userSub, String pwd) {
        Log.info("unsubscribeUser : user = " + user + "; userSub = " + userSub + "; pwd = " + pwd);

        Result<Void> res1 = verifyUser(user, pwd);
        if (!res1.isOK())
            return Result.error(res1.error());

        // Check if userSub exists (if it is in another domain)
        Result<Void> res2 = verifyUser(userSub, "");
        if (res2.error() == ErrorCode.NOT_FOUND) {
            Log.info("User to unsubscribe from does not exist.");
            return Result.error(ErrorCode.NOT_FOUND);
        }

        String userSubDomain = userSub.split(AT)[1];
        //Propagate unsubscription
        new Thread (() -> feedClients
                .computeIfAbsent(userSubDomain, k -> FeedsClientFactory.get(userSubDomain))
                .changeSubStatus(user, userSub, false)).start();


        subscribedTo.get(user).remove(userSub);

        return Result.ok();
    }

    @Override
    public Result<List<String>> listSubs(String user) {
        Log.info("listSubs : user = " + user);

        Set<String> subs = subscribedTo.get(user);

        if (subs == null) {
            Log.info("User does not exist");
            return Result.error(ErrorCode.NOT_FOUND);
        }

        return Result.ok(new ArrayList<>(subs));
    }

    @Override
    public Result<Void> createFeedInfo(String user) {
        Log.info("createFeed: user = " + user);

        personalFeeds.put(user, new ConcurrentHashMap<>());
        subscribedTo.put(user, new HashSet<>());
        subscribers.put(user, new ConcurrentHashMap<>());
        subscribers.get(user).put(serviceDomain, new HashSet<>()); // Add user's own domain

        return Result.ok();
    }

    @Override
    public Result<Void> deleteFeedInfo(String user) {
        Log.info("deleteFeed: user = " + user);

        personalFeeds.remove(user);

        // Remove this user from subscriber lists of all users he is subscribed to
        for (String u: subscribedTo.get(user)) {                   // ISTO ESTA A APAGAR DOS SUBSCRIBERS QUE ESTAO NOUTRO DOMINIO?
            subscribers.get(u).get(serviceDomain).remove(user);    // TALVEZ PRECISE DE COMPUTE IF PRESENT (CONFIRMAR SE TA CERTO)
        }
        subscribedTo.remove(user);

        // Remove this user from subscriber lists of all users that are subscribed to him
        subscribers.remove(user);

        return Result.ok();
    }

    @Override
    public Result<Void> addMessageToUsers(Message msg, String users) {
        String[] usersToAddMessage = users.split(",");

        Log.info("addMessageToUsers : msg = " + msg + "; users = " + users);
        Log.info ("domain: " + serviceDomain);
        long mid = msg.getId();

        for (String user : usersToAddMessage) {
            Log.info("addMessageToUsers (DEBUG) : user = " + user);
            personalFeeds.get(user).put(mid, msg);
        }

        return Result.ok();
    }

    @Override
    public Result<Void> changeSubStatus(String user, String userSub, boolean subscribing) {
        Log.info("changeSubStatus : user = " + user + "; userSub = " + userSub);

        String userDomain = user.split(AT)[1];
        if (subscribing) {
            subscribers.get(userSub).computeIfAbsent(userDomain, k -> new HashSet<>()).add(user);
        } else {
            subscribers.get(userSub).get(userDomain).remove(user);
        }

        return Result.ok();
    }

    private Result<Void> verifyUser(String user, String pwd) {
        String[] userInfo = user.split(AT);
        String userName = userInfo[0];
        String userDomain = userInfo[1];

        var userClient = userClients.computeIfAbsent(userDomain, k -> UsersClientFactory.get(userDomain));

        var res = userClient.getUser(userName, pwd);
        if (!res.isOK()) {  // If request failed throw given error
            return Result.error(res.error());
        }
        return Result.ok();
    }
}
