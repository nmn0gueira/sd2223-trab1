package sd2223.trab1.server.soap;


import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.xml.ws.Endpoint;
import sd2223.trab1.server.util.Discovery;

public class SoapFeedsServer {

    public static final int PORT = 8081;
    public static final String SERVICE = "feeds";
    public static String SERVER_BASE_URI = "http://%s:%s/soap";

    private static final Logger Log = Logger.getLogger(SoapFeedsServer.class.getName());

    public static void main(String[] args) throws Exception {

        String domainName = args[0];
        int serverId = Integer.parseInt(args[1]);

//		System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
//		System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
//		System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
//		System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");

        Log.setLevel(Level.INFO);

        String ip = InetAddress.getLocalHost().getHostAddress();
        String serverURI = String.format(SERVER_BASE_URI, ip, PORT);

        Discovery discovery = Discovery.getInstance();
        discovery.announce(domainName, SERVICE, serverURI);
        Endpoint.publish(serverURI.replace(ip, "0.0.0.0"), new SoapFeedsWebService(serverId, domainName));

        Log.info(String.format("%s Soap Server ready @ %s\n", SERVICE, serverURI));
    }
}
