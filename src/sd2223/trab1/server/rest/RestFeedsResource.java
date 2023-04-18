package sd2223.trab1.server.rest;

import sd2223.trab1.api.Message;
import sd2223.trab1.api.java.Feeds;
import sd2223.trab1.api.rest.FeedsService;
import sd2223.trab1.server.java.JavaFeeds;

import java.util.List;

public class RestFeedsResource extends RestResource implements FeedsService {

    final Feeds impl;

    public RestFeedsResource(int serverId) {
        this.impl = new JavaFeeds(serverId);
    }

    @Override
    public long postMessage(String user, String pwd, Message msg) {
        return super.fromJavaResult(impl.postMessage(user, pwd, msg));
    }

    @Override
    public void removeFromPersonalFeed(String user, long mid, String pwd) {
        super.fromJavaResult(impl.removeFromPersonalFeed(user, mid, pwd));
    }

    @Override
    public Message getMessage(String user, long mid) {
        return super.fromJavaResult(impl.getMessage(user, mid));
    }

    @Override
    public List<Message> getMessages(String user, long time) {
        return super.fromJavaResult(impl.getMessages(user, time));
    }

    @Override
    public void subUser(String user, String userSub, String pwd) {
        super.fromJavaResult(impl.subUser(user, userSub, pwd));
    }

    @Override
    public void unsubscribeUser(String user, String userSub, String pwd) {
        super.fromJavaResult(impl.unsubscribeUser(user, userSub, pwd));
    }

    @Override
    public List<String> listSubs(String user) {
        return super.fromJavaResult(impl.listSubs(user));
    }

    @Override
    public void createFeed(String user) {
        super.fromJavaResult(impl.createFeedInfo(user));
    }

    @Override
    public void deleteFeed(String user) {
        super.fromJavaResult(impl.deleteFeedInfo(user));
    }

    @Override
    public void propagateMessage(Message msg) {super.fromJavaResult(impl.propagateMessage(msg));}

    public void propagateSub(String user, String userSub) {super.fromJavaResult(impl.propagateSub(user, userSub));}

    @Override
    public void addMessage(Message msg) {
        super.fromJavaResult(impl.addMessage(msg));
    }

}
