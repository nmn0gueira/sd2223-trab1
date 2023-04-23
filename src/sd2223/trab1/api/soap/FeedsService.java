package sd2223.trab1.api.soap;

import java.util.List;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import sd2223.trab1.api.Message;

@WebService(serviceName=FeedsService.NAME, targetNamespace=FeedsService.NAMESPACE, endpointInterface=FeedsService.INTERFACE)
public interface FeedsService {

	String NAME = "feeds";
	String NAMESPACE = "http://sd2223";
	String INTERFACE = "sd2223.trab1.api.soap.FeedsService";
	
	/**
	 * Posts a new message in the feed, associating it to the feed of the specific user.
	 * A message should be identified before publish it, by assigning an ID.
	 * A user must contact the server of her domain directly (i.e., this operation should not be
	 * propagated to other domain)
	 *
	 * @param user user of the operation (format user@domain)
	 * @param msg the message object to be posted to the server
	 * @param pwd password of the user sending the message
	 * @return the unique numerical identifier for the posted message;
	 * @throws FeedsException otherwise
	 */
	@WebMethod
	long postMessage(String user, String pwd, Message msg) throws FeedsException;

	/**
	 * Removes the message identified by mid from the feed of user.
	 * A user must contact the server of her domain directly (i.e., this operation should not be
	 * propagated to other domain)
	 * 
	 * @param user user feed being accessed (format user@domain)
	 * @param mid the identifier of the message to be deleted
	 * @param pwd password of the user
	 * @throws FeedsException otherwise
	 */
	@WebMethod
	void removeFromPersonalFeed(String user, long mid, String pwd) throws FeedsException;

	/**
	 * Obtains the message with id from the feed of user (may be a remote user)
	 * 
	 * @param user user feed being accessed (format user@domain)
	 * @param mid id of the message
	 *
	 * @return the message if it exists;
	 * @throws FeedsException otherwise
	 */
	@WebMethod
	Message getMessage(String user, long mid) throws FeedsException;

	/**
	 * Returns a list of all messages stored in the server for a given user newer than time
	 * (note: may be a remote user)
	 * 
	 * @param user user feed being accessed (format user@domain)
	 * @param time the oldest time of the messages to be returned
	 * @return	a list of messages, potentially empty;
	 * @throws FeedsException otherwise
	 */
	@WebMethod
	List<Message> getMessages(String user, long time) throws FeedsException;

	/**
	 * Request to subscribe a user.
	 * A user must contact the server of her domain directly (i.e., this operation should not be
	 * propagated to other domain)
	 *
	 * @param user the user subscribing (following) other user (format user@domain)
	 * @param userSub the user to be subscribed (followed) (format user@domain)
	 * @param pwd password of the user to subscribe
	 * @throws FeedsException otherwise
	 */
	@WebMethod
	void subUser(String user, String userSub, String pwd) throws FeedsException;

	/**
	 * Request to unsubscribe a user.
	 * A user must contact the server of her domain directly (i.e., this operation should not be
	 * propagated to other domain)
	 *
	 * @param user the user unsubscribing (following) other user (format user@domain)
	 * @param userSub the identifier of the user to be unsubscribed (format user@domain)
	 * @param pwd password of the user to subscribe
	 * @throws FeedsException otherwise
	 */
	@WebMethod
	void unsubscribeUser(String user, String userSub, String pwd) throws FeedsException;

	/**
	 * Subscribed users.
	 *
	 * @param user user being accessed (format user@domain)
	 * @throws FeedsException otherwise
	 */
	@WebMethod
	List<String> listSubs(String user) throws FeedsException;

	/**
	 * Creates the feed info for a user (personal feed, subscriptions, subscribers).
	 * A user must contact the server of her domain directly (i.e., this operation should not be
	 * propagated to other domain)
	 *
	 * @param user user being accessed (format user@domain)
	 * @throws FeedsException otherwise
	 */
	@WebMethod
	void createFeed(String user) throws FeedsException;

	/**
	 * Deletes the feed info for a user (personal feed, subscriptions, subscribers).
	 * A user must contact the server of her domain directly (i.e., this operation should not be
	 * propagated to other domain)
	 *
	 * @param user user being accessed (format user@domain)
	 * @throws FeedsException otherwise
	 */
	@WebMethod
	void deleteFeed(String user) throws FeedsException;

	/**
	 * Add a message to the personal feed of a set of users
	 *
	 * @param msg message to be added to subscribed users' feeds
	 * @param users String.join(",", set of users to add messages to)
	 * @throws FeedsException otherwise
	 */
	@WebMethod
	void addMessageToUsers(Message msg, String users) throws FeedsException;

	/**
	 * Remove a user from their subscribers' list of subscriptions
	 *
	 * @param user user that has been deleted
	 * @param users String.join(",", set of users to update)
	 * @throws FeedsException otherwise
	 */
	@WebMethod
	void removeUserFromSubscribers(String user, String users) throws FeedsException;

	/**
	 * Remove a user from their subscribed users' list of subscribers
	 *
	 * @param user user that has been deleted
	 * @param users String.join(",", set of users to update)
	 * @throws FeedsException otherwise
	 */
	@WebMethod
	void removeUserFromSubscribedTo(String user, String users) throws FeedsException;

	/**
	 * Change subscription status of user to another user internally
	 *
	 * @param user user that is subbing/unsubbing (format user@domain)
	 * @param userSub user that is getting subbed/unsubbed (format user@domain)
	 * @param subscribing true if subscribing, false if unsubscribing
	 * @throws FeedsException otherwise
	 */
	@WebMethod
	void changeSubStatus(String user, String userSub, boolean subscribing) throws FeedsException;
}
