package sd2223.trab1.server;

import sd2223.trab1.api.Message;
import sd2223.trab1.api.User;
import sd2223.trab1.client.FeedsClientFactory;
import sd2223.trab1.client.UsersClientFactory;
import sd2223.trab1.server.rest.RestFeedsServer;
import sd2223.trab1.server.rest.RestUsersServer;
import sd2223.trab1.server.soap.SoapUsersServer;
import sd2223.trab1.server.util.Discovery;


public class Test {
	public static void main(String[] args) {
		Discovery discovery = Discovery.getInstance();

		RestUsersServer.main(new String[] {"nova"} );
		//RestFeedsServer.main(new String[] {"nova", "1"} );
		//RestUsersServer.main(new String[] {"domain2"} );
		//RestFeedsServer.main(new String[] {"domain2", "2"} );
		// SoapUsersServer.main(new String[] {"ourorg-0", "1"} );

		var uriUserRest1 = discovery.knownUrisOf("users.nova",1)[0];
		//var uriUserRest2 = discovery.knownUrisOf("users.domain2",1)[0];
		//var uriFeedRest1 = discovery.knownUrisOf("feeds.nova",1)[0];
		//var uriFeedRest2 = discovery.knownUrisOf("feeds.domain2",1)[0];
		//var uriSoap = URI.create("http://localhost:8081/soap");

		var restUserClt1 = UsersClientFactory.get(uriUserRest1);
		//var restUserClt2 = UsersClientFactory.get(uriUserRest2);
		//var restFeedClt1 = FeedsClientFactory.get(uriFeedRest1);
		//var restFeedClt2 = FeedsClientFactory.get(uriFeedRest2);

		var user1 = new User("refugio.bergstorm", "1234", "nova","Refugio Bergstorm" );

		var res1 = restUserClt1.createUser(user1);
		System.out.println(res1);
		//var restFeedClt = FeedsClientFactory.get(uriRest);





	}
}
