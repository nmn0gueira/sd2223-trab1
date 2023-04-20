package sd2223.trab1.server.soap;


import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.xml.ws.Endpoint;
import sd2223.trab1.client.util.Discovery;

public class SoapUsersServer {

	public static final int PORT = 8081;
	public static final String SERVICE = "users";
	public static String SERVER_BASE_URI = "http://%s:%s/soap";

	private static final Logger Log = Logger.getLogger(SoapUsersServer.class.getName());

	public static void main(String[] args) throws Exception {

		String serviceDomain = args[0];
//		System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
//		System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
//		System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
//		System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");

		Log.setLevel(Level.INFO);

		String ip = InetAddress.getLocalHost().getHostAddress();
		String serverURI = String.format(SERVER_BASE_URI, ip, PORT);

		Discovery discovery = Discovery.getInstance();
		discovery.announce(serviceDomain, SERVICE, serverURI);
		Endpoint.publish(serverURI.replace(ip, "0.0.0.0"), new SoapUsersWebService(serviceDomain));

		Log.info(String.format("%s Soap Server ready @ %s\n", SERVICE, serverURI));
	}
}
