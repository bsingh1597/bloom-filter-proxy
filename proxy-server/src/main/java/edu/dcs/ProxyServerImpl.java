package edu.dcs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ProxyServerImpl
 */
public class ProxyServerImpl implements ProxyServer {

    private static ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
    private static Character DELIMITER = ':';
    BloomFilter filter;
    ConcurrentHashMap<URL, BitSet> filterArray;
    String multicastGroup;
    int multicastPort;
    int publishFrequecy;
    int proxyPort;

    public ProxyServerImpl(BloomFilter filter, int proxyPort, String multicastGroup, int multicastPort,
            int publishFrequecy) {
        this.filter = filter;
        this.proxyPort = proxyPort;
        this.multicastGroup = multicastGroup;
        this.multicastPort = multicastPort;
        this.publishFrequecy = publishFrequecy;

        Thread publisherThread = new Thread(() -> multicastPublisher());
        publisherThread.start();
        Thread receiverThread = new Thread(() -> multicastReciever());
        receiverThread.start();
    }

    /**
     * Creates server cache.
     *
     * @param port Port on which server will listen.
     */
    @Override
    public ServerSocket createSocket(int port) throws IOException {
        return new ServerSocket(port);
    }

    @Override
    public void handleRequest(Socket clientSocket) {
        try {
            BufferedReader clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String requestLine = clientReader.readLine();
            String[] requestTokens = requestLine.split(" ");
            String method = requestTokens[0];
            String actualUrl = requestTokens[1];

            System.out.println("Request: " + method + " " + actualUrl);

            // Check if url exists in the cache
            String response = cache.get(actualUrl);

            if (response == null) {

                // Check in the Bloom Filter of the Proxy Servers

                // if response is null then Call internet
                response = fetchRequestFromInternet(actualUrl, method);
                // After fetch from internet put in cache
                appendCache(actualUrl, response);
            }

            sendReponseToClient(clientSocket, response);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends reponse to client.
     *
     * @param clientSocket Accepted socket in the ServerSocket.
     * @param response     Text Reponse from the url.
     */
    @Override
    public void sendReponseToClient(Socket clientSocket, String response) {

        try (OutputStream outputStream = clientSocket.getOutputStream()) {
            byte[] responseBytes = response.getBytes();
            outputStream.write(responseBytes);

            // Flush and close the output stream
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void appendCache(String url, String content) {
        cache.put(url, content);
    }

    /**
     * Fetches request from the url.
     *
     * @param urlString Url to call from internet.
     * @param method    Method of the API - For now we are using only GET requests.
     * @return Text reponse form internet.
     */
    @Override
    public String fetchRequestFromInternet(String urlString, String method) {
        URL url;
        StringBuilder responseContent = null;
        try {
            url = new URI(urlString).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
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

    public void multicastReciever() {

        try {
            InetAddress group = InetAddress.getByName(multicastGroup);

            try (MulticastSocket socket = new MulticastSocket(multicastPort)) {

                socket.joinGroup(new InetSocketAddress(group, multicastPort), null);

                while (true) {
                    byte[] buffer = new byte[filter.getBitSet().toByteArray().length];
                    DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

                    socket.receive(receivePacket);

                    String port = extractSubstring(new String(receivePacket.getData()), ":");

                    System.out.println(port);

                    BitSet bitArray = BitSet.valueOf(receivePacket.getData());
                    System.out.println("Received message: " + bitArray);
                    System.out.println("Received from: " + receivePacket.getPort());
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

                byte[] buffer = appendByteArrays(getDelimitedPort().getBytes(), filter.getBitSet().toByteArray());

                while (true) {
                    System.out.println("Sending");
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, multicastPort);
                    socket.send(packet);
                    System.out.println("Message sent.");
                    try {
                        Thread.sleep(15000);
                    } catch (InterruptedException e) { 
                        e.printStackTrace();
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getDelimitedPort() {
        return String.valueOf(proxyPort) + ":";
    }

    private static byte[] appendByteArrays(byte[] array1, byte[] array2) {
        byte[] resultArray = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, resultArray, 0, array1.length);
        System.arraycopy(array2, 0, resultArray, array1.length, array2.length);
        return resultArray;
    }

    private String extractSubstring(String inputString, String delimiter) {
        int index = inputString.indexOf(delimiter);
        if (index != -1) {
            return inputString.substring(0, index + 1);
        } else {
            return "";
        }
    }

}