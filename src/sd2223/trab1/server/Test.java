package sd2223.trab1.server;

import sd2223.trab1.api.User;
import sd2223.trab1.client.UsersClientFactory;
import sd2223.trab1.server.rest.RestUsersServer;
import sd2223.trab1.server.soap.SoapUsersServer;

import java.net.URI;

public class Test {
	public static void main(String[] args) throws Exception {

		RestUsersServer.main(new String[] {"ourorg-0", "ourorg-0"} );
		SoapUsersServer.main(new String[] {"ourorg-0", "1", "ourorg-0"} );
		
		var uriRest = URI.create("http://localhost:8080/rest");
		var uriSoap = URI.create("http://localhost:8081/soap");

		var restClt = UsersClientFactory.get(uriRest);
		var soapClt = UsersClientFactory.get(uriSoap);
		
		var user = new User("nmp", "12345", "nova", "Preguiça");
		
		var res1 = restClt.createUser( user );
		System.out.println( res1 );
		var res2 = restClt.getUser("nmp", "9999");
		System.out.println( res2 );
		var res3 = restClt.verifyPassword("nmp", "12345");
		System.out.println( res3 );

		var res4 = soapClt.createUser( user );
		System.out.println( res4 );
		var res5 = soapClt.getUser("nmp", "9999");
		System.out.println( res5 );
		var res6 = soapClt.verifyPassword("nmp", "12345");
		System.out.println( res6 );

	}
}
