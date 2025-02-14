package sd2223.trab1.client.rest;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import sd2223.trab1.api.Message;
import sd2223.trab1.api.java.Feeds;
import sd2223.trab1.api.java.Result;
import sd2223.trab1.api.rest.FeedsService;

import java.net.URI;
import java.util.List;

public class RestFeedsClient extends RestClient implements Feeds {

    private static final String PATH_SUB = "sub";
    private static final String PATH_LIST = "list";
    private static final String PATH_CREATE = "create";
    private static final String PATH_DELETE = "delete";
    private static final String PATH_ADD = "add";
    private static final String PATH_UNSUB_ALL = "unsubAll";
    private static final String PATH_REMOVE_ALL_SUBS = "removeAllSubs";
    private static final String PATH_SUBSCRIBER = "subscriber";

    final WebTarget target;

    public RestFeedsClient( URI serverURI ) {
        super( serverURI );
        target = client.target( serverURI ).path( FeedsService.PATH );
    }

    private Result<Long> clt_postMessage(String user, String pwd, Message msg) {

        Response r = target.path(user)
                .queryParam(FeedsService.PWD, pwd).request()
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(msg, MediaType.APPLICATION_JSON));

        return super.toJavaResult(r, Long.class);
    }

    private Result<Void> clt_removeFromPersonalFeed(String user, long mid, String pwd) {

        Response r = target.path(user).path(String.valueOf(mid))
                .queryParam(FeedsService.PWD, pwd).request()
                .delete();

        return super.toJavaResult(r, Void.class);
    }

    private Result<Message> clt_getMessage(String user, long mid) {

        Response r = target.path(user).path(String.valueOf(mid))
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get();

        return super.toJavaResult(r, Message.class);
    }

    private Result<List<Message>> clt_getMessages(String user, long time) {

        Response r = target.path(user)
                .queryParam(FeedsService.TIME, time)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get();

        return super.toJavaResult(r, new GenericType<List<Message>>() {});
    }

    private Result<Void> clt_subUser(String user, String userSub, String pwd) {

        Response r = target.path(PATH_SUB).path(user).path(userSub)
                .queryParam(FeedsService.PWD, pwd).request()
                .post(Entity.json(null));

        return super.toJavaResult(r, Void.class);
    }

    private Result<Void> clt_unsubscribeUser(String user, String userSub, String pwd) {

        Response r = target.path(PATH_SUB).path(user).path(userSub)
                .queryParam(FeedsService.PWD, pwd).request()
                .delete();

        return super.toJavaResult(r, Void.class);
    }

    private Result<List<String>> clt_listSubs(String user) {

        Response r = target.path(PATH_SUB).path(PATH_LIST).path(user)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get();

        return super.toJavaResult(r, new GenericType<List<String>>() {});
    }

    private Result<Void> clt_createFeed(String user) {

        Response r = target.path(PATH_CREATE).path(user)
                .request()
                .post(Entity.json(null));

        return super.toJavaResult(r, Void.class);
    }

    private Result<Void> clt_deleteFeed(String user) {

        Response r = target.path(PATH_DELETE).path(user)
                .request()
                .delete();

        return super.toJavaResult(r, Void.class);
    }

    private Result<Void> clt_addMessageToUsers(Message msg, String users) {

        Response r = target.path(PATH_ADD).queryParam(FeedsService.USERS, users)
                .request()
                .post(Entity.entity(msg, MediaType.APPLICATION_JSON));

        return super.toJavaResult(r, Void.class);
    }

    private Result<Void> clt_removeUserFromSubscribers(String userRem, String users) {

        Response r = target.path(PATH_UNSUB_ALL).path(userRem).queryParam(FeedsService.USERS, users)
                .request()
                .delete();

        return super.toJavaResult(r, Void.class);
    }

    private Result<Void> clt_removeUserFromSubscriptions(String userRem, String users) {

        Response r = target.path(PATH_REMOVE_ALL_SUBS).path(userRem).queryParam(FeedsService.USERS, users)
                .request()
                .delete();

        return super.toJavaResult(r, Void.class);
    }

    private Result<Void> clt_changeSubStatus(String user, String userSub, boolean subscribing) {
        Response r;
        Invocation.Builder b = target.path(PATH_SUBSCRIBER).path(user).path(userSub).request();
        if (subscribing) {
            r = b.post(Entity.json(null));
        } else {
            r = b.delete();
        }

        return super.toJavaResult(r, Void.class);
    }

    @Override
    public Result<Long> postMessage(String user, String pwd, Message msg) {
        return super.reTry(() -> clt_postMessage(user, pwd, msg));
    }

    @Override
    public Result<Void> removeFromPersonalFeed(String user, long mid, String pwd) {
        return super.reTry(() -> clt_removeFromPersonalFeed(user, mid, pwd));
    }

    @Override
    public Result<Message> getMessage(String user, long mid) {
        return super.reTry(() -> clt_getMessage(user, mid));
    }

    @Override
    public Result<List<Message>> getMessages(String user, long time) {
        return super.reTry(() -> clt_getMessages(user, time));
    }

    @Override
    public Result<Void> subUser(String user, String userSub, String pwd) {
        return super.reTry(() -> clt_subUser(user, userSub, pwd));
    }

    @Override
    public Result<Void> unsubscribeUser(String user, String userSub, String pwd) {
        return super.reTry(() -> clt_unsubscribeUser(user, userSub, pwd));
    }

    @Override
    public Result<List<String>> listSubs(String user) {
        return super.reTry(() -> clt_listSubs(user));
    }

    @Override
    public Result<Void> createFeedInfo(String user) {
       return super.reTry(() -> clt_createFeed(user));
    }

    @Override
    public Result<Void> deleteFeedInfo(String user) {
        return super.reTry(() -> clt_deleteFeed(user));
    }

    @Override
    public Result<Void> addMessageToUsers(Message msg, String users) {
        return super.reTry(() -> clt_addMessageToUsers(msg, users));
    }

    @Override
    public Result<Void> removeUserFromSubscribers(String userRem, String users) {
        return super.reTry(() -> clt_removeUserFromSubscribers(userRem, users));
    }

    @Override
    public Result<Void> removeUserFromSubscriptions(String userRem, String users) {
        return super.reTry(() -> clt_removeUserFromSubscriptions(userRem, users));
    }

    @Override
    public Result<Void> changeSubStatus(String user, String userSub, boolean subscribing) {
        return super.reTry(() -> clt_changeSubStatus(user, userSub, subscribing));
    }

}
