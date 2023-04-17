package sd2223.trab1.server;

import sd2223.trab1.api.Message;
import sd2223.trab1.api.User;
import sd2223.trab1.client.FeedsClientFactory;
import sd2223.trab1.client.UsersClientFactory;
import sd2223.trab1.server.rest.RestFeedsServer;
import sd2223.trab1.server.rest.RestUsersServer;
import sd2223.trab1.server.soap.SoapUsersServer;

import java.net.URI;

public class Test {
	public static void main(String[] args) {

		RestUsersServer.main(new String[] {"nova"} );
		//RestFeedsServer.main(new String[] {"nova", "1"} );
		// SoapUsersServer.main(new String[] {"ourorg-0", "1"} );
		
		var uriRest = URI.create("http://localhost:8080/rest");
		//var uriSoap = URI.create("http://localhost:8081/soap");

		var restUserClt = UsersClientFactory.get(uriRest);
		//var restFeedClt = FeedsClientFactory.get(uriRest);
		
		var user = new User("nmp", "12345", "nova", "Preguiça");
		
		var res1 = restUserClt.createUser( user );
		System.out.println( res1 );

		/*var user2 = new User("nmpdadsasd", "12345", "nova", "Preguiça");

		var res4 = restUserClt.createUser( user2 );
		System.out.println( res4 );

		var res5 = restFeedClt.subUser("nmp@nova", "nmpdadsasd@nova", "12345");
		System.out.println( res5 );

		var res = restFeedClt.getMessages("nmp@nova", 0);
		System.out.println( res);

		Message msg = new Message(-1, "nmp", "nova", "wahtare");
		var res6 = restFeedClt.postMessage("nmp@nova", "12345", msg);
		System.out.println( res6 );*/


	}
}
