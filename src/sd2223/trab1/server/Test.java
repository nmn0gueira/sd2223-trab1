package sd2223.trab1.server;

import sd2223.trab1.api.Message;
import sd2223.trab1.api.User;
import sd2223.trab1.client.FeedsClientFactory;
import sd2223.trab1.client.UsersClientFactory;
import sd2223.trab1.server.rest.RestFeedsServer;
import sd2223.trab1.server.rest.RestUsersServer;
import sd2223.trab1.server.soap.SoapUsersServer;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Test {
	public static void main(String[] args) {

		//RestUsersServer.main(new String[] {"nova"} );
		//RestFeedsServer.main(new String[] {"nova", "1"} );
		// SoapUsersServer.main(new String[] {"ourorg-0", "1"} );
		
		//var uriRest = URI.create("http://localhost:8080/rest");
		//var uriSoap = URI.create("http://localhost:8081/soap");

		//var restUserClt = UsersClientFactory.get(uriRest);
		//var restFeedClt = FeedsClientFactory.get(uriRest);

		Set<String> set = new HashSet<>();
		set.add("1");
		set.add("2");
		System.out.println(set);

		List<String> list = new ArrayList<>(set);
		System.out.println(list);


	}
}
