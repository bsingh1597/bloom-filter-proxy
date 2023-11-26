package edu.dcs.proxyserverdcs.server;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.MulticastSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProxyServer {

    private static final Logger logger = LoggerFactory.getLogger(ProxyServer.class);

    private static ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    @Autowired
    BloomFilter filter;

    ConcurrentHashMap<Integer, BitSet> filterMap = new ConcurrentHashMap<>();
    String multicastGroup;
    int multicastPort;
    int multicastFrequency;

    int failedInternetCalls = 0;

    int totalNumOfDirectRequests = 0;
    int totalNumOfFalsePositive = 0;
    // int totalNumOfTruePositive = 0;

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

        try {
            if (response == null) {
                int possibleProxyPort = findProxy(url);
                if (possibleProxyPort != 0) {
                    // Call proxy to fetch response
                    response = callProxyServer(possibleProxyPort, url);
                } else {
                    // if response is null then Call internet
                    response = fetchRequestFromInternet(url);
                    if(response != null) {
                        // After fetch from internet put in cache
                        appendCache(url, response);
                        totalNumOfDirectRequests++;
                        // Append in Bloom filter
                        filter.add(url);
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    @GetMapping("/fetch-proxy")
    public String callFromProxyServer(@RequestParam(name = "url") String url) {
        // Check if url exists in the cache
        String response = cache.get(url);

        try {
            if (response == null) {
                // Case of false positive
                totalNumOfFalsePositive++;
                // if response is null then Call internet
                response = fetchRequestFromInternet(url);
                // After fetch from internet put in cache
                appendCache(url, response);
                // Append in Bloom filter
                filter.add(url);

            } else {
                // Case of true positive
                // totalNumOfTruePositive++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info("****** Total Requests On this Proxy Server: {}", totalNumOfDirectRequests);
        logger.info("****** False posititves: {}", totalNumOfFalsePositive);
        // logger.info("****** True posititves: {}", totalNumOfTruePositive);
        return response;
    }

    private String callProxyServer(int possibleProxyPort, String stringUrl) {

        // Create a URL object
        URL url;
        StringBuilder response = new StringBuilder();
        try {
            url = getUrl(possibleProxyPort, stringUrl);

            // Open a connection to the URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set the request method to GET
            connection.setRequestMethod("GET");

            // Get the response code
            // int responseCode = connection.getResponseCode();
            // System.out.println("Response Code: " + responseCode);

            // Read the response
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
        return response.toString();
    }

    private int findProxy(String url) {
        for (Map.Entry<Integer, BitSet> entry : filterMap.entrySet()) {
            if (checkInBloomFilter(entry.getValue(), url)) {
                return entry.getKey();
            }
        }
        return 0;
    }

    private boolean checkInBloomFilter(BitSet bitset, String url) {
        return filter.contains(bitset, url);
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
        String responseContent = null;
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            url = new URL(urlString);
            Future<String> future = executor.submit(() -> performApiCall(url));

            // Set a timeout of 5 seconds
            responseContent = future.get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            failedInternetCalls++;
            logger.info("API call timed out");
            logger.info("failed internet call {}", failedInternetCalls);

        } catch (Exception e) {
            failedInternetCalls++;
            System.out.println("Exception while calling internet");
            // e.printStackTrace();
        } finally {
            executor.shutdown();
        }
        /*
         * try {
         * 
         * url = new URI(urlString).toURL();
         * HttpURLConnection connection = (HttpURLConnection) url.openConnection();
         * connection.setRequestMethod("GET");
         * connection.setConnectTimeout(5000);
         * connection.setConnectTimeout(5000);
         * // int responseCode = connection.getResponseCode();
         * // System.out.println("Response Code: " + responseCode);
         * BufferedReader reader = new BufferedReader(new
         * InputStreamReader(connection.getInputStream()));
         * String line;
         * responseContent = new StringBuilder();
         * while ((line = reader.readLine()) != null) {
         * responseContent.append(line);
         * break;
         * }
         * reader.close();
         * } catch (IOException e) {
         * e.printStackTrace();
         * } catch (URISyntaxException e) {
         * e.printStackTrace();
         * }
         */

        return responseContent != null ? responseContent.toString() : null;
    }

    private String performApiCall(URL url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder responseContent = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                responseContent.append(line);
                break;
            }

            reader.close();
            connection.disconnect();

            return responseContent.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
                    byte[] buffer = new byte[90000];
                    DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

                    socket.receive(receivePacket);

                    MessageObject object = getMulticastMessage(receivePacket.getData());

                    if (object != null && !serverPort.equals(object.getPortNumber())) {
                        // If the portnum from the multicast doesn't match the port number of this
                        // server then add to Bitset array
                        filterMap.put(Integer.parseInt(object.getPortNumber()), object.getBitSet());
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private MessageObject getMulticastMessage(byte[] data) {
        MessageObject multicastMessage = null;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
                ObjectInputStream ois = new ObjectInputStream(bis)) {

            // Deserialize the byte array into an object
            multicastMessage = (MessageObject) ois.readObject();

            // Extract the string and BitSet from the received object
            String portNum = multicastMessage.getPortNumber();
            BitSet receivedBitSet = multicastMessage.getBitSet();

            // BitSet bitArray = BitSet.valueOf(receivePacket.getData());
            // logger.info("Received bitset {}", receivedBitSet);
            // logger.info("Received from {}", portNum);
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return multicastMessage;
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
                    // logger.info("Publish message");
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
                        logger.info("Message sent.");
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

    public static URL getUrl(int portNumber, String internetUrl)
            throws UnknownHostException, UnsupportedEncodingException, MalformedURLException, URISyntaxException {

        StringBuilder urlBuilder = new StringBuilder("http://");
        urlBuilder.append(InetAddress.getLocalHost().getHostAddress());
        urlBuilder.append(":");
        urlBuilder.append(portNumber);
        urlBuilder.append("/fetch-proxy");
        urlBuilder.append("?");
        urlBuilder.append(("url="));
        urlBuilder.append(URLEncoder.encode(internetUrl, "UTF-8"));

        return new URI(urlBuilder.toString()).toURL();

    }
}
