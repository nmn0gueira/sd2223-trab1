package sd2223.trab1.server.soap;

import jakarta.jws.WebService;
import sd2223.trab1.api.Message;
import sd2223.trab1.api.java.Feeds;
import sd2223.trab1.api.soap.FeedsException;
import sd2223.trab1.api.soap.FeedsService;
import sd2223.trab1.server.java.JavaFeeds;

import java.util.List;

@WebService(serviceName= FeedsService.NAME, targetNamespace=FeedsService.NAMESPACE, endpointInterface=FeedsService.INTERFACE)
public class SoapFeedsWebService extends SoapWebService<FeedsException> implements FeedsService{

    final Feeds impl;
    SoapFeedsWebService(int serverId, String serviceDomain) {
        super(result -> new FeedsException(result.error().toString()));
        this.impl = new JavaFeeds(serverId, serviceDomain);
    }

    @Override
    public long postMessage(String user, String pwd, Message msg) throws FeedsException {
        return super.fromJavaResult(impl.postMessage(user, pwd, msg));
    }

    @Override
    public void removeFromPersonalFeed(String user, long mid, String pwd) throws FeedsException {
        super.fromJavaResult(impl.removeFromPersonalFeed(user, mid, pwd));
    }

    @Override
    public Message getMessage(String user, long mid) throws FeedsException {
        return super.fromJavaResult(impl.getMessage(user, mid));
    }

    @Override
    public List<Message> getMessages(String user, long time) throws FeedsException {
        return super.fromJavaResult(impl.getMessages(user, time));
    }

    @Override
    public void subUser(String user, String userSub, String pwd) throws FeedsException {
        super.fromJavaResult(impl.subUser(user, userSub, pwd));
    }

    @Override
    public void unsubscribeUser(String user, String userSub, String pwd) throws FeedsException {
        super.fromJavaResult(impl.unsubscribeUser(user, userSub, pwd));
    }

    @Override
    public List<String> listSubs(String user) throws FeedsException {
        return super.fromJavaResult(impl.listSubs(user));
    }

    @Override
    public void createFeed(String user) throws FeedsException {
        super.fromJavaResult(impl.createFeedInfo(user));
    }

    @Override
    public void deleteFeed(String user) throws FeedsException {
        super.fromJavaResult(impl.deleteFeedInfo(user));
    }

    @Override
    public void addMessageToUsers(Message msg, String users) throws FeedsException {
        super.fromJavaResult(impl.addMessageToUsers(msg, users));
    }

    @Override
    public void removeUserFromSubscribers(String user, String users) throws FeedsException {
        super.fromJavaResult(impl.removeUserFromSubscribers(user, users));
    }

    @Override
    public void removeUserFromSubscriptions(String user, String users) throws FeedsException {
        super.fromJavaResult(impl.removeUserFromSubscriptions(user, users));
    }

    @Override
    public void changeSubStatus(String user, String userSub, boolean subscribing) throws FeedsException{
        super.fromJavaResult(impl.changeSubStatus(user, userSub, subscribing));
    }

}
