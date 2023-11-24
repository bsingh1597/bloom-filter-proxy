package edu.dcs.proxyserverdcs.server;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProxyServer {

    private static ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    @Autowired
    BloomFilter filter;

    ConcurrentHashMap<Integer, BitSet> filterArray;
    String multicastGroup;
    int multicastPort;
    int multicastFrequency;

    @Value("${server.port}")
    String serverPort;

    @Autowired
    public ProxyServer(@Value("${multicast.addr}") String multicastGroup,
            @Value("${multicast.port}") int multicastPort, @Value("${multicast.freq}") int multicastFrequency) {
        this.multicastGroup = multicastGroup;
        this.multicastPort = multicastPort;
        this.multicastFrequency = multicastFrequency;
    }

    public ProxyServer() {
    }

    @GetMapping("/proxy")
    public String handleClient(@RequestParam(name = "url") String url) {

        // Check if url exists in the cache
        String response = cache.get(url);

        if (response == null) {

            // Check in the Bloom Filter of the Proxy Servers

            // if response is null then Call internet
            response = fetchRequestFromInternet(url);
            // After fetch from internet put in cache
            appendCache(url, response);
        }
        return response;
    }

    /**
     * Fetches request from the url.
     *
     * @param urlString Url to call from internet.
     * @param method    Method of the API - For now we are using only GET requests.
     * @return Text reponse form internet.
     */
    public String fetchRequestFromInternet(String urlString) {
        URL url;
        StringBuilder responseContent = null;
        try {
            url = new URI(urlString).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            // Read the response content
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            responseContent = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                responseContent.append(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return responseContent != null ? responseContent.toString() : null;
    }

    public void appendCache(String url, String content) {
        cache.put(url, content);
    }

    public void multicastReciever() {

        try {
            InetAddress group = InetAddress.getByName(multicastGroup);

            try (MulticastSocket socket = new MulticastSocket(multicastPort)) {

                socket.joinGroup(new InetSocketAddress(group, multicastPort), null);

                while (true) {
                    byte[] buffer = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

                    socket.receive(receivePacket);

                    try (ByteArrayInputStream bis = new ByteArrayInputStream(receivePacket.getData());
                            ObjectInputStream ois = new ObjectInputStream(bis)) {

                        // Deserialize the byte array into an object
                        MessageObject obj = (MessageObject) ois.readObject();

                        // Extract the string and BitSet from the received object
                        String receivedString = obj.getPortNumber();
                        BitSet receivedBitSet = obj.getBitSet();

                        // BitSet bitArray = BitSet.valueOf(receivePacket.getData());
                        System.out.println("Received message: " + receivedBitSet);
                        System.out.println("Received from: " + receivedString);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * run in thread serving as the braodcast server.
     *
     */
    public void multicastPublisher() {
        try {
            InetAddress group = InetAddress.getByName(multicastGroup);
            try (MulticastSocket socket = new MulticastSocket()) {
                // Set the time-to-live for the message
                socket.setTimeToLive(5);

                while (true) {
                    System.out.println("Sending");
                    // Create an object representing both the string and BitSet
                    MessageObject messageObject = new MessageObject(serverPort, filter.getBitSet());
                    // Serialize the object to JSON
                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                        // Serialize the object into the byte array
                        oos.writeObject(messageObject);

                        DatagramPacket packet = new DatagramPacket(bos.toByteArray(), bos.toByteArray().length, group,
                                multicastPort);
                        socket.send(packet);
                        System.out.println("Message sent.");
                        try {
                            Thread.sleep(15000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
