package sd2223.trab1.server.java;

import sd2223.trab1.api.Message;
import sd2223.trab1.api.java.Feeds;
import sd2223.trab1.api.java.Result;
import sd2223.trab1.api.java.Result.ErrorCode;
import sd2223.trab1.api.java.Users;
import sd2223.trab1.client.FeedsClientFactory;
import sd2223.trab1.client.UsersClientFactory;
import sd2223.trab1.server.util.Discovery;

import java.net.URI;
import java.util.*;
import java.util.logging.Logger;


public class JavaFeeds implements Feeds {

    private final Map<String, Map<String, Set<String>>> subscribers = new HashMap<>(); // User with subscribers -> Domain -> Set of users from domain
    private final Map<String, Set<String>> subscribedTo = new HashMap<>(); // Users-> Set of users subscribed
    private final Map<String, Map<Long,Message>> personalFeeds = new HashMap<>();
    private final Map<String, Users> userClients = new HashMap<>();
    private final Map<String, Feeds> feedClients = new HashMap<>();
    private final Discovery discovery = Discovery.getInstance();
    private final int serverId;
    private final String domainName;
    private long seqNum = 1;


    private static final Logger Log = Logger.getLogger(JavaFeeds.class.getName());

    public JavaFeeds(int serverId, String domainName) {
        this.serverId = serverId;
        this.domainName = domainName;
    }

    @Override
    public Result<Long> postMessage(String user, String pwd, Message msg)  {
        Log.info("postMessage : user = " + user + "; pwd = " + pwd + "; msg = " + msg);

        // Check if user or message is valid (if it is null or if domain does not match)
        if (user == null || msg == null || !msg.getDomain().equals(user.split("@")[1])) {
            Log.info("Message object invalid.");
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        Result<Void> res = verifyUser(user, pwd);
        if (!res.isOK())
            return Result.error(res.error());

        msg.setId(seqNum * 256 + serverId); // Set new message id
        seqNum++;

        // Add message to own personal feed of user
        personalFeeds.get(user).put(msg.getId(), msg);

        // Add message to personal feeds of subscribers
        String domain = user.split("@")[1];
        Set<String> subs = subscribers.get(user).get(domain);
        for (String s : subs) {
            personalFeeds.get(s).put(msg.getId(), msg);
        }
        propagateMessage(msg);  // Propagate message to other servers if needed

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

        // This will check if the user exists or if the message does not exist
        /*if (messages == null || (msg = messages.get(mid)) == null) {
            Log.info("User or message do not exist.");
            return Result.error(ErrorCode.NOT_FOUND);
        }*/

        String domain = user.split("@")[1];

        if (domain.equals(domainName)) {
            if (messages == null || (msg = messages.get(mid)) == null) {
                Log.info("User or message do not exist.");
                return Result.error(ErrorCode.NOT_FOUND);
            }
            return Result.ok(msg);
        }
        URI uri = discovery.knownUrisOf("feeds".concat("." + domain), 1)[0];
        return FeedsClientFactory.get(uri).getMessage(user, mid);
    }

    @Override
    public Result<List<Message>> getMessages(String user, long time) {
        Log.info("getMessages : user = " + user + "; time = " + time);

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

    @Override
    public Result<Void> subUser(String user, String userSub, String pwd) {
        Log.info("subUser : user = " + user + "; userSub = " + userSub + "; pwd = " + pwd);

        Result<Void> res = verifyUser(user, pwd);
        if (!res.isOK())
            return Result.error(res.error());

        String userDomain = user.split("@")[1];
        String userSubDomain = userSub.split("@")[1];
        if (userSubDomain.equals(userDomain)) {
            // Check if userSub exists (if it is in this domain)
            if (subscribers.get(userSub) == null) {
                Log.info("User to subscribe to does not exist.");
                return Result.error(ErrorCode.NOT_FOUND);
            }

            subscribers.get(userSub).computeIfAbsent(userSubDomain, k -> new HashSet<>()).add(user);

        } else {
            // Check if userSub exists (if it is in another domain)
            Result<Void> res2 = verifyUser(userSub, "");
            if (res2.error() == ErrorCode.NOT_FOUND) {
                Log.info("User to subscribe to does not exist.");
                return Result.error(ErrorCode.NOT_FOUND);
            }

            //Propagate unsubscription to other server
            propagateSubChange(user, userSub, true);

        }

        subscribedTo.get(user).add(userSub);

        return Result.ok();
    }

    @Override
    public Result<Void> unsubscribeUser(String user, String userSub, String pwd) {
        Log.info("unsubscribeUser : user = " + user + "; userSub = " + userSub + "; pwd = " + pwd);

        Result<Void> res1 = verifyUser(user, pwd);
        if (!res1.isOK())
            return Result.error(res1.error());

        String userDomain = user.split("@")[1];
        String userSubDomain = userSub.split("@")[1];
        if (userSubDomain.equals(userDomain)) {
            // Check if userSub exists (if it is in this domain)
            if (subscribers.get(userSub) == null) {
                Log.info("User to unsubscribe from does not exist.");
                return Result.error(ErrorCode.NOT_FOUND);
            }

            subscribers.get(userSub).get(userDomain).remove(user);

        } else {
            // Check if userSub exists (if it is in another domain)
            Result<Void> res2 = verifyUser(userSub, "");
            if (res2.error() == ErrorCode.NOT_FOUND) {
                Log.info("User to unsubscribe from does not exist.");
                return Result.error(ErrorCode.NOT_FOUND);
            }

            //Propagate unsubscription to other server
            propagateSubChange(user, userSub, false);

        }

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

        personalFeeds.put(user, new HashMap<>());
        subscribedTo.put(user, new HashSet<>());
        subscribers.put(user, new HashMap<>());
        subscribers.get(user).put(user.split("@")[1], new HashSet<>()); // Add user's own domain

        return Result.ok();
    }

    @Override
    public Result<Void> deleteFeedInfo(String user) {
        Log.info("deleteFeed: user = " + user);

        personalFeeds.remove(user);
        subscribedTo.remove(user);
        subscribers.remove(user);
        String userDomain = user.split("@")[1];
        for (String u: subscribedTo.keySet()) {
            subscribers.get(u).get(userDomain).remove(user);    // TALVEZ PRECISE DE COMPUTE IF PRESENT (CONFIRMAR SE TA CERTO)
        }

        return Result.ok();
    }


    private void propagateMessage(Message msg) {
        Log.info("propagateMessage : msg = " + msg);

        String domain = msg.getDomain();
        String user = msg.getUser().concat("@" + domain);

        for (String d: subscribers.get(user).keySet()) {
            if (!d.equals(domain)) {
                URI uri = discovery.knownUrisOf("feeds".concat("." + d), 1)[0];
                FeedsClientFactory.get(uri).addMessage(msg);
            }
        }

    }

    private void propagateSubChange(String user, String userSub, boolean subscribing) {
        Log.info("propagateSubChange : user = " + user + "; userSub = " + userSub);

        String userSubDomain = userSub.split("@")[1];
        URI uri = discovery.knownUrisOf("feeds".concat("." + userSubDomain), 1)[0];
        FeedsClientFactory.get(uri).changeSubStatus(user, userSub, subscribing);


    }

    @Override
    public Result<Void> addMessage(Message msg) {
        Log.info("addMessage : msg = " + msg);

        String poster = msg.getUser() + "@" + msg.getDomain();
        long mid = msg.getId();

        for (String user : personalFeeds.keySet()) {
            if (subscribedTo.get(user).contains(poster)) {
                personalFeeds.get(user).put(mid, msg);
                Log.info(user + " is subscribed to " + poster + "; message added");
            }
        }

        return Result.ok();
    }

    @Override
    public Result<Void> changeSubStatus(String user, String userSub, boolean subscribing) {
        Log.info("changeSubStatus : user = " + user + "; userSub = " + userSub);

        String userDomain = user.split("@")[1];
        if (subscribing) {
            subscribers.get(userSub).computeIfAbsent(userDomain, k -> new HashSet<>()).add(user);
            Log.info("User " + user + " subscribed to " + userSub + " in domain " + userSub.split("@")[1]);
        } else {
            subscribers.get(userSub).get(userDomain).remove(user);
            Log.info("User " + user + " unsubscribed from " + userSub + " in domain " + userSub.split("@")[1]);
        }

        return Result.ok();
    }

    private Result<Void> verifyUser(String user, String pwd) {
        String[] userInfo = user.split("@");
        String userName = userInfo[0];
        String domain = userInfo[1];
        String serviceNameAndDomain = "users".concat("." + domain);
        var userClient = userClients.computeIfAbsent(domain, k -> UsersClientFactory.get(discovery.knownUrisOf(serviceNameAndDomain, 1)[0]));

        var res = userClient.verifyPassword(userName, pwd);
        if (!res.isOK()) {  // If request failed throw given error
            return Result.error(res.error());
        }
        return Result.ok();
    }
}
