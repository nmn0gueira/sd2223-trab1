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
import java.util.stream.Collectors;


public class JavaFeeds implements Feeds {

    private static final String DOMAIN_SEPARATOR = "@"; // Used to concatenate name and domain
    private static final int USER_NAME_INDEX = 0;
    private static final int DOMAIN_NAME_INDEX = 1;
    private static final int MESSAGE_ID_FACTOR = 256; // Used to generate message id

    private final Map<String, Map<String, Set<String>>> subscribers = new ConcurrentHashMap<>(); // User with subscribers -> Domain -> Set of subscribers in domain
    private final Map<String, Map<String, Set<String>>> subscriptions = new ConcurrentHashMap<>(); // Users -> Domain -> Set of users subscribed in domain
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
        if (user == null || msg == null || !user.split(DOMAIN_SEPARATOR)[DOMAIN_NAME_INDEX].equals(msg.getDomain())) {
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
            Map<String, Set<String>> subsByDomain = subscribers.get(user);
            Set<String> domains = subsByDomain.keySet();

            domains.stream()
                    .parallel()
                    .forEach(d -> feedClients
                            .computeIfAbsent(d, k -> FeedsClientFactory.get(d))
                            .addMessageToUsers(msg, String.join(",",subsByDomain.get(d))));
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

        String userDomain = user.split(DOMAIN_SEPARATOR)[DOMAIN_NAME_INDEX];

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

        String userDomain = user.split(DOMAIN_SEPARATOR)[DOMAIN_NAME_INDEX];

        if (userDomain.equals(serviceDomain)) {

            Map<Long, Message> messages = personalFeeds.get(user);
            if (messages == null) {
                Log.info("User does not exist.");
                return Result.error(ErrorCode.NOT_FOUND);
            }

            List<Message> list = new LinkedList<>();

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
        String userSubDomain = userSub.split(DOMAIN_SEPARATOR)[DOMAIN_NAME_INDEX];
        new Thread (() -> feedClients
                .computeIfAbsent(userSubDomain, k -> FeedsClientFactory.get(userSubDomain))
                .changeSubStatus(user, userSub, true)).start();

        subscriptions.get(user).computeIfAbsent(userSubDomain, k -> new HashSet<>()).add(userSub);

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

        String userSubDomain = userSub.split(DOMAIN_SEPARATOR)[DOMAIN_NAME_INDEX];
        //Propagate unsubscription
        new Thread (() -> feedClients
                .computeIfAbsent(userSubDomain, k -> FeedsClientFactory.get(userSubDomain))
                .changeSubStatus(user, userSub, false)).start();


        subscriptions.get(user).get(userSubDomain).remove(userSub);

        return Result.ok();
    }

    @Override
    public Result<List<String>> listSubs(String user) {
        Log.info("listSubs : user = " + user);

        Map<String, Set<String>> subs = subscriptions.get(user);

        if (subs == null) {
            Log.info("User does not exist");
            return Result.error(ErrorCode.NOT_FOUND);
        }

        // Convert all the sets of subscribers into a single list
        return Result.ok(new LinkedList<>(subs.values().stream().flatMap(Collection::stream).collect(Collectors.toSet())));
    }

    @Override
    public Result<Void> createFeedInfo(String user) {
        Log.info("createFeed: user = " + user);

        personalFeeds.put(user, new HashMap<>());
        subscriptions.put(user, new HashMap<>());
        subscribers.put(user, new HashMap<>());

        return Result.ok();
    }

    @Override
    public Result<Void> deleteFeedInfo(String user) {
        Log.info("deleteFeed: user = " + user);

        personalFeeds.remove(user);

        new Thread(() -> {
            // Remove this user from subscriber lists of all users they were subscribed to
            Map<String, Set<String>> subsByDomain = subscribers.remove(user);
            Set<String> domains1 = subsByDomain.keySet();


            domains1.stream()
                    .parallel()
                    .forEach(d -> feedClients
                            .computeIfAbsent(d, k -> FeedsClientFactory.get(d))
                            .removeUserFromSubscribers(user, String.join(",",subsByDomain.get(d))));

            // Remove this user from subscription lists of all users that are subscribed to him
            Map<String, Set<String>> subbedToByDomain = subscriptions.remove(user);
            Set<String> domains2 = subbedToByDomain.keySet();


            domains2.stream()
                    .parallel()
                    .forEach(d -> feedClients
                            .computeIfAbsent(d, k -> FeedsClientFactory.get(d))
                            .removeUserFromSubscriptions(user, String.join(",",subbedToByDomain.get(d))));
        }).start();

        return Result.ok();
    }

    @Override
    public Result<Void> addMessageToUsers(Message msg, String users) {

        // Check if there are users to add message to
        String[] usersToAddMessage = users.isEmpty() ? new String[0] : users.split(",");

        Log.info("addMessageToUsers : msg = " + msg + "; users = " + users);
        long mid = msg.getId();

        for (String user : usersToAddMessage) {
            Log.info("addMessageToUsers (DEBUG) : user = " + user);
            personalFeeds.get(user).put(mid, msg);
        }

        return Result.ok();
    }

    @Override
    public Result<Void> removeUserFromSubscribers(String userRem, String users) {
        Log.info("removeUserFromSubscribers : user = " + userRem + "; users = " + users);

        // All users subscribed to userRem will no longer include userRem in their subscription lists
        return removeUserFromMap(userRem, users, subscriptions);
    }

    @Override
    public Result<Void> removeUserFromSubscriptions(String userRem, String users) {
        Log.info("removeUserFromSubscriptions : user = " + userRem + "; users = " + users);

        // All users that userRem was subscribed to will no longer include userRem in their subscriber lists
        return removeUserFromMap(userRem, users, subscribers);
    }

    @Override
    public Result<Void> changeSubStatus(String user, String userSub, boolean subscribing) {
        Log.info("changeSubStatus : user = " + user + "; userSub = " + userSub);

        String userDomain = user.split(DOMAIN_SEPARATOR)[DOMAIN_NAME_INDEX];
        if (subscribing) {
            subscribers.get(userSub).computeIfAbsent(userDomain, k -> new HashSet<>()).add(user);
        } else {
            subscribers.get(userSub).get(userDomain).remove(user);
        }

        return Result.ok();
    }

    private Result<Void> verifyUser(String user, String pwd) {
        String[] userInfo = user.split(DOMAIN_SEPARATOR);
        String userName = userInfo[USER_NAME_INDEX];
        String userDomain = userInfo[DOMAIN_NAME_INDEX];

        var userClient = userClients.computeIfAbsent(userDomain, k -> UsersClientFactory.get(userDomain));

        var res = userClient.getUser(userName, pwd);
        if (!res.isOK()) {  // If request failed throw given error
            return Result.error(res.error());
        }
        return Result.ok();
    }

    private Result<Void> removeUserFromMap(String userRem, String users, Map<String, Map<String, Set<String>>> map) {
        String[] usersToUpdate = users.isEmpty() ? new String[0] : users.split(",");
        String domain = userRem.split(DOMAIN_SEPARATOR)[DOMAIN_NAME_INDEX];

        for (String u : usersToUpdate) {
            map.get(u).get(domain).remove(userRem);
        }

        return Result.ok();
    }
}
