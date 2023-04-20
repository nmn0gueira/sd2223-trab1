package sd2223.trab1.client;

import sd2223.trab1.api.java.Feeds;
import sd2223.trab1.client.rest.RestFeedsClient;
import sd2223.trab1.client.soap.SoapFeedsClient;
import sd2223.trab1.client.util.Discovery;


import java.net.URI;

public class FeedsClientFactory {

    private static final String REST = "/rest";
    private static final String SOAP = "/soap";
    private static final String FEEDS = "feeds.";

    public static Feeds get(String domain) {

        URI serverURI = Discovery.getInstance().knownUrisOf(FEEDS + domain, 1)[0];
        var uriString = serverURI.toString();

        if (uriString.endsWith(REST))
            return new RestFeedsClient(serverURI);
        else if (uriString.endsWith(SOAP))
            return new SoapFeedsClient(serverURI);
        else
            throw new RuntimeException("Unknown service type..." + uriString);
    }
}
