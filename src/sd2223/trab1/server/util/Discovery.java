package sd2223.trab1.server.util;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * <p>A class interface to perform service discovery based on periodic 
 * announcements over multicast communication.</p>
 * 
 */

public interface Discovery {

	/**
	 * Used to announce the URI of the given service name.
	 * @param serviceName - the name of the service
	 * @param serviceURI - the uri of the service
	 */
	void announce(String serviceName, String serviceURI);

	/**
	 * Get discovered URIs for a given service name
	 * @param serviceName - name of the service
	 * @param minReplies - minimum number of requested URIs. Blocks until the number is satisfied.
	 * @return array with the discovered URIs for the given service name.
	 */
	URI[] knownUrisOf(String serviceName, int minReplies);

	/**
	 * Get the instance of the Discovery service
	 * @return the singleton instance of the Discovery service
	 */
	static Discovery getInstance() {
		return DiscoveryImpl.getInstance();
	}
}

/**
 * Implementation of the multicast discovery service
 */
class DiscoveryImpl implements Discovery {
	
	private static final Logger Log = Logger.getLogger(Discovery.class.getName());

	// The pre-agreed multicast endpoint assigned to perform discovery.

	static final int DISCOVERY_RETRY_TIMEOUT = 5000;
	static final int DISCOVERY_ANNOUNCE_PERIOD = 1000;

	// Replace with appropriate values...
	static final InetSocketAddress DISCOVERY_ADDR = new InetSocketAddress("226.226.226.226", 2266);

	// Used separate the two fields that make up a service announcement.
	private static final String DELIMITER = "\t";

	private static final int MAX_DATAGRAM_SIZE = 65536;

	private static Discovery singleton;

	private final Map<String, List<URI>> discoveredServices;

	synchronized static Discovery getInstance() {
		if (singleton == null) {
			singleton = new DiscoveryImpl();
		}
		return singleton;
	}

	private DiscoveryImpl() {
		this.discoveredServices = new HashMap<>(1024);
		this.startListener();
	}

	@Override
	public void announce(String serviceName, String serviceURI) {
		Log.info(String.format("Starting Discovery announcements on: %s for: %s -> %s\n", DISCOVERY_ADDR, serviceName,
				serviceURI));

		var pktBytes = String.format("%s%s%s", serviceName, DELIMITER, serviceURI).getBytes();
		var pkt = new DatagramPacket(pktBytes, pktBytes.length, DISCOVERY_ADDR);

		// start thread to send periodic announcements
		new Thread(() -> {
			try (var ds = new DatagramSocket()) {
				while (true) {
					try {
						ds.send(pkt);
						Thread.sleep(DISCOVERY_ANNOUNCE_PERIOD);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}


	@Override
	public URI[] knownUrisOf(String serviceName, int minEntries) {
		// Wait until we receive at least minEntries announcements
		while (true) {
			synchronized (discoveredServices) {
				List<URI> uris = discoveredServices.get(serviceName);
				if (uris == null) {
					try {
						discoveredServices.wait(DISCOVERY_RETRY_TIMEOUT);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				else if (uris.size() >= minEntries) 	// If there are at least minEntries services, return them
					return uris.toArray(new URI[0]);

			}
		}

	}

	private void startListener() {
		Log.info(String.format("Starting discovery on multicast group: %s, port: %d\n", DISCOVERY_ADDR.getAddress(),
				DISCOVERY_ADDR.getPort()));

		new Thread(() -> {
			try (var ms = new MulticastSocket(DISCOVERY_ADDR.getPort())) {
				ms.joinGroup(DISCOVERY_ADDR, NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));
				for (;;) {
					try {
						var pkt = new DatagramPacket(new byte[MAX_DATAGRAM_SIZE], MAX_DATAGRAM_SIZE);
						ms.receive(pkt);

						var msg = new String(pkt.getData(), 0, pkt.getLength());
						Log.info(String.format("Received: %s", msg));

						var parts = msg.split(DELIMITER);
						if (parts.length == 2) {
							var serviceName = parts[0];
							var serviceURI = URI.create(parts[1]);
							synchronized (discoveredServices) {
								discoveredServices.computeIfAbsent(serviceName, k -> new ArrayList<>()).add(serviceURI);
								discoveredServices.notifyAll();

							}
						}

					} catch (Exception x) {
						x.printStackTrace();
					}
				}
			} catch (Exception x) {
				x.printStackTrace();
			}
		}).start();
	}
}