package sd2223.trab1.client;

import sd2223.trab1.api.java.Users;
import sd2223.trab1.client.rest.RestUsersClient;
import sd2223.trab1.client.soap.SoapUsersClient;
import sd2223.trab1.server.util.Discovery;

import java.net.URI;

public class UsersClientFactory {

	private static final String REST = "/rest";
	private static final String SOAP = "/soap";
	private static final String USERS = "users.";

	public static Users get(String domain) {

		URI serverURI = Discovery.getInstance().knownUrisOf(USERS + domain, 1)[0];
		var uriString = serverURI.toString();

		if (uriString.endsWith(REST))
			return new RestUsersClient(serverURI);
		else if (uriString.endsWith(SOAP))
			return new SoapUsersClient(serverURI);
		else
			throw new RuntimeException("Unknown service type..." + uriString);
	}
}
